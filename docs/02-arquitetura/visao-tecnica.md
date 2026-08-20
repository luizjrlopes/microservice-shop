# Visão Técnica — Microservice Shop

> **Escopo:** arquitetura atualmente implementada. O estado futuro fica no [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md) e no [`../../ROADMAP.md`](../../ROADMAP.md).

## Stack atual

| Componente | Linguagem | Framework / libs | Comunicação |
|---|---|---|---|
| `order-service` | Java 25 | Spring Boot 3, Spring AMQP, Spring Data JPA, Flyway | HTTP + SQL + AMQP |
| PostgreSQL | — | PostgreSQL 16 | SQL |
| `scheduler-agent` | Python 3.11 | pika, requests, prometheus-client | AMQP + HTTP |
| `tests/bdd` | TypeScript | Cucumber, Axios, amqplib | HTTP + AMQP |
| Prometheus | — | Prometheus | HTTP scrape |
| `ml/experiments` | Python | pandas, scikit-learn | scripts standalone |
| RabbitMQ | — | RabbitMQ | AMQP |

`ml/llm` continua experimental; não representa uma feature integrada ao runtime.

## Estrutura principal

```text
microservice-shop/
├── services/
│   ├── api/order-service/
│   │   └── src/main/java/com/shop/order/
│   │       ├── domain/
│   │       ├── application/
│   │       │   ├── events/
│   │       │   ├── outbox/
│   │       │   └── ports/
│   │       ├── infrastructure/
│   │       │   ├── persistence/
│   │       │   └── outbox/
│   │       └── interfaces/
│   └── workers/scheduler-agent/
├── contracts/
├── tests/bdd/
├── infra/observability/
├── ml/
├── docs/
├── docker-compose.yml
└── Makefile
```

## Fluxo atual

```mermaid
flowchart LR
    C[Cliente] -->|POST /orders| API[order-service]
    API -->|TX| DB[(PostgreSQL\norders + outbox_events)]
    P[Outbox Publisher] -->|pending batch| DB
    P -->|order.created.v1| EX{{orders.events}}
    EX --> Q[scheduler.order-created.v1]
    Q --> W[scheduler-agent]
    W -->|POST /orders/{id}/confirm| API
    W --> R[retry queue]
    R -->|TTL| EX
    W --> D[DLQ]
```

## `order-service`

A implementação separa:

- `domain/` — `Order`, invariantes e `OrderStatus`;
- `application/` — casos de uso, evento versionado, Outbox e ports;
- `infrastructure/persistence/` — JPA/PostgreSQL;
- `infrastructure/outbox/` — persistência e publicação do Outbox;
- `interfaces/` — controller e contrato HTTP.

`CreateOrderService` é transacional: salva o pedido e `OrderCreatedEventV1` no Outbox na mesma transação. A publicação não ocorre diretamente no caso de uso.

`PublishPendingOutboxEventsService` processa eventos pendentes em lote. O adapter RabbitMQ usa publisher confirm; somente após confirmação positiva o registro é marcado como publicado. Em falha, o evento permanece pendente e a tentativa é registrada.

## Contratos

### HTTP

```text
POST /orders
GET  /orders/{id}
POST /orders/{id}/confirm
```

A criação rejeita `productId` vazio e quantidade não positiva. Pedido inexistente retorna `404`. A confirmação repetida é segura para reentregas.

### AMQP

```text
exchange: orders.events
routing key: order.created.v1
scheduler queue: scheduler.order-created.v1
retry queue: scheduler.order-created.retry.v1
DLQ: scheduler.order-created.dlq.v1
```

O contrato versionado está em:

- `contracts/events/order-created-v1.schema.json`;
- `contracts/asyncapi.yaml`.

## Semântica at-least-once

O `scheduler-agent`:

1. consome com ACK manual;
2. valida envelope/tipo/versão e IDs mínimos;
3. chama a confirmação HTTP com timeout;
4. dá ACK somente após sucesso ou após encaminhamento seguro para retry/DLQ;
5. usa uma retry queue com TTL e dead-letter de volta ao exchange;
6. envia para DLQ eventos inválidos e entregas que ultrapassam o limite;
7. usa NACK + requeue se não conseguir publicar o retry/DLQ.

As propriedades `message_id`, `correlation_id` e `x-retry-count` preservam identidade e contexto operacional entre tentativas.

## Persistência

PostgreSQL persiste pedidos e Outbox. Flyway mantém as migrations e o Hibernate valida o schema. Testcontainers comprova o comportamento contra PostgreSQL real, incluindo a persistência atômica usada pelo caso de criação.

## Testes

A suíte atual cobre:

- domínio, aplicação, controller, persistência e Outbox em Java;
- PostgreSQL real via Testcontainers;
- comportamento do worker Python em sucesso, retry, DLQ, evento inválido e falha de republicação;
- BDD HTTP + RabbitMQ com fila de auditoria dedicada;
- cenário `POST /orders -> order.created.v1 -> scheduler-agent -> CONFIRMED`.

O pipeline de PR separa qualidade, testes, segurança e E2E distribuído, e preserva logs/estado do Compose como artifacts quando a integração falha.

## Observabilidade

Já existem:

- Actuator/Micrometer no `order-service`;
- métricas Prometheus do `scheduler-agent` para processamento, retry, DLQ e latência;
- logs JSON do worker com IDs de negócio/evento nas transições principais;
- profile `observability` no Compose com Prometheus coletando API e worker.

Ainda faltam padronização de logs do serviço Java, métricas de Outbox/domínio, correlação ponta a ponta facilmente consultável, dashboards/alertas e eventual tracing distribuído.

## Infraestrutura

O Docker Compose é a infraestrutura executável local. `infra/terraform` não deve ser apresentado como implantação real enquanto permanecer apenas documental/experimental.

## Próximas etapas

```text
core de confiabilidade concluído
-> integração dedicada RabbitMQ + Outbox
-> auditoria de dependências mais abrangente
-> observabilidade operacional completa
-> escolher uma diferenciação principal: Cloud/IaC ou ML integrado
```

A evolução deve aumentar evidência de engenharia, não quantidade artificial de componentes.