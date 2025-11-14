# Guia de Setup

Este guia consolida dependências, variáveis de ambiente e procedimentos de instalação para trabalhar com o Microservice Shop.

## Pré-requisitos globais
| Ferramenta | Versão mínima | Uso |
| --- | --- | --- |
| Docker + Docker Compose | 24.x / v2 | Orquestração local via `docker-compose.yml` (RabbitMQ, order-service, scheduler-agent). |
| Java JDK | 17 | Build do `services/api/order-service` com Maven. |
| Maven | 3.9 | Execução de testes/build Spring Boot. |
| Python | 3.11 | Execução standalone do `services/workers/scheduler-agent` e notebooks de `ml/llm`. |
| Node.js + npm | 18 / 9 | Testes BDD (`tests/bdd`). |
| Make | 4+ | Acesso unificado aos comandos definidos no `Makefile`. |

## Variáveis de ambiente
| Variável | Serviço | Descrição | Valor padrão |
| --- | --- | --- | --- |
| `RABBIT_URL` | order-service, scheduler-agent | URL de conexão AMQP usada para publicar/consumir `order.created`. | `amqp://guest:guest@rabbitmq:5672` no Compose; `amqp://guest:guest@localhost:5672` fora dele. |
| `ORDER_URL` | scheduler-agent | Endpoint base do serviço de pedidos. | `http://order-service:8080` (Compose) ou `http://localhost:8080`. |

Configure variáveis via `.env` na raiz ou exporte antes de iniciar cada processo.

## Instalação por componente
### order-service (Java)
```bash
cd services/api/order-service
mvn wrapper:wrapper   # opcional, se quiser usar mvnw
mvn clean install
```
> Equivalente: `make api-build` / `make api-test` na raiz.
O serviço expõe `POST /orders` e `POST /orders/{id}/confirm` em `http://localhost:8080` quando iniciado localmente (`mvn spring-boot:run`).

### scheduler-agent (Python)
```bash
cd services/workers/scheduler-agent
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```
> Equivalente: `make worker-install` e `make worker-run`.
O worker abre conexão em RabbitMQ, declara a fila `order.created` e confirma pedidos conforme eventos recebidos.

### Testes BDD (TypeScript)
```bash
cd tests/bdd
npm install
npm test
```
> Equivalente: `make bdd-install` e `make bdd-test`.
Os testes assumem que RabbitMQ e `order-service` estão em execução. Use `RABBIT_URL` e `ORDER_BASE_URL` definidos em `.env` da pasta, se necessário.

## Assets de dados e notebooks
- Centralize datasets em `./data/` (não versionado) para uso compartilhado.
- Os notebooks de IA/LLM devem residir em `ml/llm/notebooks/` e seguir a convenção descrita em [`docs/experiments-notebooks.md`](./experiments-notebooks.md). Instale as dependências compartilhadas com `make llm-setup`.

## Troubleshooting
1. **RabbitMQ indisponível**: reinicie via `docker compose restart rabbitmq` e confirme `rabbitmq-diagnostics check_running`.
2. **Portas ocupadas**: customize mapeamentos editando `docker-compose.yml` e atualize os clientes.
3. **Problemas de SSL**: use URLs `amqp://`/`http://` em ambientes de laboratório; configure certificados somente em ambientes produtivos.
