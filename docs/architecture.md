# Arquitetura Atual — Microservice Shop

> Este documento descreve **somente o que está implementado hoje**. A arquitetura-alvo está em [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## Visão em alto nível

```mermaid
flowchart LR
    Client((Cliente REST)) -->|POST /orders| OrderService
    OrderService[order-service\nSpring Boot] -->|save| Repo[(InMemoryOrderRepository)]
    OrderService -->|order.created| RabbitMQ[(RabbitMQ)]
    RabbitMQ --> Scheduler[scheduler-agent\nPython]
    Scheduler -->|POST /orders/{id}/confirm| OrderService
```

## Componentes

| Componente | Tecnologia | Responsabilidade atual |
|---|---|---|
| `order-service` | Java 17 + Spring Boot | cria e confirma pedidos; publica `order.created` |
| `InMemoryOrderRepository` | Java | mantém pedidos durante a vida do processo |
| RabbitMQ | AMQP | transporta eventos entre produtor e consumidor |
| `scheduler-agent` | Python 3.11 + pika + requests | consome evento e chama a confirmação HTTP |
| `tests/bdd` | TypeScript + Cucumber | exercita API e RabbitMQ |

## Contratos HTTP atuais

| Método | Rota | Comportamento |
|---|---|---|
| `POST` | `/orders` | cria um pedido e retorna `201` com `{ "id": string }` |
| `POST` | `/orders/{id}/confirm` | marca o pedido como `CONFIRMED` e retorna `200` |

O estado atual ainda não expõe `GET /orders/{id}`.

## Contrato AMQP atual

| Item | Valor |
|---|---|
| Exchange | `order.exchange` |
| Routing key | `order.created` |
| Fila | `order.created` |
| Payload | `{ id, productId, quantity, status }` |

Essa topologia funciona para o fluxo mínimo, mas conflui conceito de evento e fila do consumidor. A evolução aprovada separa esses papéis e introduz versionamento explícito.

## Persistência atual

O `order-service` usa `InMemoryOrderRepository`, baseado em `ConcurrentHashMap`.

Consequências:

- reiniciar o processo remove os pedidos;
- não há transação de banco;
- não é possível garantir atomicidade entre persistência e publicação;
- o desenho atual é adequado apenas ao estágio presente do projeto.

## Fronteiras arquiteturais

O código está organizado em `domain`, `application`, `infrastructure` e `interfaces`, inspirado em Clean Architecture. Entretanto, o contrato `OrderRepository` ainda reside em `infrastructure` e é importado pelos casos de uso.

Por isso, a documentação **não afirma que a implementação atual segue Clean Architecture estrita**. A evolução prevista move ports de saída para uma camada adequada e mantém adapters em infraestrutura.

## Limitações distribuídas conhecidas

As limitações mais importantes hoje são:

- ACK do worker não depende de sucesso real da operação HTTP;
- chamada HTTP sem timeout explícito;
- ausência de retry/DLQ completos;
- dual write entre persistência do pedido e publicação no broker;
- confirmação não modelada como operação idempotente explícita;
- evento sem envelope/versionamento/correlação suficientes;
- BDD competindo pela fila do worker;
- CI sem gate E2E distribuído.

Esses pontos não são bugs escondidos da documentação: estão registrados formalmente na [`05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](05-evolucao/AUDITORIA_ESTADO_ATUAL.md).

## Arquitetura-alvo

A evolução aprovada introduz:

- PostgreSQL;
- Transactional Outbox;
- evento `order.created.v1`;
- fila exclusiva do `scheduler-agent`;
- retry com atraso e DLQ;
- idempotência;
- contratos versionados;
- E2E distribuído no CI;
- observabilidade correlacionada.

O detalhamento e a sequência estão em [`05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md) e no [`../ROADMAP.md`](../ROADMAP.md).