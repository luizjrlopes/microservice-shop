# Entregas do estágio: principais impactos e artefatos

Esta visão resume as entregas consolidadas no Microservice Shop e como elas respondem aos requisitos do estágio focados em **LLMs**, **MLOps** e **documentação**. Use esta lista para reportar progresso e localizar rapidamente os materiais de apoio (vídeos, demos ou links externos quando existirem).

## Resumo das entregas

| Entrega | Impacto principal | Artefatos disponíveis | Requisito do estágio |
| --- | --- | --- | --- |
| Trilhas de IA/LLM aplicadas ao fluxo de pedidos | Mantêm experimentos prontos para gerar copilotos e previsões a partir de dados reais de pedidos. | Documentação dedicada em [`docs/experiments-notebooks.md`](./experiments-notebooks.md) e bibliotecas em [`ml/llm/`](../ml/llm/). Sem vídeo/HuggingFace publicados ainda; notebooks podem ser executados localmente via `make llm-setup`. | **LLMs** |
| Pipelines e governança MLOps/LLMOps | Padronizam ingestão, treino e deploy das trilhas de IA, garantindo versionamento e segurança para chaves LLM. | Guia completo em [`docs/mlops-llmops.md`](./mlops-llmops.md), além dos exemplos em [`ml/experiments/`](../ml/experiments/) e dos alvos do [`Makefile`](../Makefile). Demos adicionais podem ser reproduzidas com `docker compose up -d`. | **MLOps** |
| Documentação operacional e runbooks unificados | Facilita onboarding, execução e troubleshooting da stack de microsserviços e dos experimentos de IA. | [`README.md`](../README.md), [`docs/setup.md`](./setup.md), [`docs/runbook.md`](./runbook.md) e [`docs/architecture.md`](./architecture.md). Ainda sem vídeo oficial; os exemplos de uso são executáveis localmente. | **Documentação** |

## Detalhes por entrega

### 1. Trilhas de IA/LLM aplicadas ao fluxo de pedidos
- **Impacto:** garante que os times possam evoluir dos experimentos descritos em [`docs/experiments-notebooks.md`](./experiments-notebooks.md) para copilotos operacionais e previsões descritas no [`ROADMAP.md`](../ROADMAP.md).
- **Artefatos:** notebooks, requisitos e utilitários residem em [`ml/llm/`](../ml/llm/) com instruções adicionais no [`ml/llm/README.md`](../ml/llm/README.md). Executar `make llm-setup` prepara o ambiente local; ainda não há publicação em HuggingFace ou vídeos demonstrativos.
- **Conexão com o requisito:** cumpre o pilar de **LLMs** ao oferecer baseline de embeddings, previsão e geração de respostas para os dados de pedidos.

### 2. Pipelines e governança MLOps/LLMOps
- **Impacto:** o guia [`docs/mlops-llmops.md`](./mlops-llmops.md) descreve como versionar datasets, armazenar segredos (Vault) e orquestrar deploys híbridos, reduzindo risco operacional.
- **Artefatos:** exemplos práticos vivem em [`ml/experiments/demand-forecasting`](../ml/experiments/demand-forecasting/) e [`ml/experiments/order-anomaly-detection`](../ml/experiments/order-anomaly-detection/), enquanto os alvos `make llm-setup` e `make worker-run` demonstram pipelines locais. As demos podem ser reproduzidas com `docker compose up -d`; não há vídeo/hospedagem HuggingFace pública por enquanto.
- **Conexão com o requisito:** cobre **MLOps**, pois detalha desde preparo de ambiente até execução controlada das trilhas LLM.

### 3. Documentação operacional e runbooks unificados
- **Impacto:** os guias [`docs/setup.md`](./setup.md), [`docs/runbook.md`](./runbook.md) e [`docs/architecture.md`](./architecture.md) eliminam lacunas de execução, descrevendo dependências, fluxos AMQP e observabilidade. Isso acelera onboarding e reduz erros em produção.
- **Artefatos:** o [`README.md`](../README.md) centraliza fluxos e comandos (incluindo `docker compose up -d` como demo local). Ainda não há vídeos oficiais, mas os comandos fornecem o passo a passo completo para reproduzir os cenários.
- **Conexão com o requisito:** satisfaz **documentação**, pois consolida tutoriais, runbooks e arquitetura numa fonte única.

> **Próximos passos:** ao publicar vídeos, demos hospedadas ou modelos em HuggingFace, atualize a coluna "Artefatos disponíveis" para manter o histórico completo das entregas.
