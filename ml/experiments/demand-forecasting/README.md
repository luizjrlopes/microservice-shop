# Previsão de Demanda Orientada por Eventos

Este experimento constrói uma baseline simples para prever o volume diário de pedidos a partir de sinais agregados (sazonalidade, promoções e ticket médio). Ele serve como referência para conectar notebooks exploratórios, scripts de produção e pipelines de MLOps descritos em [`docs/mlops-llmops.md`](../../../docs/mlops-llmops.md).

## Estrutura
```
ml/experiments/demand-forecasting/
├── data/                  # Arquivos CSV públicos usados como exemplo
├── notebooks/             # Exploração interativa (não incluído por padrão)
├── train.py               # Script de treinamento
├── infer.py               # Script de inferência
└── artifacts/             # Modelos exportados (.joblib)
```

## Dataset de exemplo
- `data/sample_demand.csv` – contém 30 dias de histórico sintético com as colunas:
  - `date` (YYYY-MM-DD)
  - `product_id`
  - `avg_basket_value` (ticket médio em USD)
  - `promo_flag` (0 ou 1)
  - `demand` (quantidade vendida)

> Substitua o CSV por dados reais exportados do data lake e atualize este README com o procedimento de coleta.

## Como treinar
```bash
cd ml/experiments/demand-forecasting
python train.py --data data/sample_demand.csv --model-out artifacts/demand_forecasting.joblib
```
Parâmetros suportados:
- `--data`: caminho para o CSV limpo.
- `--model-out`: destino do modelo salvo.
- `--test-size`: percentual (0-1) reservado para validação (default: 0.2).

O script imprime MAE e R² no conjunto de teste e salva o modelo em `artifacts/`.

## Como executar inferência
```bash
python infer.py --model artifacts/demand_forecasting.joblib \
  --date 2024-08-15 \
  --product-id SKU-001 \
  --avg-basket 87.5 \
  --promo-flag 1
```
A saída inclui a previsão numérica e o payload usado (útil para logs/tracking).

## Próximos passos sugeridos
1. Substituir a regressão linear por modelos de séries temporais/LLMs especializados.
2. Integrar o script no `order-service` através de um endpoint `/forecasts/daily`.
3. Registrar datasets e modelos no registry descrito na documentação de pipelines.
