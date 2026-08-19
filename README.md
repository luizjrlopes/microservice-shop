# Microservice Shop

**Microservice Shop** é uma plataforma de processamento de pedidos orientada a eventos, construída para explorar decisões reais de sistemas distribuídos: fronteiras entre serviços, mensageria assíncrona, entrega *at-least-once*, idempotência, retry com atraso, Dead Letter Queue, contratos de eventos e testes ponta a ponta.

O projeto usa uma stack poliglota de forma intencional. O `order-service`, em Java/Spring Boot, mantém o domínio e a API de pedidos. O `scheduler-agent`, em Python, reage aos eventos publicados no RabbitMQ e executa a confirmação assíncrona sem acessar diretamente o estado interno do serviço.

## O que o projeto demonstra

- arquitetura orientada a eventos com RabbitMQ;
- separação de responsabilidades entre API e worker;
- Clean Architecture no serviço de pedidos;
- processamento assíncrono com semântica *at-least-once*;
- confirmação idempotente de pedidos;
- retry configurável com fila intermediária e atraso;
- Dead Letter Queue para falhas não recuperáveis ou retries esgotados;
- eventos persistentes com `eventId`, timestamp e correlation ID;
- validação de entrada na fronteira HTTP;
- logs estruturados no consumidor com duração, tentativa e motivo de falha;
- testes unitários em Java e Python;
- cenários BDD em TypeScript cobrindo HTTP + AMQP + processamento assíncrono;
- pipeline de CI com lint, testes, segurança e validação end-to-end via Docker Compose.

## Arquitetura

```text
                         POST /orders
                              │
                              ▼
                  ┌──────────────────────┐
                  │    order-service     │
                  │ Java + Spring Boot   │
                  │                      │
                  │ domain / use cases   │
                  │ repository adapter   │
                  └──────────┬───────────┘
                             │
                      order.created
                             │
                             ▼
                  ┌──────────────────────┐
                  │      RabbitMQ        │
                  │    order.exchange    │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   scheduler-agent    │
                  │       Python         │
                  └──────────┬───────────┘
                             │
                   POST /orders/{id}/confirm
                             │
                             ▼
                      order-service
```

### Caminho de recuperação

```text
order.created
     │
     ▼
scheduler-agent
     │
     ├── sucesso ───────────────────────────────► ACK
     │
     ├── falha transitória
     │       │
     │       ▼
     │  order.retry.exchange
     │       │
     │       ▼
     │  order.created.retry
     │       │ TTL
     │       └──────────────────────────────────► order.created
     │
     └── retries esgotados / erro não recuperável
             │
             ▼
          order.dlx
             │
             ▼
       order.created.dlq
```

O consumidor só confirma a mensagem após uma resposta HTTP bem-sucedida. Falhas transitórias percorrem o ciclo de retry e mensagens que excedem o orçamento de tentativas são preservadas na DLQ para inspeção.

## Contrato do evento

`order.created` carrega os dados mínimos necessários para rastreabilidade e processamento:

```json
{
  "eventId": "d45457bf-6efa-4b0b-98ee-2e20d0e5f6aa",
  "occurredAt": "2026-08-19T00:00:00Z",
  "id": "order-id",
  "productId": "p1",
  "quantity": 2,
  "status": "PENDING"
}
```

A mensagem também recebe `messageId`, `correlationId` e headers de evento no RabbitMQ. O `correlationId` permite relacionar a criação do pedido ao processamento assíncrono nos logs.

## Semântica de entrega

O fluxo foi desenhado para tolerar reentrega:

1. o `order-service` cria o pedido e publica `order.created`;
2. o `scheduler-agent` recebe a mensagem;
3. a confirmação é feita exclusivamente pela API do `order-service`;
4. a operação de confirmação é idempotente;
5. o worker faz `ACK` somente após sucesso;
6. falhas transitórias entram em retry com atraso;
7. erros não recuperáveis ou tentativas esgotadas seguem para DLQ.

Esse desenho evita o erro clássico de considerar “mensagem recebida” equivalente a “trabalho concluído”.

## Stack

