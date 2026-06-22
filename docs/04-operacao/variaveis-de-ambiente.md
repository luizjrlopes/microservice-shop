# Variáveis de Ambiente — microservice-shop

## Variáveis por serviço

### order-service

| Variável | Exemplo | Obrigatória | Descrição |
|----------|---------|-------------|-----------|
| `RABBIT_URL` | `amqp://guest:guest@rabbitmq:5672` | Sim | URL de conexão AMQP |

### scheduler-agent

| Variável | Exemplo | Obrigatória | Descrição |
|----------|---------|-------------|-----------|
| `RABBIT_URL` | `amqp://guest:guest@rabbitmq:5672` | Sim | URL de conexão AMQP |
| `ORDER_URL` | `http://order-service:8080` | Sim | URL base do order-service |

## Ambientes

| Variável | Docker Compose | Local sem Docker |
|----------|---------------|-----------------|
| `RABBIT_URL` | `amqp://guest:guest@rabbitmq:5672` | `amqp://guest:guest@localhost:5672` |
| `ORDER_URL` | `http://order-service:8080` | `http://localhost:8080` |

## Configuração

Via Docker Compose: definidas no `environment:` de cada serviço no `docker-compose.yml`  
Via local: exportar antes de iniciar: `export RABBIT_URL=amqp://guest:guest@localhost:5672`
