# Microservice Shop

**Microservice Shop** é uma plataforma de processamento de pedidos orientada a eventos, construída para demonstrar propriedades reais de sistemas distribuídos em uma stack poliglota Java/Python.

O projeto concentra complexidade onde ela importa: consistência entre banco e mensageria, entrega at-least-once, recuperação de falhas, contratos versionados, idempotência, testes distribuídos e observabilidade operacional.

## O que está implementado

- `order-service` em Java 25 + Spring Boot;
- PostgreSQL com Spring Data JPA e migrações Flyway;
- criação, consulta e confirmação idempotente de pedidos via HTTP;
- **Transactional Outbox**: pedido e evento são persistidos na mesma transação;
- publisher assíncrono do Outbox com publisher confirms do RabbitMQ;
- contrato versionado `order.created.v1` descrito em JSON Schema + AsyncAPI;
- `scheduler-agent` em Python com consumo manual, timeout HTTP e ACK controlado;
- retry com atraso e contagem preservada em headers;
- Dead Letter Queue para eventos inválidos ou retries esgotados;
- propagação de `eventId` e `correlationId` no fluxo assíncrono;
- métricas Prometheus no serviço Java e no worker Python;
- logs estruturados no worker;
- Docker Compose para PostgreSQL, RabbitMQ, API, worker e profile opcional de observabilidade;
- testes Java/Python, integração PostgreSQL via Testcontainers e BDD distribuído em TypeScript/Cucumber;
- gates de CI separados para qualidade, testes, segurança e fluxo E2E;
- ADRs, contratos e documentação arquitetural versionados.

## Arquitetura executável

```mermaid
flowchart LR
    C[Client] -->|POST /orders| API[order-service\nSpring Boot]
    API -->|transação| DB[(PostgreSQL\norders + outbox_events)]
    PUB[Outbox Publisher] -->|lê pendentes| DB
    PUB -->|order.created.v1| EX{{orders.events\nRabbitMQ}}
    EX --> Q[scheduler.order-created.v1]
    Q --> W[scheduler-agent\nPython]
    W -->|falha transitória| R[scheduler retry queue]
    R -->|TTL + dead letter| EX
    W -->|retries esgotados / inválido| D[DLQ]
    W -->|POST /orders/{id}/confirm| API
    C -->|GET /orders/{id}| API
```

A criação de um pedido não depende de o broker estar disponível para preservar o evento: o estado do pedido e o registro do Outbox são gravados atomicamente no PostgreSQL. O publisher processa registros pendentes e só os marca como publicados após confirmação do RabbitMQ.

No consumidor, falhas transitórias não geram ACK prematuro. A mensagem é encaminhada para uma fila de retry com atraso; após o limite configurado, segue para DLQ. Se a própria publicação de retry/DLQ falhar, a entrega original é reencaminhada pelo broker em vez de ser silenciosamente descartada.

## Stack

| Área            | Tecnologia                                                 |
| --------------- | ---------------------------------------------------------- |
| API             | Java 25, Spring Boot 3, Spring AMQP, Spring Data JPA       |
| Persistência    | PostgreSQL 16, Flyway                                      |
| Mensageria      | RabbitMQ, topic exchange, publisher confirms               |
| Worker          | Python 3.11, pika, requests, prometheus-client             |
| Contratos       | AsyncAPI + JSON Schema                                     |
| Testes          | JUnit, Mockito, Testcontainers, Pytest, Cucumber           |
| Observabilidade | Spring Actuator, Micrometer/Prometheus, métricas do worker |
| Execução local  | Docker Compose                                             |
| Automação       | Makefile, GitHub Actions                                   |
| ML experimental | Python, pandas, scikit-learn                               |

## Propriedades demonstradas

O objetivo do repositório não é aumentar artificialmente a quantidade de microserviços. O fluxo foi mantido pequeno para que as propriedades distribuídas possam ser verificadas de ponta a ponta:

- **consistência estado + evento:** Transactional Outbox remove o dual write entre PostgreSQL e RabbitMQ;
- **contrato explícito:** `order.created.v1` possui envelope e schema versionados;
- **entrega resiliente:** ACK manual, timeout, retry com atraso e DLQ;
- **idempotência de negócio:** confirmar novamente um pedido já confirmado não cria uma transição inválida;
- **falha observável:** retries, DLQ e latência de confirmação possuem métricas;
- **prova automatizada:** testes de integração e BDD exercitam persistência e fluxo distribuído real.

## Executando localmente

### Pré-requisitos

- Docker;
- Docker Compose v2+.

### Subir a stack

```bash
docker compose up -d --build
```

Serviços principais:

- API: `http://localhost:8080`;
- health: `http://localhost:8080/actuator/health`;
- PostgreSQL: `localhost:5432`;
- RabbitMQ Management: `http://localhost:15672`.

As credenciais `microservice_shop` / `microservice_shop` existem apenas no ambiente Compose de desenvolvimento.

### Criar e acompanhar um pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"SKU-1","quantity":2}'

curl http://localhost:8080/orders/<order-id>
```

O pedido nasce como `PENDING`; o Outbox publica `order.created.v1`; o `scheduler-agent` processa o evento e confirma o pedido de forma assíncrona.

## Qualidade e testes

O `Makefile` concentra a mesma superfície usada no CI:

```bash
make lint
make test
make security
make bdd-test
```

A suíte inclui testes de domínio e aplicação, persistência real com PostgreSQL/Testcontainers, políticas de retry/DLQ do worker e cenários Cucumber contra a stack distribuída.

## Observabilidade

Para subir também o Prometheus:

```bash
make compose-observability
```

O Prometheus coleta métricas do `order-service` em `/actuator/prometheus` e do `scheduler-agent` na porta `9100`.

## ML e IA

Os experimentos de ML permanecem deliberadamente desacoplados do fluxo transacional principal. Há trilhas executáveis de previsão de demanda e detecção de anomalias; `ml/llm` continua experimental e não é apresentada como feature integrada.

## Documentação

- [`docs/architecture.md`](docs/architecture.md) — arquitetura implementada;
- [`docs/02-arquitetura/visao-tecnica.md`](docs/02-arquitetura/visao-tecnica.md) — visão técnica detalhada;
- [`docs/02-arquitetura/decisoes/`](docs/02-arquitetura/decisoes/) — ADRs;
- [`contracts/`](contracts/) — contratos de evento;
- [`docs/04-operacao/`](docs/04-operacao/) — operação;
- [`docs/05-evolucao/`](docs/05-evolucao/) — evolução arquitetural;
- [`ROADMAP.md`](ROADMAP.md) — estado das ondas e próximos gates.

Documentos históricos são mantidos para rastreabilidade, mas o estado implementado é determinado pelo código, testes, contratos executáveis e documentação atual.
