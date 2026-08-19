# Microservice Shop

**Microservice Shop** é uma plataforma de processamento de pedidos orientada a eventos, construída para explorar problemas reais de sistemas distribuídos em uma stack poliglota Java/Python.

O fluxo principal combina uma API Spring Boot, RabbitMQ e um worker Python para processar pedidos de forma assíncrona. A evolução arquitetural do projeto está concentrada em confiabilidade: consistência entre estado e evento, processamento idempotente, recuperação de falhas, contratos versionados e observabilidade.

## Estado atual

Hoje o repositório implementa:

- `order-service` em Java 17 + Spring Boot;
- criação e confirmação de pedidos via HTTP;
- publicação do evento `order.created` em RabbitMQ;
- `scheduler-agent` em Python consumindo eventos e acionando a confirmação;
- Docker Compose para executar a stack local;
- testes unitários em Java e Python;
- suíte BDD em TypeScript/Cucumber;
- ADRs e documentação arquitetural;
- experimentos executáveis de ML clássico para previsão de demanda e detecção de anomalias.

O repositório **ainda não possui** persistência PostgreSQL, Transactional Outbox, retry/DLQ completos, observabilidade distribuída ou uma feature LLM integrada. Esses itens aparecem apenas quando fazem parte do plano de evolução e não são apresentados como capacidades concluídas.

## Arquitetura atual

```mermaid
flowchart LR
    C[Client] -->|POST /orders| API[order-service\nJava + Spring Boot]
    API -->|save| MEM[(In-memory repository)]
    API -->|order.created| MQ{{RabbitMQ}}
    MQ --> W[scheduler-agent\nPython]
    W -->|POST /orders/{id}/confirm| API
```

### Stack

| Área | Tecnologia |
|---|---|
| API | Java 17, Spring Boot, Spring AMQP |
| Worker | Python 3.11, pika, requests |
| Mensageria | RabbitMQ |
| Testes E2E | TypeScript, Cucumber, Axios, amqplib |
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

O alvo aprovado para a próxima versão é:

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

A estratégia completa, incluindo matriz de gaps, arquitetura-alvo, ondas de implementação e Definition of Done, está em [`docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md`](docs/05-evolucao/PLANO_EVOLUCAO_ARQUITETURAL.md).

A auditoria factual que motivou esse plano está em [`docs/05-evolucao/AUDITORIA_ESTADO_ATUAL.md`](docs/05-evolucao/AUDITORIA_ESTADO_ATUAL.md).

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
- RabbitMQ Management: `http://localhost:15672`.

### Criar um pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":"SKU-1","quantity":2}'
```

O pedido é criado como `PENDING`, um evento é publicado no RabbitMQ e o worker tenta confirmar o pedido de forma assíncrona.

## Qualidade e testes

O `Makefile` centraliza as rotinas principais:

```bash
make lint
make test
make security
make bdd-test
```

Os testes Java e Python já existem no estado atual. A suíte BDD também existe, mas sua execução como gate distribuído completo faz parte da evolução planejada porque o desenho atual ainda possui competição pela fila do consumidor.

## ML e IA

O repositório possui duas trilhas de ML clássico executáveis:

- `ml/experiments/demand-forecasting`;
- `ml/experiments/order-anomaly-detection`.

A área `ml/llm` é experimental e ainda não representa uma feature LLM integrada ao produto. A decisão atual é não adicionar um `AI Advisor` ou outro microsserviço de IA antes de existir um caso de uso implementado e justificável.

## Documentação

As fontes principais são:

- [`docs/00-produto/`](docs/00-produto/) — visão e escopo;
- [`docs/02-arquitetura/visao-tecnica.md`](docs/02-arquitetura/visao-tecnica.md) — arquitetura atualmente implementada;
- [`docs/02-arquitetura/decisoes/`](docs/02-arquitetura/decisoes/) — ADRs;
- [`docs/04-operacao/`](docs/04-operacao/) — operação;
- [`docs/05-evolucao/`](docs/05-evolucao/) — auditoria e plano arquitetural vigente;
- [`ROADMAP.md`](ROADMAP.md) — sequência de evolução.

Documentos históricos continuam no repositório quando úteis para rastreabilidade, mas não devem ser tratados como fonte canônica do estado atual.