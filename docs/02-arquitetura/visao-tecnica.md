# Visão Técnica — Microservice Shop

## Stack por componente

| Componente | Linguagem | Framework / biblioteca | Comunicação |
|---|---|---|---|
| `order-service` | Java 17 | Spring Boot 3, Spring AMQP, Actuator | HTTP + AMQP |
| `scheduler-agent` | Python 3.11 | Pika + Requests | AMQP + HTTP |
| `tests/bdd` | TypeScript | Cucumber + Axios + amqplib | HTTP + AMQP |
| `ml/` | Python | notebooks e libs experimentais | fora do caminho crítico |

## Fronteiras

O `order-service` é dono do estado e das regras de pedido. Nenhum worker acessa seu repositório diretamente. O `scheduler-agent` recebe eventos e solicita mudanças de estado pela API HTTP.

Essa regra mantém a autonomia do serviço e permite trocar o adapter de persistência sem alterar o consumidor.

## Fluxo principal

```text
Client
  │
  │ POST /orders
  ▼
order-service
  │
  │ order.created
  ▼
order.exchange
  │
  ▼
order.created queue
  │
  ▼
scheduler-agent
  │
  │ POST /orders/{id}/confirm
  ▼
order-service
```

O estado pode ser observado por `GET /orders/{id}`.

## Fluxo de falha

```text
order.created
   │
   ▼
scheduler-agent
   │
   ├── success ─────────────────────► ACK
   │
   ├── transient failure ──► NACK
   │                         │
   │                         ▼
   │                 order.retry.exchange
   │                         │
   │                         ▼
   │                 order.created.retry
   │                         │ TTL
   │                         ▼
   │                    order.exchange
   │                         │
   │                         └────────────► nova tentativa
   │
   └── terminal failure / retry budget exhausted
                             │
                             ▼
                         order.dlx
                             │
                             ▼
                      order.created.dlq
```

## Semântica de entrega

A estratégia é **at-least-once + idempotência**.

- `auto_ack` é desabilitado;
- o worker só faz ACK após sucesso ou encaminhamento explícito para DLQ;
- a fila de retry adiciona atraso para evitar hot loop;
- `x-death` é usado para calcular o número de reentregas;
- `MAX_RETRIES` limita o orçamento de recuperação;
- a confirmação é idempotente e tolera duplicatas.

A decisão completa está em `decisoes/ADR-003-retry-dlq-at-least-once.md`.

## Evento `order.created`

Campos mínimos:

```json
{
  "eventId": "uuid",
  "occurredAt": "ISO-8601",
  "id": "order-id",
  "productId": "p1",
  "quantity": 1,
  "status": "PENDING"
}
```

A mensagem AMQP também carrega `messageId`, `correlationId` e headers de evento.

## Arquitetura interna do order-service

O serviço mantém separação em quatro áreas:

- `domain/` — modelo de pedido e erros de domínio;
- `application/` — casos de uso de criação, consulta e confirmação;
- `infrastructure/` — `OrderRepository`, adapter em memória e publisher/topologia AMQP;
- `interfaces/` — controller HTTP e tratamento de erros.

O `InMemoryOrderRepository` continua sendo o adapter atual. A interface permite introduzir PostgreSQL posteriormente sem mover regras de negócio para a camada de persistência.

## Testes

### Unitários

- JUnit/Mockito/MockMvc no `order-service`;
- Pytest no `scheduler-agent`, incluindo sucesso, retry e DLQ.

### End-to-end

Os cenários Cucumber sobem contra a stack real e cobrem:

- publicação de `order.created`;
- confirmação assíncrona via scheduler;
- validação HTTP de entrada.

Para observar a publicação sem disputar a fila operacional com o worker, o BDD cria uma fila exclusiva de auditoria ligada a `order.exchange`.

## Convenções

- código em inglês;
- documentação principal em português;
- Java formatado por Spotless/Google Java Format;
- Python validado por Black e Ruff;
- contratos e trade-offs relevantes registrados por ADR.
