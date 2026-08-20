# Arquitetura Atual — Microservice Shop

> Este documento descreve **somente o que está implementado nesta versão**. Evoluções futuras permanecem em [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md) e no [`ROADMAP.md`](../ROADMAP.md).

## Visão em alto nível

```mermaid
flowchart LR
    Client((Cliente REST)) -->|POST /orders| API[order-service\nSpring Boot]
    Client -->|GET /orders/{id}| API
    API -->|transação JPA| DB[(PostgreSQL\norders + outbox_events)]
    PUB[Outbox Publisher] -->|lê pendentes| DB
    PUB -->|order.created.v1| EX{{orders.events\nRabbitMQ}}
    EX --> Q[scheduler.order-created.v1]
    Q --> Worker[scheduler-agent\nPython]
    Worker -->|POST /orders/{id}/confirm| API
    Worker --> Retry[scheduler retry queue]
    Retry -->|TTL + dead letter| EX
    Worker --> DLQ[scheduler DLQ]
```

## Componentes

| Componente | Tecnologia | Responsabilidade implementada |
|---|---|---|
| `order-service` | Java 25 + Spring Boot | cria, consulta e confirma pedidos; persiste Outbox; publica eventos pendentes |
| PostgreSQL | PostgreSQL 16 | persiste pedidos e `outbox_events` |
| Flyway | Flyway | versiona schemas de pedidos e Outbox |
| RabbitMQ | AMQP | transporta `order.created.v1`, retry e DLQ |
| `scheduler-agent` | Python 3.11 + pika + requests | consome evento, confirma pedido e aplica política de retry/DLQ |
| Prometheus | profile Compose opcional | coleta métricas do serviço e do worker |
| `tests/bdd` | TypeScript + Cucumber | prova fluxo HTTP + AMQP distribuído |

## Contratos HTTP

| Método | Rota | Comportamento |
|---|---|---|
| `POST` | `/orders` | cria pedido `PENDING` e retorna `201` |
| `GET` | `/orders/{id}` | retorna o estado persistido |
| `POST` | `/orders/{id}/confirm` | confirma o pedido; repetição segura |

Entradas inválidas retornam `400`; pedido inexistente retorna `404`.

## Consistência transacional

A criação executa pedido + evento como uma única unidade transacional:

```text
BEGIN
  INSERT orders
  INSERT outbox_events(order.created.v1)
COMMIT
```

O `PublishPendingOutboxEventsService` consulta eventos pendentes em lotes. `RabbitOutboxEventPublisher` publica no exchange `orders.events` usando routing key `order.created.v1`, mensagem persistente e publisher confirm. O registro só recebe `published_at` após ACK do broker; falhas incrementam o estado de tentativa e mantêm o evento pendente.

Isso remove a antiga janela de dual write `commit PostgreSQL -> publish RabbitMQ` do caso de uso de criação.

## Contrato AMQP

| Item | Valor |
|---|---|
| Exchange | `orders.events` |
| Tipo | `topic` |
| Routing key | `order.created.v1` |
| Fila do scheduler | `scheduler.order-created.v1` |
| Retry queue | `scheduler.order-created.retry.v1` |
| DLQ | `scheduler.order-created.dlq.v1` |
| Contrato | `contracts/events/order-created-v1.schema.json` + `contracts/asyncapi.yaml` |

O envelope contém `eventId`, `eventType`, `eventVersion`, `correlationId` e `payload`. O publisher também propaga IDs relevantes em propriedades AMQP.

## Semântica do consumidor

O `scheduler-agent` opera com `auto_ack=False` e prefetch limitado. O fluxo é:

```text
mensagem válida
  -> POST /orders/{id}/confirm com timeout
  -> sucesso: ACK
  -> falha transitória: publica em retry queue + ACK original
  -> retry queue expira por TTL e retorna ao exchange
  -> retries esgotados: publica em DLQ + ACK original
  -> evento inválido: DLQ + ACK original

se publicar retry/DLQ falhar
  -> NACK requeue da mensagem original
```

A confirmação de domínio é idempotente, reduzindo o risco de efeitos incorretos em reentregas at-least-once.

## Persistência

- `V1__create_orders.sql` cria a estrutura de pedidos;
- `V2__create_outbox_events.sql` cria o Outbox;
- Hibernate opera com schema validado;
- integração transacional usa PostgreSQL real via Testcontainers.

## Observabilidade implementada

- Spring Actuator/Micrometer expõe métricas do `order-service`;
- `scheduler-agent` expõe contadores de processamento, retry e DLQ e histograma de latência de confirmação;
- o worker emite logs JSON com `eventId`, `correlationId` e `orderId` nas transições principais;
- `infra/observability/prometheus.yml` coleta os dois endpoints quando o profile `observability` é ativado.

A correlação ponta a ponta completa e a instrumentação OpenTelemetry continuam como evolução futura.

## Testes e gates

O repositório possui:

- testes Java de domínio, aplicação, adapters e Outbox;
- integração PostgreSQL via Testcontainers;
- testes Python de ACK, retry, DLQ, evento inválido e falha ao republicar;
- BDD distribuído com fila exclusiva de auditoria;
- CI separado em `quality`, `unit-tests`, `security` e `integration-e2e`;
- coleta de estado/logs dos containers quando o E2E falha.

## Execução local

O Docker Compose contém PostgreSQL, RabbitMQ, `order-service` e `scheduler-agent`. O profile opcional `observability` adiciona Prometheus.

```bash
docker compose up -d --build
make compose-observability
```

## Próximas mudanças

O core de confiabilidade já está materializado. Os próximos ganhos estão em **prova e operação**, não em adicionar serviços por quantidade:

1. integração dedicada RabbitMQ + Outbox;
2. auditoria de dependências mais abrangente;
3. correlação/logging consistente no `order-service`;
4. métricas de domínio e Outbox;
5. dashboards/alertas e troubleshooting orientado a sinais;
6. OpenTelemetry, se continuar agregando evidência operacional.

Depois do gate de observabilidade, o projeto pode escolher uma única frente de diferenciação: Cloud/IaC executável ou integração real de uma capacidade de ML.