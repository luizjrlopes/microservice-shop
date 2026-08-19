# Visão Técnica — Microservice Shop

> **Escopo deste documento:** arquitetura atualmente implementada. A evolução futura está em [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## Stack atual

| Componente | Linguagem | Framework / libs | Comunicação |
|---|---|---|---|
| `order-service` | Java 17 | Spring Boot 3, Spring AMQP, Spring Data JPA | HTTP + SQL + AMQP |
| PostgreSQL | — | PostgreSQL 16 + Flyway | SQL |
| `scheduler-agent` | Python 3.11 | pika, requests | AMQP + HTTP |
| `tests/bdd` | TypeScript | Cucumber, Axios, amqplib | HTTP + AMQP |
| `ml/experiments` | Python | pandas, scikit-learn | scripts standalone |
| RabbitMQ | — | RabbitMQ | AMQP |

A área `ml/llm` contém dependências e documentação experimental, mas não possui atualmente uma feature LLM integrada ao sistema.

## Estrutura principal

```text
microservice-shop/
├── services/
│   ├── api/order-service/
│   │   └── src/main/java/com/shop/order/
│   │       ├── domain/
│   │       ├── application/
│   │       │   └── ports/
│   │       ├── infrastructure/
│   │       │   └── persistence/
│   │       └── interfaces/
│   └── workers/scheduler-agent/
├── contracts/
├── tests/bdd/
├── ml/experiments/
├── ml/llm/
├── infra/terraform/
├── docs/
├── docker-compose.yml
└── Makefile
```

## Fluxo atual

```mermaid
flowchart LR
    Client[Cliente] -->|POST /orders| API[order-service]
    Client -->|GET /orders/{id}| API
    API -->|JPA| DB[(PostgreSQL)]
    API -->|order.created| EX{{order.exchange}}
    EX --> Q[order.created]
    Q --> W[scheduler-agent]
    W -->|POST /orders/{id}/confirm| API
```

## Organização interna do `order-service`

A implementação mantém separação entre:

- `domain/` — entidade `Order`, invariantes e `OrderStatus`;
- `application/` — casos de uso e ports de saída;
- `infrastructure/` — adapters JPA/PostgreSQL, configuração AMQP e publisher;
- `interfaces/` — controller e tratamento HTTP de erros.

`OrderRepository` agora é um port da camada de aplicação. `PostgresOrderRepositoryAdapter` implementa esse contrato em infraestrutura.

## Contratos atuais

### HTTP

```text
POST /orders
GET  /orders/{id}
POST /orders/{id}/confirm
```

A criação rejeita `productId` vazio e quantidade não positiva. Pedido inexistente é tratado como `404`.

### AMQP

O runtime ainda usa temporariamente o contrato legado:

```text
exchange: order.exchange
routing key: order.created
queue: order.created
payload: id + productId + quantity + status
```

O contrato-alvo `order.created.v1` já existe em `contracts/`, mas só será adotado pelo runtime junto com Transactional Outbox.

## Persistência

Pedidos são persistidos no PostgreSQL via Spring Data JPA. O schema é versionado por Flyway e validado pelo Hibernate (`ddl-auto=validate`).

Isso garante que os pedidos sobrevivam ao restart do `order-service` e cria a base transacional necessária para o Outbox.

A limitação atual continua sendo o **dual write**:

```text
commit PostgreSQL
      ↓
publish RabbitMQ
```

Uma falha entre essas duas operações ainda pode deixar estado e evento inconsistentes. A PR de Transactional Outbox elimina essa janela.

## Testes atuais

O repositório possui:

- testes unitários de domínio/casos de uso e controller Java;
- teste do adapter de persistência contra PostgreSQL real via Testcontainers;
- testes unitários do worker Python;
- cenários BDD em Cucumber.

A suíte BDD atual ainda observa a fila usada pelo scheduler, por isso será redesenhada antes de virar gate E2E distribuído definitivo.

## Infraestrutura

O Docker Compose executa:

- PostgreSQL;
- RabbitMQ com credencial local explícita;
- `order-service`;
- `scheduler-agent`.

`infra/terraform` continua documental e não deve ser apresentado como IaC implementado.

## Próximas etapas

```text
Transactional Outbox
-> order.created.v1 no runtime
-> filas por consumidor
-> retry/DLQ
-> E2E distribuído no CI
-> correlação/observabilidade
```

Para detalhes e trade-offs, consulte:

- [`../05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](../05-evolucao/AUDITORIA_ESTADO_ATUAL.md)
- [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md)
- [`../../ROADMAP.md`](../../ROADMAP.md)