# Modelos de Dados — microservice-shop

## Entidade: Order (order-service)

> Atualmente em memória (`InMemoryOrderRepository`). Quando migrar para banco, usar este modelo como referência.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `id` | String (UUID) | Sim | PK — gerado no service |
| `productId` | String | Sim | Identificador do produto |
| `quantity` | Integer | Sim | Quantidade (> 0) |
| `status` | OrderStatus | Sim | `CREATED` → `CONFIRMED` |

**Status possíveis:** `CREATED`, `CONFIRMED`

## Evento: order.created (RabbitMQ)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | String | ID do pedido |
| `productId` | String | ID do produto |
| `quantity` | Integer | Quantidade |
| `status` | String | Status ao publicar (sempre `CREATED`) |

## Dados de experimento ML

### demand-forecasting (`data/sample_demand.csv`)
| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `date` | Date | Data da observação |
| `product_id` | String | Produto |
| `quantity` | Integer | Quantidade demandada |

### order-anomaly-detection (`data/sample_order_metrics.csv`)
| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `order_id` | String | ID do pedido |
| `amount` | Float | Valor do pedido |
| `quantity` | Integer | Quantidade |
| `is_anomaly` | Boolean | Label de anomalia (para treinamento) |

## Quando adicionar banco real

Ao migrar para PostgreSQL/Mongo, criar migration a partir deste modelo. O `InMemoryOrderRepository` implementa a mesma interface `OrderRepository` — basta criar `PostgresOrderRepository` implementando a mesma interface.
