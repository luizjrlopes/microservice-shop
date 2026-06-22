# Regras de Negócio — microservice-shop

## Regras do order-service

**RN-001: Criação de pedido publica evento**
- **O que:** Todo `POST /orders` bem-sucedido deve publicar `order.created` no RabbitMQ antes de retornar 201
- **Por quê:** O scheduler só confirma pedidos que chegam via evento — sem publicação, o pedido nunca é confirmado
- **Exemplo:** POST /orders → 201 Created + evento publicado

**RN-002: Confirmação é idempotente**
- **O que:** `POST /orders/{id}/confirm` pode ser chamado múltiplas vezes sem efeito colateral negativo
- **Por quê:** O scheduler pode reprocessar a mensagem em falhas; não deve gerar inconsistência

**RN-003: productId e quantity são obrigatórios**
- **O que:** POST /orders sem productId ou com quantity ≤ 0 retorna 400
- **Por quê:** Pedido sem produto ou quantidade zero não faz sentido de negócio

## Regras do scheduler-agent

**RN-010: Confirmar pedido via HTTP, nunca via banco**
- **O que:** O scheduler chama `POST /orders/{id}/confirm` no order-service — não acessa o banco diretamente
- **Por quê:** Respeitar a fronteira do microsserviço; o banco é privado ao order-service

**RN-011: Reprocessar em caso de falha de rede**
- **O que:** Se o POST de confirmação falhar, o scheduler não deve fazer ack da mensagem (RabbitMQ reentregará)
- **Por quê:** Garantia de entrega at-least-once

## Regras de mensageria

**RN-020: Payload do evento**
- **O que:** `order.created` deve conter no mínimo: `{ id, productId, quantity, status }`
- **Por quê:** O scheduler precisa do `id` para confirmar; os outros campos são para rastreabilidade

**RN-021: Um consumidor por fila**
- **O que:** A fila `order.created` é consumida somente pelo scheduler-agent
- **Por quê:** Evitar processamento duplicado e manter responsabilidade única
