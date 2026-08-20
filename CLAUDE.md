# CLAUDE.md — Contexto para o AI

> Este arquivo é lido automaticamente pelo Claude em cada sessão. Mantenha-o sempre atualizado.

## O que é este projeto

**Produto:** microservice-shop — Monorepo de plataforma de pedidos que demonstra padrões de microsserviços: mensageria assíncrona (RabbitMQ), automação com worker, testes BDD e trilhas de experimentação com IA/LLM.  
**Público:** Portfólio/estudo do desenvolvedor — demonstra múltiplos padrões em um único repositório  
**Stack:** Java 25 + Spring Boot (order-service), Python 3.11 (scheduler-agent), Node.js 18 + Cucumber (BDD), RabbitMQ, Docker Compose

## Estado atual

**Fase:** MVP funcional com trilhas de evolução documentadas  
**Último trabalho:** Implementação do order-service (Java/Spring Boot), scheduler-agent (Python), testes BDD, experimentos ML/LLM, documentação de arquitetura e roadmap  
**Próximo passo:** Adicionar persistência real (PostgreSQL/Mongo) ao order-service; evoluir experimentos LLM

## Regras que o AI não pode violar

- [ ] Cada serviço tem sua própria linguagem/stack — não misturar Java no worker Python nem vice-versa
- [ ] O order-service publica evento `order.created` no exchange `order.exchange` com routing key `order.created`
- [ ] O scheduler-agent é o único consumidor da fila `order.created`
- [ ] O scheduler confirma pedidos via `POST /orders/{id}/confirm` — não acessa o banco diretamente
- [ ] Notebooks e experimentos ML vivem em `ml/` — não no código de produção

## Arquitetura em uma linha

```
[Cliente REST] → [order-service :8080] → [RabbitMQ] → [scheduler-agent] → [POST /orders/{id}/confirm]
```

## Padrões de código

- Java: Spring Boot + padrões de Clean Architecture (application/domain/infrastructure/interfaces)
- Python: worker simples sem frameworks pesados
- TypeScript: Cucumber BDD para testes E2E
- Commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`)
- Branches: `feat/<nome>`, `fix/<nome>`, `docs/<nome>`

## Onde estão as coisas

| O que | Onde |
|-------|------|
| Serviço de pedidos (Java) | `services/api/order-service/` |
| Worker de confirmação (Python) | `services/workers/scheduler-agent/` |
| Testes BDD | `tests/bdd/` |
| Experimentos ML/LLM | `ml/` |
| Documentação de arquitetura | `docs/architecture.md` |
| Variáveis de ambiente | `docs/setup.md` + esta doc |
| Roadmap | `ROADMAP.md` |
