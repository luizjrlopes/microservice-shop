# Visão Técnica — Microservice Shop

> **Escopo deste documento:** arquitetura atualmente implementada. A evolução futura está em [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## Stack atual

| Componente | Linguagem | Framework / libs | Comunicação |
|---|---|---|---|
| `order-service` | Java 17 | Spring Boot 3, Spring AMQP | HTTP + AMQP |
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
│   │       ├── infrastructure/
│   │       └── interfaces/
│   └── workers/scheduler-agent/
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
    API --> MEM[(InMemoryOrderRepository)]
    API -->|order.created| EX{{order.exchange}}
    EX --> Q[order.created]
    Q --> W[scheduler-agent]
    W -->|POST /orders/{id}/confirm| API
```

## Organização interna do `order-service`

A implementação está separada em quatro grupos:

- `domain/` — entidade `Order`;
- `application/` — casos de uso de criação e confirmação;
- `infrastructure/` — repositório, configuração AMQP e publisher;
- `interfaces/` — controller HTTP.

Essa organização é **inspirada em Clean Architecture**, mas a implementação atual não é tratada como Clean Architecture estrita: `OrderRepository` ainda está em `infrastructure` e é importado pela camada de aplicação.

A evolução prevista cria ports de saída em uma camada adequada e mantém adapters de persistência/mensageria em infraestrutura.

## Contratos atuais

### HTTP

```text
POST /orders
POST /orders/{id}/confirm
```

### AMQP

```text
exchange: order.exchange
routing key: order.created
queue: order.created
payload: id + productId + quantity + status
```

## Persistência

A implementação atual usa `ConcurrentHashMap` através de `InMemoryOrderRepository`.

Isso significa que:

- pedidos não sobrevivem a restart;
- não existe transação de banco;
- persistência e publicação AMQP não são atômicas.

## Testes atuais

O repositório possui:

- testes unitários de aplicação e controller Java;
- testes unitários do worker Python;
- cenários BDD em Cucumber.

A suíte BDD atual observa a própria fila `order.created`, que também é consumida pelo scheduler. Por isso, o desenho atual não é adequado como prova distribuída determinística quando ambos estão ativos.

## Infraestrutura

- Docker Compose executa RabbitMQ, `order-service` e `scheduler-agent`;
- `infra/terraform` é atualmente um placeholder documental, sem recursos Terraform implementados;
- GitHub Actions executa lint, testes unitários e segurança, mas ainda não executa a stack distribuída completa como gate obrigatório.

## Evolução aprovada

O próximo estágio técnico adiciona:

```text
PostgreSQL
+ Transactional Outbox
+ order.created.v1
+ filas por consumidor
+ retry/DLQ
+ idempotência
+ E2E no CI
+ correlação/observabilidade
```

Para detalhes e trade-offs, consulte:

- [`../05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](../05-evolucao/AUDITORIA_ESTADO_ATUAL.md)
- [`../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](../05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md)
- [`../../ROADMAP.md`](../../ROADMAP.md)