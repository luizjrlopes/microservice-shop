# Glossário — microservice-shop

## Termos do domínio

| Termo | Definição |
|-------|-----------|
| Pedido (`Order`) | Solicitação de compra com productId e quantity; ciclo de vida: criado → confirmado |
| Evento (`order.created`) | Mensagem publicada no RabbitMQ após criação de pedido; payload: `{ id, productId, quantity, status }` |
| Exchange | Ponto de entrada no RabbitMQ; aqui: `order.exchange` (direct) |
| Fila (`queue`) | Armazenamento de mensagens no RabbitMQ; aqui: `order.created` |
| Routing Key | Identificador que direciona a mensagem para a fila correta; aqui: `order.created` |

## Termos técnicos

| Termo | Significado |
|-------|-------------|
| `InMemoryOrderRepository` | Repositório em memória do order-service — sem persistência real |
| `scheduler-agent` | Worker Python que consome `order.created` e confirma pedidos via HTTP |
| `actuator/health` | Endpoint Spring Boot Actuator para healthcheck |

## O que NÃO usar

| Evitar | Usar |
|--------|------|
| "pedido processado" | "pedido confirmado" |
| "fila de mensagens" | "fila RabbitMQ" ou simplesmente "fila" |
