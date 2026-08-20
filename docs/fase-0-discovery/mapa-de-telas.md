# Mapa de Componentes — microservice-shop

> Este é um sistema de microsserviços sem frontend. O "mapa de telas" equivale ao mapa de endpoints e componentes.

## Mapa de serviços

```
[Cliente REST / BDD Tests]
          │
          ▼
[order-service :8080]  ──(AMQP order.created)──►  [RabbitMQ :5672/:15672]
          ▲                                                    │
          │  POST /orders/{id}/confirm                         ▼
          └─────────────────────────────────  [scheduler-agent]
```

## Endpoints do order-service

| Método | Rota                   | Descrição                                 |
| ------ | ---------------------- | ----------------------------------------- |
| POST   | `/orders`              | Criar pedido, publicar evento             |
| POST   | `/orders/{id}/confirm` | Confirmar pedido (chamado pelo scheduler) |
| GET    | `/actuator/health`     | Health check Spring Boot                  |

## Componentes por pasta

| Pasta                                    | Componente                    | Linguagem                 |
| ---------------------------------------- | ----------------------------- | ------------------------- |
| `services/api/order-service`             | API HTTP + publisher AMQP     | Java 25 + Spring Boot     |
| `services/workers/scheduler-agent`       | Worker AMQP consumer          | Python 3.11               |
| `tests/bdd`                              | Testes BDD E2E                | TypeScript + Cucumber     |
| `ml/experiments/demand-forecasting`      | Modelo de previsão de demanda | Python + ML               |
| `ml/experiments/order-anomaly-detection` | Detecção de anomalias         | Python + ML               |
| `ml/llm`                                 | Experimentos LLM              | Python + LangChain/OpenAI |
| `infra/terraform`                        | IaC (stub)                    | Terraform                 |
