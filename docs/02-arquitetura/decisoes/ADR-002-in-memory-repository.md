# ADR-002: InMemoryOrderRepository no MVP

**Data:** 2026-06-22  
**Status:** Aceita (temporária — prevista migração para banco real)

## Contexto

O order-service precisa de um mecanismo de persistência. Para o MVP, adicionar PostgreSQL aumentaria a complexidade sem acrescentar valor à demonstração dos padrões de mensageria.

## Decisão

**Usar `InMemoryOrderRepository`** implementando a interface `OrderRepository`. Quando a migração para banco real for necessária, basta criar `PostgresOrderRepository` com a mesma interface — o restante do código não muda (Clean Architecture).

## Consequências

**Positivas:** Stack simples; foco na mensageria  
**Negativas:** Dados perdidos ao reiniciar; não adequado para produção  
**Próximo passo:** ADR-003 definirá o banco real (PostgreSQL ou MongoDB)
