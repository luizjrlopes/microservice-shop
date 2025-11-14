# Arquitetura

Este documento descreve a arquitetura atual do Microservice Shop, cobrindo microsserviços, mensagens e contratos expostos.

## Visão em alto nível
```mermaid
flowchart LR
    Client((Cliente REST)) -->|POST /orders| OrderService
    OrderService[order-service \n Spring Boot] -->|evento order.created| RabbitMQ[(RabbitMQ)]
    RabbitMQ --> Scheduler[scheduler-agent \n Python Worker]
    Scheduler -->|POST /orders/{id}/confirm| OrderService
```

- **order-service** (`services/api/order-service`): API HTTP que cria pedidos, persiste em memória e publica eventos.
- **RabbitMQ**: transporte AMQP usando exchange `order.exchange`, routing key `order.created` e fila homônima.
- **scheduler-agent** (`services/workers/scheduler-agent`): worker que consome `order.created` e confirma pedidos.

## Componentes detalhados
| Componente | Linguagem | Entradas | Saídas | Responsabilidade |
| --- | --- | --- | --- | --- |
| order-service (`services/api/order-service`) | Java 17 (Spring Boot) | HTTP (`POST /orders`, `POST /orders/{id}/confirm`), eventos `order.created`. | HTTP 201/200, eventos `order.created`. | Orquestra ciclo de vida do pedido e dispara mensagens para automação downstream. |
| scheduler-agent (`services/workers/scheduler-agent`) | Python 3.11 | Fila `order.created` | HTTP `POST /orders/{id}/confirm` | Automatiza a confirmação assíncrona usando o ID publicado no evento. |
| tests/bdd | Node 18 + Cucumber | HTTP/API, AMQP | Relatórios de testes | Valida cenários ponta-a-ponta usando RabbitMQ real. |

## Contratos principais
### HTTP – order-service
| Método/rota | Payload | Resposta |
| --- | --- | --- |
| `POST /orders` | `{ "productId": string, "quantity": number }` | `201 Created` com `{ "id": string }`. Publica evento `order.created`. |
| `POST /orders/{id}/confirm` | nenhum | `200 OK`. Atualiza estado do pedido. |

### Mensageria
| Exchange | Routing Key | Payload | Consumidor |
| --- | --- | --- | --- |
| `order.exchange` | `order.created` | `{ id, productId, quantity, status }` | `scheduler-agent` (fila `order.created`). |

## Dependências externas
- **RabbitMQ**: obrigatório. Configure via `RABBIT_URL`.
- **Banco de dados**: atualmente `InMemoryOrderRepository`. Planeje integração com PostgreSQL ou Mongo (ver `ROADMAP.md`).

## Extensões planejadas
- Adicionar `catalog-service`, `auth-service` e serviços de pagamento para refletir a visão completa descrita em [`docs/restructure-plan.md`](./restructure-plan.md).
- Instrumentar métricas (Micrometer) e tracing distribuído.
- Conectar notebooks LLM (`ml/llm/notebooks`) para prever demanda e sugerir promoções automatizadas (detalhes em [`docs/experiments-notebooks.md`](./experiments-notebooks.md)).
