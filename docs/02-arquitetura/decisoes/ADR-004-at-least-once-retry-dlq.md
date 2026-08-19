# ADR-004: At-least-once, retry com atraso e Dead Letter Queue

**Status:** Aceita para implementação  
**Data:** 2026-08-19

## Contexto

O `scheduler-agent` atual confirma a mensagem no `finally`, mesmo quando a chamada HTTP pode falhar. Isso permite perda silenciosa do trabalho.

Além disso, a topologia atual não diferencia falhas transitórias de mensagens definitivamente inválidas.

## Decisão

O consumidor do evento `order.created.v1` adota semântica **at-least-once**.

### Sucesso

HTTP 2xx na confirmação do pedido:

- registrar sucesso;
- ACK da mensagem.

### Falha transitória

Exemplos:

- timeout;
- connection error;
- HTTP 5xx.

Ação:

- não considerar o efeito concluído;
- NACK sem requeue imediato;
- encaminhar para fila de retry com atraso;
- retornar à fila principal após TTL;
- limitar quantidade de tentativas.

### Falha definitiva

Exemplos:

- JSON inválido;
- contrato incompatível;
- campos obrigatórios ausentes;
- HTTP 4xx classificado como não recuperável.

Ação:

- enviar para DLQ;
- preservar payload e metadados;
- ACK da mensagem original apenas depois de a transferência ser aceita pelo broker.

### Retries esgotados

- enviar para DLQ;
- registrar número de tentativas e motivo final.

## Topologia-alvo

```text
exchange: orders.events
routing key: order.created.v1

scheduler-agent
├── queue: scheduler.order-created.v1
├── retry: scheduler.order-created.retry.v1
└── dlq:   scheduler.order-created.dlq.v1
```

A fila pertence ao consumidor. O produtor conhece apenas o contrato do evento e o exchange/routing key.

## Idempotência

At-least-once permite reentrega. Por isso, confirmar um pedido que já está `CONFIRMED` deve ser uma operação segura e sem efeito incorreto.

A primeira implementação pode garantir idempotência pelo estado de domínio do pedido. Se efeitos externos adicionais forem introduzidos no futuro, uma estratégia explícita de deduplicação por `eventId` deverá ser avaliada.

## Alternativas consideradas

### ACK sempre no final do callback

Rejeitada porque falhas podem desaparecer sem reprocessamento.

### `requeue=true` imediato

Rejeitada porque pode criar hot loop durante indisponibilidade persistente.

### Exactly-once

Não adotada. RabbitMQ e chamadas HTTP entre processos tornam essa promessa inadequada sem complexidade desproporcional.

## Consequências

**Positivas**

- falha transitória deixa evidência e pode ser recuperada;
- mensagens inválidas não bloqueiam a fila principal;
- comportamento fica testável;
- semântica distribuída fica explícita.

**Negativas**

- consumidor precisa tolerar duplicidade;
- topologia RabbitMQ fica mais rica;
- retry/DLQ exigem operação e observabilidade.

## Relações

- depende do contrato `order.created.v1`;
- complementa o ADR-003 de Transactional Outbox;
- observabilidade deve registrar `eventId`, `correlationId`, `orderId`, tentativa e motivo de falha.
