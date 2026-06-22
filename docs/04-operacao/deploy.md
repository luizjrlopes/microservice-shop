# Deploy — microservice-shop

> Projeto de estudo sem ambiente de produção configurado.

## Opções de hospedagem (quando necessário)

| Serviço | O que hospedar | Custo |
|---------|---------------|-------|
| Railway | order-service + RabbitMQ plugin | ~$5-10/mês |
| Render | order-service (free tier) | Gratuito com limitações |
| CloudAMQP | RabbitMQ gerenciado | Free tier disponível |
| Fly.io | scheduler-agent | Free tier disponível |

## Checklist pré-deploy

- [ ] Trocar credenciais RabbitMQ (guest/guest não é aceito em produção por padrão)
- [ ] Adicionar persistência real (ADR-002 — InMemoryRepository não é adequado para produção)
- [ ] Configurar healthchecks nos serviços de cloud
- [ ] Configurar `RABBIT_URL` e `ORDER_URL` com as URLs reais de produção
