# Guia de Notebooks e Experimentos de IA/LLM

Este guia organiza como conduzir experimentos de IA e modelos de linguagem em torno do Microservice Shop.

## Estrutura recomendada
```
ml/
├── llm/                 # Ambiente de notebooks ad-hoc
│   ├── notebooks/
│   └── requirements.txt
└── experiments/
    ├── demand-forecasting/
    │   ├── data/
    │   ├── notebooks/
    │   ├── train.py / infer.py
    │   └── README.md
    └── order-anomaly-detection/
        └── ...
```
- Notebooks exploratórios permanecem em `ml/llm/notebooks`.
- Assim que um experimento amadurecer, mova notebooks limpos e scripts para `ml/experiments/<nome>/` e inclua um `README.md` com contexto, datasets e próximos passos.
- Novos experimentos devem seguir o template de `ml/experiments/README.md`.

## Fontes de dados
1. **Eventos RabbitMQ**: utilize `docker compose logs order-service` ou conecte-se diretamente à fila `order.created` para exportar amostras.
2. **APIs REST**: gere datasets via scripts que chamam `POST /orders` com dados sintéticos.
3. **Arquivos locais**: armazene CSV/Parquet em `data/` (não versionado) e documente a origem.

## Fluxo sugerido para notebooks
1. **Coleta & limpeza** – normalize colunas (`productId`, `quantity`, `status`, `timestamp`).
2. **Feature engineering** – crie embeddings de descrições de produtos usando modelos como `text-embedding-3-large` (ou equivalentes self-hosted).
3. **Modelagem** – explore duas frentes:
   - *Forecasting/Anomalias*: modelos clássicos + prompts que consultam o histórico para justificar alertas.
   - *Copiloto operacional*: LLM gera comandos `curl` ou consultas `kubectl` para operadores, usando contexto do runbook.
4. **Avaliação** – capture métricas (MAE, precisão) e inclua checklist qualitativo dos prompts.
5. **Produtização** – proponha rotas claras para mover o experimento para um serviço dedicado (por exemplo, `ai-advisor-service`).

## Integração com o monorepo
- Armazene dependências em `ml/llm/requirements.txt` e utilize `pip install -r` dentro de um ambiente virtual.
- Versione apenas notebooks limpos (`jupyter nbconvert --ClearOutputPreprocessor.enabled=True`).
- Cada experimento deve conter scripts `train.py`/`infer.py` reproduzíveis e documentar o processo em seu README.
- Referencie resultados relevantes em [`CHANGELOG.md`](../CHANGELOG.md) quando um experimento virar feature.

## Boas práticas de prompt engineering
- Documente cada prompt como código em células separadas.
- Inclua testes unitários para helpers críticos (por exemplo, validação de esquema de resposta JSON).
- Registre riscos de segurança (PII, vazamento de chaves) em cada notebook.

## Próximos experimentos
- **Previsão de demanda**: treinar modelo híbrido (estatístico + LLM) para sugerir estoques – baseline disponível em `ml/experiments/demand-forecasting`.
- **Detecção de fraude**: usar embeddings de histórico para detectar padrões anômalos antes da confirmação – baseada em `ml/experiments/order-anomaly-detection`.
- **Geração de playbooks**: sintetizar runbooks específicos por incidente, alimentados pelo conteúdo de [`docs/runbook.md`](./runbook.md).
