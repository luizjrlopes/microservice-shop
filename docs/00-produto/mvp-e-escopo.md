# MVP e Escopo — microservice-shop

## O que está implementado

| Componente | Status |
|-----------|--------|
| order-service (Spring Boot): POST /orders + POST /orders/{id}/confirm | ✅ |
| Publicação de evento `order.created` no RabbitMQ | ✅ |
| scheduler-agent (Python): consumidor AMQP + chamada de confirmação | ✅ |
| Testes BDD Cucumber (TypeScript) | ✅ |
| Docker Compose com RabbitMQ, order-service e scheduler-agent | ✅ |
| Makefile com comandos padronizados | ✅ |
| Experimentos ML: previsão de demanda + detecção de anomalias | ✅ (dados de exemplo) |
| Documentação de arquitetura (Mermaid + contratos) | ✅ |
| GitHub Actions CI | ✅ |

## O que está FORA do MVP atual

- [ ] Banco de dados real (order-service usa in-memory)
- [ ] Autenticação/autorização
- [ ] Frontend
- [ ] catalog-service, auth-service (planejados no roadmap)
- [ ] Retry logic com dead letter queue (DLQ)
- [ ] Deploy em cloud

## Roadmap pós-MVP

- **v1.1:** Persistência real (PostgreSQL) no order-service
- **v1.2:** catalog-service + auth-service
- **v2.0:** Integração LLM no pipeline (copiloto operacional + previsão)
