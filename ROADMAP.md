# Roadmap — Microservice Shop

O roadmap prioriza profundidade arquitetural antes de aumentar a quantidade de microsserviços. A regra é simples: um novo bounded context só entra quando o fluxo existente possui contratos, resiliência, testes e observabilidade suficientes para justificar a distribuição.

## Estado atual — Event-driven core

Concluído:

- `order-service` em Java/Spring Boot com separação por camadas;
- `scheduler-agent` em Python consumindo eventos AMQP;
- RabbitMQ como backbone assíncrono;
- evento `order.created` com identidade, timestamp e correlação;
- confirmação idempotente;
- retry com atraso e orçamento configurável;
- Dead Letter Queue;
- logs estruturados do worker;
- validação de entrada HTTP;
- testes unitários Java e Python;
- BDD TypeScript observando HTTP + RabbitMQ + confirmação assíncrona;
- CI com quality gate, testes, segurança e stack real em Docker Compose.

## Próxima etapa — Persistência e observabilidade

### 1. PostgreSQL como adapter do `OrderRepository`

Objetivo: substituir o adapter em memória no runtime sem alterar os casos de uso.

Critérios:

- migrations versionadas;
- estado preservado entre reinícios;
- testes de integração com banco efêmero;
- `InMemoryOrderRepository` mantido apenas quando útil para testes rápidos.

### 2. OpenTelemetry

Objetivo: correlacionar criação HTTP, publicação AMQP, consumo e confirmação.

Critérios:

- trace/correlation ID visível ponta a ponta;
- instrumentação do Spring Boot e do worker Python;
- collector local no Compose;
- documentação de um trace completo no runbook.

### 3. Métricas operacionais

Objetivo: tornar saúde e backlog mensuráveis.

Métricas prioritárias:

- pedidos criados e confirmados;
- latência de confirmação;
- retries;
- mensagens na DLQ;
- profundidade das filas;
- taxa de falha do worker.

## Etapa seguinte — Infraestrutura e contratos

### 4. Testcontainers

Adicionar testes de integração para RabbitMQ e PostgreSQL sem depender de serviços externos permanentes.

### 5. Contract testing

Formalizar o contrato de `order.created` e detectar breaking changes entre publisher e consumer.

### 6. Infraestrutura como código

Transformar `infra/` em uma implementação reproduzível para um ambiente cloud controlado, mantendo Docker Compose como caminho local de custo zero.

## Expansão de domínio — somente após o core

Candidatos:

- `catalog-service` para catálogo e disponibilidade;
- autenticação/autorização para APIs externas;
- inventory/reservation apenas se houver necessidade de consistência entre contextos;
- um componente de IA somente se existir um problema claro em que recomendação, classificação ou análise realmente agregue valor.

Adicionar serviços apenas para aumentar a contagem de caixas no diagrama não é objetivo deste projeto.

## Critério de maturidade

O projeto é considerado pronto para uma nova expansão quando:

1. o fluxo atual é reproduzível localmente;
2. CI executa testes unitários e ponta a ponta;
3. falhas de consumidor não perdem eventos;
4. mensagens irrecuperáveis são inspecionáveis em DLQ;
5. contratos e trade-offs estão documentados;
6. o próximo serviço resolve uma responsabilidade de domínio real, não apenas uma meta de portfólio.
