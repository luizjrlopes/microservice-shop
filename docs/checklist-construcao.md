# Checklist de Construção — microservice-shop

**Stack:** Java 25 + Spring Boot / Python 3.11 / RabbitMQ / Node.js + Cucumber
**Status:** ✅ MVP completo  
**Última atualização:** 2026-06-22

---

## BLOCO 0 — Setup e Infraestrutura

- [x] **0.1** Estrutura do monorepo: `services/`, `ml/`, `tests/`, `docs/`, `infra/`
- [x] **0.2** Docker Compose: RabbitMQ + order-service + scheduler-agent com healthchecks
- [x] **0.3** Makefile com comandos padronizados
- [x] **0.4** GitHub Actions CI (`.github/workflows/ci.yml`)
- [x] **0.5** AGENTS.md com convenções para todos os serviços

## BLOCO 1 — order-service (Java + Spring Boot)

- [x] **1.1** `POST /orders`: validação, criação, publicação AMQP, retorno 201
- [x] **1.2** `POST /orders/{id}/confirm`: atualização de status
- [x] **1.3** `InMemoryOrderRepository` implementando interface `OrderRepository`
- [x] **1.4** `OrderEventPublisher`: publicação em `order.exchange` / `order.created`
- [x] **1.5** `MessagingConfig`: declaração de exchange e fila
- [x] **1.6** `GET /actuator/health`: health check
- [x] **1.7** Testes unitários: `CreateOrderServiceTest`, `ConfirmOrderServiceTest`, `OrderControllerTest`
- [x] **1.8** Dockerfile

## BLOCO 2 — scheduler-agent (Python)

- [x] **2.1** Conexão AMQP com pika
- [x] **2.2** Consumo da fila `order.created`
- [x] **2.3** Chamada `POST /orders/{id}/confirm` via requests
- [x] **2.4** Ack/nack correto
- [x] **2.5** Dockerfile
- [x] **2.6** Testes unitários (`tests/test_app.py`)

## BLOCO 3 — Testes BDD

- [x] **3.1** Feature file: `tests/bdd/features/order_event.feature`
- [x] **3.2** Steps TypeScript: criação de pedido + verificação de evento

## BLOCO 4 — Experimentos ML

- [x] **4.1** `ml/experiments/demand-forecasting/`: train.py + infer.py + dados de exemplo
- [x] **4.2** `ml/experiments/order-anomaly-detection/`: train.py + infer.py + dados de exemplo
- [x] **4.3** `ml/llm/`: requirements.txt + estrutura de notebooks

---

## Progresso

**Total:** 22 itens | **Concluídos:** 22

```
Progresso: [██████████] 100%
```

## Próximo bloco planejado (pós-MVP)

- [ ] BLOCO 5 — Persistência real: `PostgresOrderRepository` + PostgreSQL no Compose
