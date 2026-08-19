# Arquitetura Atual — Microservice Shop

> Este documento descreve **somente o que está implementado nesta versão**. A arquitetura-alvo está em [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## Visão em alto nível

```mermaid
flowchart LR
    Client((Cliente REST)) -->|POST /orders| OrderService
    Client -->|GET /orders/{id}| OrderService
    OrderService[order-service\nSpring Boot] -->|JPA| PostgreSQL[(PostgreSQL)]
    OrderService -->|order.created| RabbitMQ[(RabbitMQ)]
    RabbitMQ --> Scheduler[scheduler-agent\nPython]
    Scheduler -->|POST /orders/{id}/confirm| OrderService
```

## Componentes

| Componente | Tecnologia | Responsabilidade atual |
|---|---|---|
| `order-service` | Java 17 + Spring Boot | cria, consulta e confirma pedidos; publica `order.created` |
| PostgreSQL | PostgreSQL 16 | persiste pedidos |
| Flyway | Flyway | versiona o schema de dados |
| RabbitMQ | AMQP | transporta eventos entre produtor e consumidor |
| `scheduler-agent` | Python 3.11 + pika + requests | consome evento e chama a confirmação HTTP |
| `tests/bdd` | TypeScript + Cucumber | exercita API e RabbitMQ |

## Contratos HTTP atuais

| Método | Rota | Comportamento |
|---|---|---|
| `POST` | `/orders` | cria pedido `PENDING` e retorna `201` com o ID |
| `GET` | `/orders/{id}` | retorna o estado persistido do pedido |
| `POST` | `/orders/{id}/confirm` | confirma o pedido e retorna `200` |

Entradas inválidas de criação retornam `400`; pedido inexistente retorna `404`.

## Domínio

`Order` agora possui:

- ID UUID;
- `productId` obrigatório;
- `quantity > 0`;
- `OrderStatus` explícito (`PENDING`, `CONFIRMED`);
- timestamps de criação/atualização;
- confirmação repetida segura.

O contrato `OrderRepository` é um port da camada de aplicação. A implementação PostgreSQL fica em `infrastructure/persistence`.

## Persistência

A persistência usa Spring Data JPA sobre PostgreSQL. O schema é criado por `V1__create_orders.sql` e o Hibernate opera em modo `validate`, evitando criação implícita de tabelas.

O adapter de persistência possui teste contra PostgreSQL real via Testcontainers.

## Contrato AMQP atual

O runtime ainda publica o contrato legado:

| Item | Valor |
|---|---|
| Exchange | `order.exchange` |
| Routing key | `order.created` |
| Fila | `order.created` |
| Payload | `{ id, productId, quantity, status }` |

O contrato `order.created.v1` já está definido em `contracts/`, mas sua adoção no runtime pertence à PR de Transactional Outbox.

## Limitação estrutural atual

Persistir o pedido e publicar no RabbitMQ continuam sendo duas operações separadas:

```text
PostgreSQL commit
      ↓
RabbitMQ publish
```

Essa janela de dual write é conhecida e será removida pela Transactional Outbox na próxima etapa.

O worker também ainda mantém a semântica de entrega antiga; timeout, ACK correto, retry e DLQ pertencem à etapa seguinte.

## Execução local

O Docker Compose contém:

- PostgreSQL;
- RabbitMQ com credencial local explícita;
- `order-service`;
- `scheduler-agent`.

O `order-service` espera PostgreSQL e RabbitMQ saudáveis antes de iniciar o fluxo.

## Próximas mudanças

A próxima evolução adiciona:

- tabela `outbox_events`;
- persistência atômica de pedido + evento;
- publisher de Outbox;
- adoção runtime de `order.created.v1`.

Depois disso entram semântica at-least-once, retry/DLQ, E2E distribuído e observabilidade.