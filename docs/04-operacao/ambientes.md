# Ambientes — microservice-shop

| Ambiente | Como subir | order-service | RabbitMQ |
|----------|-----------|--------------|---------|
| Docker Compose | `docker compose up -d` | http://localhost:8080 | http://localhost:15672 |
| Local (manual) | Cada serviço individualmente | http://localhost:8080 | http://localhost:15672 |

## Docker Compose (recomendado)

```bash
# Subir tudo
docker compose up -d

# Verificar saúde
curl http://localhost:8080/actuator/health

# Ver logs do scheduler
docker compose logs -f scheduler-agent

# Derrubar
docker compose down
```

## Makefile

```bash
make compose-up      # docker compose up -d
make compose-down    # docker compose down
make api-test        # mvn test no order-service
make bdd-test        # testes Cucumber
make worker-run      # worker Python local
make llm-setup       # instalar deps de ML
```

## Credenciais RabbitMQ Management

- URL: http://localhost:15672
- Usuário: `guest` | Senha: `guest`
