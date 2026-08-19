# Microservice Shop

**Microservice Shop** é uma plataforma de processamento de pedidos orientada a eventos, construída para explorar problemas reais de sistemas distribuídos em uma stack poliglota Java/Python.

O fluxo principal combina uma API Spring Boot, PostgreSQL, RabbitMQ e um worker Python para processar pedidos de forma assíncrona. A evolução arquitetural do projeto está concentrada em confiabilidade: consistência entre estado e evento, processamento idempotente, recuperação de falhas, contratos versionados e observabilidade.

## Estado atual

Hoje o repositório implementa:

- `order-service` em Java 17 + Spring Boot;
- persistência PostgreSQL com Spring Data JPA;
- migrações versionadas com Flyway;
- criação, consulta e confirmação de pedidos via HTTP;
- domínio com invariantes e `OrderStatus` explícito;
- publicação do evento legado `order.created` em RabbitMQ;
- `scheduler-agent` em Python consumindo eventos e acionando a confirmação;
- Docker Compose para PostgreSQL, RabbitMQ e serviços;
- testes unitários em Java e Python;
- teste de persistência PostgreSQL via Testcontainers;
- suíte BDD em TypeScript/Cucumber;
- ADRs e documentação arquitetural;
- experimentos executáveis de ML clássico para previsão de demanda e detecção de anomalias.

O repositório **ainda não possui** Transactional Outbox no runtime, `order.created.v1` publicado pelo serviço, retry/DLQ completos, observabilidade distribuída ou uma feature LLM integrada. Esses itens aparecem apenas quando fazem parte do plano de evolução e não são apresentados como capacidades concluídas.

## Arquitetura atual

```mermaid
flowchart LR
    C[Client] -->|POST /orders| API[order-service\nJava + Spring Boot]
    API -->|JPA| DB[(PostgreSQL)]
    API -->|order.created| MQ{{RabbitMQ}}
    MQ --> W[scheduler-agent\nPython]
    W -->|POST /orders/{id}/confirm| API
    C -->|GET /orders/{id}| API
```

### Stack

| Área | Tecnologia |
|---|---|
| API | Java 17, Spring Boot, Spring AMQP, Spring Data JPA |
| Persistência | PostgreSQL, Flyway |
| Worker | Python 3.11, pika, requests |
| Mensageria | RabbitMQ |
| Testes | JUnit, Mockito, Testcontainers, Pytest, Cucumber |
| Execução local | Docker Compose |
| ML experimental | Python, pandas, scikit-learn |
| Automação | Makefile, GitHub Actions |

## Por que este projeto existe

O projeto não tenta maximizar a quantidade de serviços. O objetivo é aprofundar um fluxo distribuído pequeno o suficiente para ser compreendido por inteiro e complexo o suficiente para discutir propriedades que importam em produção:

- o que acontece quando uma chamada HTTP falha depois de uma mensagem ser entregue;
- como evitar perda silenciosa de eventos;
- como lidar com reentrega e efeitos duplicados;
- como garantir consistência entre persistência e publicação;
- como observar um pedido atravessando processos diferentes;
- como provar essas propriedades com testes automatizados.

## Evolução arquitetural

O próximo salto estrutural é remover o dual write entre PostgreSQL e RabbitMQ:

```text
Client
  -> order-service
      -> PostgreSQL
          -> Transactional Outbox
              -> RabbitMQ
                  -> scheduler-agent
                      -> retry / DLQ
                      -> confirmação idempotente
```

A estratégia completa está em [`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

## Executando localmente

### Pré-requisitos

- Docker;
- Docker Compose v2+.

### Subir a stack

```bash
docker compose up -d --build
```

Serviços principais:

- API: `http://localhost:8080`;
- health: `http://localhost:8080/actuator/health`;
- PostgreSQL: `localhost:5432`;
- RabbitMQ Management: `http://localhost:15672`.

As credenciais locais de PostgreSQL e RabbitMQ são `microservice_shop` / `microservice_shop` e servem apenas ao ambiente Compose de desenvolvimento.

### Criar um pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"SKU-1","quantity":2}'
```

### Consultar o estado

```bash
curl http://localhost:8080/orders/<order-id>
```

O pedido é persistido como `PENDING`, um evento é publicado no RabbitMQ e o worker tenta confirmá-lo de forma assíncrona.

## Qualidade e testes

O `Makefile` centraliza as rotinas principais:

```bash
make lint
make test
make security
make bdd-test
```

A persistência também possui teste contra PostgreSQL real usando Testcontainers. A suíte BDD ainda não é o gate distribuído final porque o desenho atual do consumidor será redesenhado nas próximas PRs.

## ML e IA

O repositório possui duas trilhas de ML clássico executáveis:

- `ml/experiments/demand-forecasting`;
- `ml/experiments/order-anomaly-detection`.

A área `ml/llm` é experimental e ainda não representa uma feature LLM integrada ao produto.

## Documentação

As fontes principais são:

- [`docs/00-produto/`](docs/00-produto/) — visão e escopo;
- [`docs/architecture.md`](docs/architecture.md) — arquitetura atual;
- [`docs/02-arquitetura/visao-tecnica.md`](docs/02-arquitetura/visao-tecnica.md) — visão técnica detalhada;
- [`docs/02-arquitetura/decisoes/`](docs/02-arquitetura/decisoes/) — ADRs;
- [`docs/04-operacao/`](docs/04-operacao/) — operação;
- [`docs/05-evolucao/`](docs/05-evolucao/) — auditoria e plano arquitetural vigente;
- [`ROADMAP.md`](ROADMAP.md) — sequência de evolução.

Documentos históricos continuam no repositório quando úteis para rastreabilidade, mas não devem ser tratados como fonte canônica do estado atual.