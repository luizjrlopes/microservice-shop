# Próximo Passo — microservice-shop

## Prioridade alta

**Adicionar persistência real ao order-service:**
1. Criar `PostgresOrderRepository` implementando `OrderRepository`
2. Adicionar `spring-data-jpa` e driver PostgreSQL no `pom.xml`
3. Adicionar serviço PostgreSQL no `docker-compose.yml`
4. Criar migration SQL ou usar `spring.jpa.hibernate.ddl-auto=update` no início
5. Atualizar `docs/02-arquitetura/modelos-de-dados.md` com o schema real

**Prompt sugerido:**
```
No microservice-shop, implemente persistência PostgreSQL no order-service.
Leia docs/02-arquitetura/modelos-de-dados.md e docs/02-arquitetura/decisoes/ADR-002-in-memory-repository.md.
Crie PostgresOrderRepository implementando a interface OrderRepository existente.
Pronto quando: POST /orders persiste e sobrevive ao restart do container.
```

## Prioridade média

- Retry logic com DLQ no scheduler-agent (para falhas de rede na confirmação)
- Adicionar `catalog-service` em Python ou Node.js

## Prompt de início sugerido

```
Estou retomando o microservice-shop.
Leia CLAUDE.md e docs/03-ia-context/estado-atual.md.
A tarefa de hoje é: [descreva aqui]
```
