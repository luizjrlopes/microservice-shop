# Visão Técnica — microservice-shop

## Stack por serviço

| Serviço | Linguagem | Framework | Comunicação |
|---------|-----------|-----------|-------------|
| order-service | Java 17 | Spring Boot 3 + Spring AMQP | HTTP (entrada) + AMQP (saída) |
| scheduler-agent | Python 3.11 | pika (AMQP) + requests (HTTP) | AMQP (entrada) + HTTP (saída) |
| tests/bdd | TypeScript | Cucumber + axios | HTTP + AMQP |
| ml/experiments | Python | scikit-learn | — (notebooks/scripts standalone) |
| ml/llm | Python | LangChain / OpenAI | — (notebooks standalone) |

## Estrutura do repositório

```
microservice-shop/
├── docker-compose.yml
├── Makefile
├── services/
│   ├── api/order-service/          # Java Spring Boot
│   │   ├── src/main/java/com/shop/order/
│   │   │   ├── OrderApplication.java
│   │   │   ├── application/         # Use cases
│   │   │   ├── domain/              # Entidades
│   │   │   ├── infrastructure/      # Repositório, AMQP, config
│   │   │   └── interfaces/          # Controller HTTP
│   │   └── pom.xml
│   └── workers/scheduler-agent/     # Python worker
│       └── app.py
├── ml/
│   ├── experiments/
│   │   ├── demand-forecasting/      # Previsão de demanda
│   │   └── order-anomaly-detection/ # Detecção de anomalias
│   └── llm/                         # Experimentos LLM
├── tests/
│   └── bdd/                         # Cucumber TypeScript
├── infra/terraform/                 # IaC (stub)
└── docs/
```

## Diagrama de arquitetura

```
[Cliente REST / BDD Tests]
          │ HTTP POST /orders
          ▼
┌─────────────────────────┐
│  order-service :8080    │
│  (Java + Spring Boot)   │
│  InMemoryOrderRepository│
└────────────┬────────────┘
             │ AMQP: order.exchange / order.created
             ▼
┌─────────────────────────┐
│     RabbitMQ :5672      │
│  (management :15672)    │
└────────────┬────────────┘
             │ consume: order.created
             ▼
┌─────────────────────────┐
│  scheduler-agent :Python│
│  → POST /orders/{id}/   │
│       confirm           │
└─────────────────────────┘
```

## Padrão de arquitetura do order-service

Clean Architecture em 4 camadas:
- `domain/` — entidades puras (Order)
- `application/` — use cases (CreateOrderService, ConfirmOrderService)
- `infrastructure/` — repositório, publisher AMQP, configuração
- `interfaces/` — controller HTTP (OrderController)

## Convenções de código

- Java: PascalCase para classes, camelCase para métodos/variáveis
- Python: snake_case para variáveis e funções
- Idioma do código: Inglês
- Diagramas: Mermaid (inline no markdown, sem imagens binárias)
