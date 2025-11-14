# Microservice Shop

Microservice Shop é uma plataforma de pedidos pensada para demonstrar padrões de mensageria e automação com serviços independentes. O monorepo inclui um serviço Java para gerenciamento de pedidos, um worker Python que confirma pedidos de forma assíncrona, além de testes BDD em TypeScript. A stack padrão utiliza Docker Compose para expor HTTP, AMQP e ferramentas de observabilidade prontas para laboratório.

## Visão geral
- **Orquestração**: `docker-compose.yml` provisiona RabbitMQ, `order-service` (Spring Boot) e `scheduler-agent` (Python).
- **Fluxo de pedidos**: clientes criam pedidos via `POST /orders`. O serviço publica um evento `order.created` no RabbitMQ, consumido pelo scheduler para confirmar o pedido.
- **Testes end-to-end**: `tests/bdd` contém cenários Cucumber simulando a criação de pedidos e verificando mensagens.

## Destaques de IA/LLM
Mesmo que os serviços de produção sejam tradicionais, o repositório mantém trilhas de experimentação com IA e LLMs:
- **Notebooks de experimentos** documentados em [`docs/experiments-notebooks.md`](docs/experiments-notebooks.md) descrevem como usar embeddings e LLMs para analisar dados de pedidos e gerar respostas automatizadas.
- **Roadmap** inclui iniciativas de copiloto operacional e de previsão baseada em modelos generativos para o pipeline de pedidos. Veja [`ROADMAP.md`](ROADMAP.md).
- **Guides de integração** recomendam como transformar os experimentos em features observáveis, preservando limites claros entre microsserviços e camadas de IA.

## Como usar
### Pré-requisitos
- Docker e Docker Compose (v2+).
- Acesso a portas `8080`, `5672` e `15672`.
- Opcional para desenvolvimento local sem containers: Java 17, Maven 3.9+, Python 3.11 e Node 18.

### Subindo toda a stack
```bash
docker compose up -d
```
1. Aguarde o healthcheck de RabbitMQ (`http://localhost:15672`).
2. Verifique o serviço de pedidos: `curl http://localhost:8080/actuator/health`.
3. Monitore os logs do scheduler: `docker compose logs -f scheduler-agent`.

### Criando e confirmando pedidos
```bash
# Cria um pedido
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"SKU-1","quantity":2}'

# Após o evento ser consumido, o scheduler chama automaticamente
# POST /orders/{id}/confirm. Use os logs do order-service
# para verificar a transição de status.
```

### Executando testes
- `make api-test` roda `mvn test` em `services/api/order-service` (ainda sem casos, configure ao evoluir).
- `make bdd-test` instala dependências e executa `npm test` em `tests/bdd` (exige stack rodando).
- Consulte [`docs/runbook.md`](docs/runbook.md) para tarefas adicionais com o `Makefile`.

## Documentação
- [`docs/setup.md`](docs/setup.md): dependências, variáveis e procedimentos de instalação.
- [`docs/runbook.md`](docs/runbook.md): runbooks, troubleshooting e validações.
- [`docs/architecture.md`](docs/architecture.md): visão sistêmica, diagramas e contratos.
- [`docs/experiments-notebooks.md`](docs/experiments-notebooks.md): governança dos experimentos de IA/LLM.
- [`docs/restructure-plan.md`](docs/restructure-plan.md): inventário histórico do repositório.
- [`docs/entregas-estagio.md`](docs/entregas-estagio.md): destaque das entregas do estágio, seus impactos e relação com requisitos de LLMs, MLOps e documentação.
- [`docs/proximos-passos.md`](docs/proximos-passos.md): guia consolidado do que falta para considerar o projeto pronto para demos ou piloto.

## Estrutura do repositório
```
microservice-shop/
├── docker-compose.yml          # Orquestra RabbitMQ + serviços
├── services/
│   ├── api/order-service/      # API Java 17 + Spring Boot
│   └── workers/scheduler-agent/ # Worker Python que consome RabbitMQ
├── ml/llm/                     # Notebooks e libs de experimentação IA/LLM
├── infra/terraform/            # Ponto de partida para provisionamento IaC
├── tests/
│   └── bdd/                    # Cenários Cucumber em TypeScript
└── docs/                       # Guias, arquitetura e experimentos
```

### Scripts padronizados
Um `Makefile` centraliza as rotinas mais comuns:

| Comando | Descrição |
| --- | --- |
| `make compose-up` / `make compose-down` | Sobe/derruba a stack completa com Docker Compose. |
| `make api-test` | Executa `mvn test` no `services/api/order-service`. |
| `make worker-run` | Exporta variáveis (`RABBIT_URL`, `ORDER_URL`) e sobe o worker Python localmente. |
| `make bdd-test` | Instala dependências e executa os testes BDD. |
| `make llm-setup` | Instala as dependências listadas em `ml/llm/requirements.txt`. |

Use `make help` para visualizar todas as metas disponíveis e parâmetros adicionais.

## Próximos passos
Consulte [`ROADMAP.md`](ROADMAP.md), [`CHANGELOG.md`](CHANGELOG.md) e o guia de [próximos passos](docs/proximos-passos.md) para entender prioridades, evolução e o que falta para finalizar o projeto. Contribuições são bem-vindas via PR seguindo as orientações do arquivo `AGENTS.md` na raiz.
