# Próximos passos para finalizar o projeto

Este guia detalha o que falta para considerar o Microservice Shop "pronto" para demonstrações de alto impacto ou para um piloto controlado. As iniciativas abaixo complementam o roadmap e focam no hardening do que já existe antes de abrir novas frentes.

## 1. Concluir o escopo mínimo viável
| Item | Objetivo | Responsável sugerido | Critérios de aceite |
| --- | --- | --- | --- |
| Cobertura de testes do `order-service` | Implementar unit tests para `OrderController`, `OrderService` e producers AMQP, além de um teste de integração usando Testcontainers. | Squad API | - `mvn test` com ≥70% de cobertura nas classes citadas.<br>- Build do CI verde com os novos testes. |
| Refatorar o `scheduler-agent` | Garantir idempotência, logs estruturados e métricas básicas (tempo de confirmação, erros por minuto). | Squad Workers | - Retries configuráveis por variável de ambiente.<br>- Export de métricas no padrão Prometheus ou logs JSON com as mesmas informações. |
| Testes BDD desacoplados de infraestrutura externa | Permitir que `npm test` rode em CI sem depender do RabbitMQ real, usando mocks ou containers efêmeros. | Chapter QA | - Pipeline CI consegue executar `make bdd-test` em < 5 min sem serviços extras.<br>- Documentação descreve as flags para rodar com stack real quando necessário. |
| Limpeza de dependências versionadas | Remover `tests/bdd/node_modules` do controle de versão e reforçar `.gitignore`. | DevEx | - Repositório limpo após `git clean -fdx`.<br>- CI instala dependências sempre via `npm ci`. |

## 2. Expandir funcionalidades prometidas
| Item | Objetivo | Dependências | Critérios de aceite |
| --- | --- | --- | --- |
| `catalog-service` | Expor `GET /products` com cache Redis e seed inicial. | Infra Redis via Compose. | - Endpoint documentado em OpenAPI.<br>- Testes unitários + integração com Redis (usando container). |
| `auth-service` | Fornecer emissão e validação de JWT para proteger o `order-service`. | Integração com `order-service` e BDD. | - Middleware no `order-service` exigindo token.<br>- Cenário BDD cobrindo requisições autenticadas. |
| `ai-advisor` | Consolidar notebooks em um microsserviço que gera recomendações textuais para pedidos. | Dados exportados do `order-service`. | - Endpoint `POST /advice/orders` com payload documentado.<br>- Testes de smoke + README com passos de reprodução do modelo. |

## 3. Preparar para operação contínua
| Item | Objetivo | Entregáveis |
| --- | --- | --- |
| Observabilidade | Adotar OpenTelemetry nos serviços Java e Python, expondo traces e métricas padrão. | - Configuração `otel-collector` no Compose.<br>- Dashboards básicos (Grafana ou equivalente) documentados em `docs/runbook.md`. |
| Automação de deploy | Expandir pipeline CI/CD para publicar imagens e executar smoke tests pós-deploy. | - Workflow GitHub Actions com stages `build`, `test`, `publish` e `smoke`.<br>- Checklist de rollback em `docs/runbook.md`. |
| Governança de IA/LLM | Formalizar a passagem dos notebooks para produção (dados, versões de modelo, monitoramento). | - Processo descrito em `docs/mlops-llmops.md` com critérios de promoção.<br>- Scripts `ml/llm` versionados com tags de modelo. |

## 4. Critérios de "pronto para demo/piloto"
1. **Fluxo de pedidos coberto**: cenários de criação, confirmação automática e manual possuem testes unitários, integração e BDD verdes no CI.
2. **Documentação alinhada**: README de cada serviço descreve autenticação, configurações e exemplos `curl`, e o novo escopo consta no `CHANGELOG.md`.
3. **Observabilidade mínima**: logs estruturados + métricas básicas acessíveis via Docker Compose.
4. **Segurança e dependências**: scanners de vulnerabilidade executam em cada pipeline e não há dependências instaladas manualmente no repositório.
5. **IA integrada**: `ai-advisor` publicado com exemplo prático conectado ao fluxo de pedidos.

## 5. Sequência sugerida
1. **Hardening** (testes, scheduler, BDD sem acoplamento e limpeza de dependências).
2. **Serviços adicionais** (catálogo, auth e advisor) em paralelo, priorizando integrações incrementais.
3. **Observabilidade + CI/CD** para suportar as novas peças.
4. **Go/No-Go review** usando os critérios acima antes de divulgar o projeto a recrutadores ou stakeholders.

Mantenha este documento atualizado ao fechar cada item para que o status do projeto reflita a realidade durante entrevistas e apresentações.
