# Estado Atual — microservice-shop

> Última atualização: 2026-06-22

## Fase atual

- [x] MVP funcional com mensageria, testes BDD e experimentos ML documentados
- [ ] Persistência real (in-memory ainda)
- [ ] Serviços adicionais (catalog, auth) — roadmap

## O que está pronto

- [x] order-service: `POST /orders`, `POST /orders/{id}/confirm`, publicação AMQP
- [x] scheduler-agent: consumo de `order.created`, confirmação via HTTP
- [x] Docker Compose: RabbitMQ + order-service + scheduler-agent
- [x] Makefile com comandos padronizados
- [x] Testes BDD Cucumber (TypeScript)
- [x] Experimentos ML: demand-forecasting + order-anomaly-detection (dados de exemplo)
- [x] ml/llm: estrutura preparada com requirements
- [x] GitHub Actions CI
- [x] Documentação de arquitetura (Mermaid + contratos)
- [x] ROADMAP.md, CHANGELOG.md, AGENTS.md
- [x] Documentação fase-0 a 04 criada

## O que NÃO está feito (roadmap)

- [ ] Banco de dados real no order-service (PostgreSQL/Mongo)
- [ ] catalog-service, auth-service
- [ ] Retry logic com DLQ no scheduler-agent
- [ ] Métricas e tracing distribuído (Micrometer, OpenTelemetry)
- [ ] Integração LLM no pipeline de produção (hoje só em notebooks)
- [ ] Deploy em cloud

## Problemas conhecidos

- `InMemoryOrderRepository`: dados perdidos ao reiniciar o order-service
