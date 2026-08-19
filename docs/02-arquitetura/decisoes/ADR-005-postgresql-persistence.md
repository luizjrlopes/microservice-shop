# ADR-005: PostgreSQL como persistência do order-service

**Status:** Aceita  
**Data:** 2026-08-19

## Contexto

O `InMemoryOrderRepository` simplificou a fase inicial, mas não permite provar persistência entre reinícios, transações reais ou a futura atomicidade entre `Order` e `OutboxEvent`.

A próxima etapa arquitetural exige um banco transacional com suporte sólido a UUID, timestamps, constraints, JSON e integração madura com Spring.

## Decisão

Usar **PostgreSQL** como persistência principal do `order-service` no ambiente local de referência.

A implementação usa:

- Spring Data JPA;
- adapter de persistência em `infrastructure/persistence`;
- port `OrderRepository` em `application/ports`;
- Flyway para migrações;
- Hibernate com `ddl-auto=validate`;
- PostgreSQL em Docker Compose;
- Testcontainers para teste de integração do adapter.

## Consequências

**Positivas**

- dados sobrevivem a restart;
- constraints e transações são reais;
- prepara o Transactional Outbox;
- permite testes de persistência contra o mesmo tipo de banco do runtime;
- remove dependência da aplicação em uma implementação in-memory.

**Negativas**

- adiciona um serviço ao ambiente local;
- aumenta o tempo de inicialização/teste de integração;
- exige migrações e gestão explícita de schema.

## Alternativas consideradas

### Continuar in-memory

Rejeitada porque impede validar a propriedade transacional necessária ao Outbox.

### MongoDB

Não escolhido para este fluxo porque a evolução arquitetural depende de uma transação relacional simples entre pedido e registro de Outbox, além de constraints de domínio facilmente auditáveis.

## Relações

- **substitui:** ADR-002 — `InMemoryOrderRepository`;
- **habilita:** ADR-003 — Transactional Outbox;
- **não implementa ainda:** Outbox ou mudança do contrato AMQP runtime.
