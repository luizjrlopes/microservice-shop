# ADR-003 — Retry, DLQ e entrega at-least-once

- **Status:** Aceito
- **Contexto:** o `scheduler-agent` consome `order.created` e confirma o pedido via HTTP. A versão anterior fazia `ACK` em um bloco `finally`, inclusive quando a chamada de confirmação falhava. Isso podia remover uma mensagem sem concluir o trabalho.

## Decisão

O processamento passa a seguir semântica **at-least-once**:

1. o worker usa `auto_ack=false`;
2. mensagens só recebem `ACK` após confirmação HTTP bem-sucedida ou após encaminhamento explícito para DLQ;
3. falhas transitórias recebem `NACK` sem requeue imediato;
4. a fila principal possui dead-letter para `order.retry.exchange`;
5. `order.created.retry` segura a mensagem por TTL e a devolve a `order.exchange`;
6. o header `x-death` do RabbitMQ define quantas reentregas já ocorreram;
7. após `MAX_RETRIES`, o worker publica a mensagem em `order.dlx` / `order.created.dlq`;
8. erros HTTP 4xx e payloads inválidos seguem diretamente para DLQ;
9. a confirmação do pedido permanece idempotente para tolerar reentrega.

## Consequências positivas

- falhas de rede não causam perda silenciosa de eventos;
- retries deixam de formar um loop quente de requeue imediato;
- mensagens problemáticas ficam disponíveis para inspeção;
- o comportamento é observável por logs estruturados com tentativa, duração e motivo;
- o contrato de idempotência deixa de ser apenas documental e passa a ser necessário para a estratégia de entrega.

## Trade-offs

- o fluxo passa a ter mais filas e exchanges;
- uma mensagem pode ser processada mais de uma vez;
- a aplicação precisa tratar operações consumidoras como idempotentes;
- a DLQ exige um procedimento operacional de inspeção/reprocessamento em uma evolução posterior.

## Alternativas rejeitadas

### `ACK` no `finally`

Rejeitado porque confirma ao broker uma mensagem cujo efeito pode não ter ocorrido.

### `basic_nack(..., requeue=true)` ilimitado

Rejeitado porque pode causar loop de reentrega sem atraso e sem orçamento de tentativas.

### Exactly-once distribuído

Não adotado. O custo e a complexidade não se justificam para o domínio. A combinação de at-least-once + idempotência é explícita e suficiente para o fluxo atual.