| Área | Tecnologia |
|---|---|
| Order API | Java 17, Spring Boot, Spring AMQP, Actuator |
| Worker | Python 3.11, Pika, Requests |
| Broker | RabbitMQ |
| Testes Java | JUnit 5, Mockito, MockMvc |
| Testes Python | Pytest |
| Testes E2E | TypeScript, Cucumber, Axios, amqplib |
| Orquestração local | Docker Compose |
| Qualidade | Spotless, Black, Ruff |
| Segurança | Bandit, npm audit |
| CI | GitHub Actions |

## Executar localmente

### Pré-requisitos

- Docker
- Docker Compose v2

### Subir a stack

```bash
docker compose up -d --build
```

Serviços disponíveis:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- RabbitMQ Management: `http://localhost:15672`

Credenciais locais do RabbitMQ: `guest` / `guest`.

### Criar um pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"p1","quantity":2}'
```

A resposta contém o ID criado. O pedido inicia em `PENDING`, publica `order.created` e é confirmado de forma assíncrona pelo scheduler.

### Consultar o estado

```bash
curl http://localhost:8080/orders/<ORDER_ID>
```

Após o processamento assíncrono, o estado esperado é `CONFIRMED`.

## Resiliência configurável

O `scheduler-agent` aceita:

| Variável | Default | Função |
|---|---:|---|
| `REQUEST_TIMEOUT_SECONDS` | `5` | timeout da chamada de confirmação |
| `MAX_RETRIES` | `3` | quantidade máxima de retries |
| `RETRY_DELAY_MS` | `5000` | atraso da fila de retry |
| `LOG_LEVEL` | `INFO` | nível dos logs estruturados |

## Testes e qualidade

### Unitários

```bash
make test
```

Executa os testes do `order-service` e do `scheduler-agent`.

### Linters

```bash
make lint
```

### Segurança

```bash
make security
```

### BDD ponta a ponta

Com a stack em execução:

```bash
make bdd-install
make bdd-test
```

Os cenários BDD observam o evento por uma fila exclusiva de auditoria, sem competir com a fila consumida pelo scheduler, e verificam também a confirmação assíncrona do pedido.

## CI

O GitHub Actions possui quatro gates:

1. **quality** — Spotless, Black, Ruff e TypeScript;
2. **unit-tests** — testes Java e Python;
3. **security** — Bandit e auditoria das dependências npm;
4. **integration-bdd** — build completo da stack em containers e cenários Cucumber reais contra HTTP + RabbitMQ.

Uma mudança só conclui a pipeline depois de atravessar o fluxo distribuído de ponta a ponta.

## Estrutura

```text
microservice-shop/
├── services/
│   ├── api/
│   │   └── order-service/          # domínio e API Spring Boot
│   └── workers/
│       └── scheduler-agent/        # consumidor RabbitMQ em Python
├── tests/
│   └── bdd/                        # cenários Cucumber
├── docs/
│   ├── 00-produto/
│   ├── 01-regras/
│   ├── 02-arquitetura/
│   ├── 03-ia-context/
│   └── 04-operacao/
├── ml/                             # trilha experimental de ML/LLM
├── infra/                          # evolução de infraestrutura como código
├── docker-compose.yml
└── Makefile
```

## Decisões e trade-offs

O repositório mantém ADRs e documentação arquitetural em `docs/02-arquitetura`.

A persistência do pedido ainda usa um adapter em memória. Isso mantém o núcleo do exemplo pequeno e facilita os testes, mas não é tratado como uma solução de produção. A evolução natural é introduzir PostgreSQL atrás da interface `OrderRepository`, preservando os casos de uso e o contrato do domínio.

A trilha `ml/` permanece experimental e desacoplada do caminho crítico. O objetivo é evitar que IA seja adicionada apenas como ornamento: qualquer promoção para o fluxo principal deve ter contrato, observabilidade e critério de falha explícitos.

## Próximas evoluções

As próximas melhorias de maior retorno arquitetural são:

- adapter PostgreSQL + migrações;
- tracing distribuído com OpenTelemetry;
- métricas Prometheus para API, filas e worker;
- testes de integração com Testcontainers;
- provisionamento de infraestrutura em ambiente cloud;
- somente depois disso, novos bounded contexts como catálogo ou autenticação.

Consulte `docs/02-arquitetura` para decisões técnicas e `ROADMAP.md` para a sequência de evolução.
