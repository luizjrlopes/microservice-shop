# Próximos passos

O primeiro ciclo de hardening do Microservice Shop foi incorporado ao fluxo principal. O projeto já possui testes unitários, processamento at-least-once, retry com atraso, DLQ, logs estruturados, validação de entrada e BDD real no CI.

A próxima evolução deve aumentar a profundidade do sistema existente, não apenas adicionar novos serviços.

## 1. Persistência PostgreSQL

Substituir o adapter em memória do runtime por um adapter PostgreSQL atrás de `OrderRepository`.

Critérios de aceite:

- migrations versionadas;
- pedidos sobrevivem a restart do container;
- use cases não dependem de detalhes de SQL/JPA;
- testes de integração executam com banco efêmero;
- documentação registra o trade-off da estratégia escolhida.

## 2. Testcontainers

Cobrir integrações reais sem depender de infraestrutura previamente ligada.

Prioridades:

- RabbitMQ para publicação, retry e DLQ;
- PostgreSQL para persistência;
- cenário verificando reentrega e idempotência.

## 3. OpenTelemetry

Instrumentar API, publisher, RabbitMQ e worker.

Critérios de aceite:

- um pedido possui um trace correlacionável do `POST /orders` até a confirmação;
- erros e retries aparecem associados ao mesmo contexto;
- collector executa localmente via Compose;
- runbook mostra como investigar uma execução completa.

## 4. Métricas e operação

Expor métricas que reflitam comportamento do sistema, não apenas disponibilidade de processo:

- taxa de criação de pedidos;
- tempo até confirmação;
- quantidade de retries;
- mensagens na DLQ;
- profundidade das filas;
- taxa de erro do scheduler.

## 5. Contract testing

Formalizar `order.created` como contrato versionado entre publisher e consumer e bloquear breaking changes no CI.

## 6. Infraestrutura como código

Evoluir `infra/` para um ambiente cloud reproduzível. Docker Compose continua sendo o caminho local e gratuito; a infraestrutura remota deve existir como demonstração de arquitetura e automação, não como dependência para executar o projeto.

## 7. Novos bounded contexts

Somente após persistência, observabilidade e contratos:

- catálogo;
- autenticação/autorização;
- estoque/reserva, se as regras de domínio justificarem;
- IA aplicada somente quando houver uma decisão ou análise concreta que realmente se beneficie dela.

## Critério para a próxima fase

Antes de adicionar outro microsserviço, o projeto deve conseguir responder claramente:

1. como uma mensagem é recuperada após falha;
2. como uma duplicata é tolerada;
3. como um incidente é rastreado ponta a ponta;
4. como o contrato entre serviços é protegido;
5. como o estado sobrevive a reinícios.

Essas respostas aumentam mais o valor técnico do projeto do que simplesmente aumentar a quantidade de serviços no diagrama.
