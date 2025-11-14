# Runbook de Execução

Referência rápida para subir, observar e depurar o Microservice Shop.

## 1. Subir via Docker Compose
```bash
make compose-up
```
- **RabbitMQ**: painel em `http://localhost:15672` (user: `guest` / `guest`).
- **order-service**: healthcheck em `http://localhost:8080/actuator/health`.
- **scheduler-agent**: logs via `docker compose logs -f scheduler-agent`.

### Parar e limpar
```bash
make compose-down
```
Remove containers e volumes (fila limpa).

## 2. Execução manual (dev)
1. Suba RabbitMQ (`docker compose up rabbitmq`).
2. Em outro terminal, rode `mvn spring-boot:run` em `services/api/order-service` ou `make api-run`.
3. Inicie o worker Python `python services/workers/scheduler-agent/app.py` (ou `make worker-run`) exportando `RABBIT_URL`/`ORDER_URL`.

## 3. Fluxos suportados
| Fluxo | Passos |
| --- | --- |
| Criar pedido | `POST /orders` com `productId` e `quantity`. Verifique resposta `201` com `id`. |
| Confirmar pedido manualmente | `POST /orders/{id}/confirm`. Útil quando o scheduler está desligado. |
| Confirmar via evento | Deixe `scheduler-agent` ativo; o evento `order.created` dispara a confirmação automática. |

## 4. Observabilidade
- `order-service` já expõe `/actuator/health`. Ative métricas adicionais editando `application.properties`.
- RabbitMQ possui métricas no painel Management.
- Inclua logs estruturados (`application.yml`) quando adicionar novas features.

## 5. Testes & validação
| Comando | Onde executar | Notas |
| --- | --- | --- |
| `mvn test` | `services/api/order-service` | Configurar testes unitários/integrados ao evoluir o domínio. |
| `npm test` | `tests/bdd` | Requer stack completa em execução; cenários usam Axios e amqplib. |
| `docker compose logs -f` | raiz | Monitorar interação entre serviços durante os testes. |

## 6. Troubleshooting rápido
1. **Mensagens não consumidas**: garanta que `scheduler-agent` declarou a fila como durável antes de publicar; use painel RabbitMQ → `Queues` → `order.created`.
2. **Falha de confirmação**: confira `ORDER_URL` e se o endpoint `/orders/{id}/confirm` está acessível dentro da rede Docker.
3. **Testes BDD travados**: limpe filas antigas com `docker compose down -v` e reinicie os serviços antes dos testes.

## 7. Links úteis
- [`docs/setup.md`](./setup.md) para preparar dependências.
- [`docs/architecture.md`](./architecture.md) para entender responsabilidades dos serviços.
- [`docs/experiments-notebooks.md`](./experiments-notebooks.md) para executar notebooks de IA/LLM em paralelo aos testes.
