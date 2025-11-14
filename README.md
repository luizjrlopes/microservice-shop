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
- `mvn test` em `services/order-service` (atualmente sem casos, configure ao evoluir).
- `npm install && npm test` em `tests/bdd` (exige stack rodando).
- Scripts adicionais estão documentados em [`docs/runbook.md`](docs/runbook.md).

## Documentação
- [`docs/setup.md`](docs/setup.md): dependências, variáveis e procedimentos de instalação.
- [`docs/runbook.md`](docs/runbook.md): runbooks, troubleshooting e validações.
- [`docs/architecture.md`](docs/architecture.md): visão sistêmica, diagramas e contratos.
- [`docs/experiments-notebooks.md`](docs/experiments-notebooks.md): governança dos experimentos de IA/LLM.
- [`docs/restructure-plan.md`](docs/restructure-plan.md): inventário histórico do repositório.

## Estrutura do repositório
```
microservice-shop/
├── docker-compose.yml      # Orquestra RabbitMQ + serviços
├── services/
│   ├── order-service/      # API Java 17 + Spring Boot
│   └── scheduler-agent/    # Worker Python que consome RabbitMQ
├── tests/
│   └── bdd/                # Cenários Cucumber em TypeScript
└── docs/                   # Guias, arquitetura e experimentos
```

## Próximos passos
Consulte [`ROADMAP.md`](ROADMAP.md) e [`CHANGELOG.md`](CHANGELOG.md) para entender prioridades e evolução. Contribuições são bem-vindas via PR seguindo as orientações do arquivo `AGENTS.md` na raiz.
