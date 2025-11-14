# Guia de Pipelines MLOps/LLMOps

Este documento descreve como operacionalizar os experimentos mantidos em `ml/experiments/`, cobrindo desde orquestração de treinamento até inferência contínua e integrações com a stack do Microservice Shop.

## Visão geral
```
┌──────────┐      ┌─────────────┐      ┌──────────────┐      ┌────────────┐
│ Dados    │ ---> │ Treinamento │ ---> │ Registry     │ ---> │ Inferência │
│ (APIs,   │      │ (scripts +  │      │ (artefatos + │      │ (batch/    │
│ filas)   │      │ pipelines)  │      │ metadados)   │      │ tempo real)│
└──────────┘      └─────────────┘      └──────────────┘      └────────────┘
```

## Pipeline de treinamento
1. **Ingestão**
   - Exportar amostras via APIs (`services/order-service`) ou filas RabbitMQ e armazenar CSV/Parquet em `ml/experiments/<exp>/data/`.
   - Catalogar a origem no README do experimento.
2. **Preparação**
   - Rodar notebooks em `ml/llm/notebooks` para inspeção rápida.
   - Migrar o código limpo para funções em `train.py` do respectivo experimento.
3. **Execução**
   - Utilizar os scripts de referência:
     ```bash
     python ml/experiments/demand-forecasting/train.py --data data/sample_demand.csv \
       --model-out artifacts/demand_forecasting.joblib
     python ml/experiments/order-anomaly-detection/train.py --data data/sample_order_metrics.csv
     ```
   - Em CI, configure um job que execute esses scripts e publique métricas como artefato (`mae`, `r2`, `precision`).
4. **Registro**
   - Salvar os arquivos `.joblib` em `ml/experiments/<exp>/artifacts/` localmente e enviar para o registry padrão (por exemplo, bucket S3 `mlops-models/`).
   - Anexar metadados (hash do commit, dataset, métricas) em um manifesto YAML ou no serviço escolhido.

## Pipeline de inferência
1. **Batch**
   - Agende um cron no `infra/` ou `services/scheduler-cpp` chamando `python infer.py` com o modelo mais recente.
   - Armazene previsões/alertas no banco operacional (`order_forecasts` ou `order_anomalies`).
2. **Tempo real**
   - Encapsule os scripts em funções HTTP/gRPC dentro de um microserviço (`ai-advisor-service`) carregando o `.joblib` em memória.
   - Utilize o mesmo payload documentado nos READMEs para garantir compatibilidade.

## Integrações suportadas
- **RabbitMQ**: coleta de eventos `order.created` e publicação de alertas `order.monitoring`.
- **Prometheus/Grafana**: exponha métricas de latência e acurácia via exporters anexados aos serviços de inferência.
- **Vault**: armazene chaves de provedores LLM (OpenAI, Azure) usados nas etapas de enriquecimento de features.
- **Data Lake/S3**: origem e destino de datasets e modelos (usar versionamento por `commit_sha/model-name`).

## Ciclo de vida recomendado
1. Prototipar em notebook (`ml/llm/notebooks`).
2. Migrar para `ml/experiments/<exp>/train.py` + `infer.py`.
3. Automatizar treino no CI/CD (GitHub Actions) com gatilhos em branches `feat/*` e nightly.
4. Promover modelos aprovados para produção via pipeline de deploy (Helm chart + variável `MODEL_URI`).
5. Monitorar métricas operacionais com alertas no Grafana e atualizar o README do experimento quando thresholds forem ajustados.

## LLMOps específicos
- Registrar prompts e templates em `ml/experiments/<exp>/notebooks/` ou em um módulo Python versionado.
- Validar respostas com testes unitários simples antes de liberar uma nova versão do modelo híbrido.
- Usar feature stores ou Redis para armazenar embeddings reutilizados entre inferências.

Para dúvidas adicionais, alinhe com o time de plataforma e mantenha este documento atualizado sempre que novos conectores ou ferramentas forem adicionados.
