# Regras de Negócio — microservice-shop

## Regras do order-service

**RN-001: Criação de pedido publica evento**
- **O que:** Todo `POST /orders` bem-sucedido deve publicar `order.created` no RabbitMQ antes de retornar 201.
- **Por quê:** O scheduler só confirma pedidos que chegam via evento; sem publicação, o pedido nunca é confirmado.
- **Exemplo:** `POST /orders` → `201 Created` + evento publicado.

**RN-002: Confirmação é idempotente**
- **O que:** `POST /orders/{id}/confirm` pode ser chamado múltiplas vezes sem efeito colateral negativo.
- **Por quê:** O scheduler trabalha com entrega at-least-once e pode receber a mesma mensagem novamente.

**RN-003: productId e quantity são obrigatórios**
- **O que:** `POST /orders` sem `productId`, com `productId` vazio ou com `quantity ≤ 0` retorna 400.
- **Por quê:** Pedido sem produto ou quantidade válida não faz sentido de negócio.

**RN-004: estado do pedido é consultável pela API**
- **O que:** `GET /orders/{id}` retorna o estado atual do pedido.
- **Por quê:** Consumidores e testes de ponta a ponta precisam observar a transição assíncrona sem acessar o repositório interno.

## Regras do scheduler-agent

**RN-010: Confirmar pedido via HTTP, nunca via repositório interno**
- **O que:** O scheduler chama `POST /orders/{id}/confirm` no `order-service` e não acessa diretamente seu estado.
- **Por quê:** A fronteira do microsserviço deve ser preservada.

**RN-011: Falha transitória não pode gerar ACK**
- **O que:** Se a confirmação falhar por erro de rede ou HTTP 5xx, o scheduler envia `NACK` sem requeue imediato. O RabbitMQ encaminha a mensagem para a fila de retry e a devolve após o TTL.
- **Por quê:** A mensagem só pode ser removida do caminho principal quando o trabalho foi concluído ou quando a falha foi explicitamente classificada para DLQ.

**RN-012: Retry possui orçamento finito**
- **O que:** O número máximo de retries é configurável por `MAX_RETRIES`.
- **Por quê:** Evitar loops infinitos e tornar falhas persistentes visíveis operacionalmente.

**RN-013: Falhas terminais seguem para DLQ**
- **O que:** payload inválido, HTTP 4xx ou orçamento de retries esgotado encaminha o evento para `order.created.dlq`.
- **Por quê:** Eventos problemáticos precisam ser preservados para investigação em vez de descartados silenciosamente.

## Regras de mensageria

**RN-020: Payload do evento**
- **O que:** `order.created` contém no mínimo `{ eventId, occurredAt, id, productId, quantity, status }`.
- **Por quê:** O scheduler precisa do `id`; os metadados adicionais suportam rastreabilidade e correlação.

**RN-021: Um consumidor funcional por fila principal**
- **O que:** A fila `order.created` é consumida pelo `scheduler-agent`. Testes que precisam observar eventos devem criar uma fila de auditoria própria ligada ao exchange.
- **Por quê:** Consumir a fila operacional durante testes concorreria com o worker e alteraria o comportamento do sistema.

**RN-022: Retry deve ter atraso**
- **O que:** `order.created.retry` aplica TTL antes de devolver a mensagem à fila principal.
- **Por quê:** Requeue imediato pode gerar loop quente, aumentar carga e piorar indisponibilidades temporárias.

**RN-023: Eventos possuem identidade e correlação**
- **O que:** mensagens publicadas possuem `eventId`, `messageId`, `correlationId` e timestamp.
- **Por quê:** Permitir rastrear a mesma operação através da API, broker e worker.
