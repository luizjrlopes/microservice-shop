# Escopo Atual — Microservice Shop

Este documento separa **implementado**, **em evolução** e **fora do núcleo atual**.

## Implementado

| Capacidade                                          | Estado                |
| --------------------------------------------------- | --------------------- |
| `order-service` em Java 25 + Spring Boot            | ✅                    |
| `POST /orders`                                      | ✅                    |
| `GET /orders/{id}`                                  | ✅                    |
| `POST /orders/{id}/confirm`                         | ✅                    |
| domínio com invariantes e `OrderStatus`             | ✅                    |
| confirmação repetida segura                         | ✅                    |
| PostgreSQL como persistência                        | ✅                    |
| migrações Flyway                                    | ✅                    |
| port de repositório na camada de aplicação          | ✅                    |
| adapter JPA/PostgreSQL                              | ✅                    |
| publicação do evento legado `order.created`         | ✅                    |
| `scheduler-agent` Python                            | ✅                    |
| Docker Compose com PostgreSQL + RabbitMQ + serviços | ✅                    |
| testes unitários Java e Python                      | ✅                    |
| teste PostgreSQL via Testcontainers                 | ✅                    |
| BDD Cucumber                                        | ✅                    |
| contratos `order.created.v1` em `contracts/`        | ✅ como contrato-alvo |
| experimentos ML clássicos                           | ✅ experimental       |

## Limitações conhecidas

- persistência e publicação AMQP ainda formam um dual write;
- `order.created.v1` ainda não é o contrato publicado pelo runtime;
- o worker ainda precisa de timeout, ACK seguro, retry e DLQ;
- a suíte BDD atual ainda não prova o ciclo distribuído de forma determinística;
- CI ainda não executa a stack completa como gate;
- observabilidade distribuída ainda não foi implementada;
- LLM não é uma feature integrada;
- Terraform continua apenas documental.

## Próxima evolução aprovada

### PR-03 — Transactional Outbox

- tabela `outbox_events`;
- persistência de `Order + OutboxEvent` na mesma transação;
- publisher de eventos pendentes;
- adoção runtime de `order.created.v1`;
- eliminação da publicação direta do controller.

### PR-04 — semântica de entrega

- timeout HTTP;
- ACK somente após sucesso;
- retry com atraso;
- DLQ;
- topologia de filas por consumidor.

As etapas seguintes cobrem E2E/CI e observabilidade.

## Fora do núcleo atual

- frontend;
- `catalog-service`;
- `auth-service` dedicado;
- `payment-service`;
- Kubernetes;
- múltiplas clouds;
- `AI Advisor`/serviço LLM.

Uma nova tecnologia só entra quando resolve um problema concreto e pode ser validada por teste reproduzível.
