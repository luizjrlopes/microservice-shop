# ADR-002: InMemoryOrderRepository no estágio inicial

**Data:** 2026-06-22  
**Status:** **Substituída pelo ADR-005**

## Contexto original

O `order-service` precisava de um mecanismo de persistência simples enquanto o foco inicial estava na comunicação HTTP/AMQP. O repositório in-memory reduziu dependências e permitiu construir o primeiro fluxo distribuído.

## Decisão original

Usar `InMemoryOrderRepository` como adapter temporário do contrato de repositório.

## Motivo da substituição

A evolução atual precisa provar:

- persistência entre reinícios;
- constraints de domínio em banco;
- transações reais;
- futura atomicidade entre `Order` e `OutboxEvent`.

Essas propriedades não podem ser demonstradas com `ConcurrentHashMap`.

## Decisão vigente

O ADR-005 adota PostgreSQL + Flyway + adapter JPA e posiciona `OrderRepository` como port da camada de aplicação.

Este ADR permanece apenas para registrar a evolução arquitetural.