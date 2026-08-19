# ADR-003: Transactional Outbox para publicação de eventos

**Status:** Aceita para implementação  
**Data:** 2026-08-19

## Contexto

O fluxo atual salva o pedido e publica `order.created` em operações independentes. Quando um banco real for introduzido, uma falha entre essas operações poderá deixar um pedido persistido sem o evento correspondente.

Uma transação distribuída entre PostgreSQL e RabbitMQ aumentaria muito a complexidade e não é necessária para este projeto.

## Decisão

Persistir `Order` e `OutboxEvent` na **mesma transação PostgreSQL**.

Um publisher separado deve:

1. consultar eventos pendentes;
2. publicar o evento no RabbitMQ;
3. marcar o registro como publicado somente após sucesso;
4. manter o evento pendente quando a publicação falhar.

A janela entre publicar no broker e atualizar `published_at` permite publicação duplicada. Isso é aceito explicitamente.

## Consequência arquitetural

O sistema adota:

```text
atomicidade local no PostgreSQL
+ publicação eventual
+ entrega at-least-once
+ consumidores idempotentes
```

Não é objetivo implementar exactly-once delivery.

## Estrutura mínima

A tabela `outbox_events` deve registrar, no mínimo:

- `id`;
- `aggregate_type`;
- `aggregate_id`;
- `event_type`;
- `event_version`;
- `correlation_id`;
- `payload`;
- `occurred_at`;
- `published_at`;
- `attempts`.

## Alternativas consideradas

### Publicar diretamente depois de `repository.save`

Rejeitada porque mantém o problema de dual write.

### Publicar antes de salvar

Rejeitada porque permite evento existir sem estado persistido correspondente.

### Transação distribuída/2PC

Rejeitada pelo custo operacional e complexidade desproporcionais ao problema.

## Consequências

**Positivas**

- pedido e intenção de publicação são atômicos;
- falha do RabbitMQ não perde o evento;
- comportamento pode ser testado com banco real;
- decisão é compatível com processamento at-least-once.

**Negativas**

- publicação deixa de ser instantaneamente acoplada à requisição;
- exige publisher e política para eventos pendentes;
- consumidores precisam tolerar duplicidade.

## Relações

- substitui a estratégia de publicação direta como desenho-alvo;
- complementa o ADR-004 sobre at-least-once, retry e DLQ;
- contrato-alvo: `contracts/events/order-created-v1.schema.json`.
