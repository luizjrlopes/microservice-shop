# Detecção de Anomalias em Pedidos

Este experimento monitora métricas operacionais do fluxo de pedidos para sinalizar eventos suspeitos antes da confirmação. A abordagem usa Isolation Forest como baseline e pode ser substituída por embeddings LLM ou classificadores híbridos conforme detalhado nos pipelines de [`docs/mlops-llmops.md`](../../../docs/mlops-llmops.md).

## Estrutura
```
ml/experiments/order-anomaly-detection/
├── data/
├── notebooks/
├── train.py
├── infer.py
└── artifacts/
```

## Dataset de exemplo
- `data/sample_order_metrics.csv` – extraído de logs sintéticos, contendo:
  - `order_id`
  - `avg_processing_ms`
  - `item_count`
  - `payment_score` (0-1)
  - `label` (0 = normal, 1 = anômalo) usado para validação supervisionada.

## Como treinar
```bash
cd ml/experiments/order-anomaly-detection
python train.py --data data/sample_order_metrics.csv --model-out artifacts/order_anomaly.joblib
```
Parâmetros relevantes:
- `--contamination`: fração estimada de anomalias (default 0.1).
- `--random-state`: semente para reprodutibilidade.

O script imprime métricas simples (precisão para o rótulo 1) e salva o modelo + threshold.

## Como executar inferência
```bash
python infer.py --model artifacts/order_anomaly.joblib \
  --order-id ORDER-123 \
  --avg-processing-ms 1800 \
  --item-count 7 \
  --payment-score 0.32
```
O output mostra a pontuação de anomalia (-1/1) e a probabilidade aproximada baseada no threshold definido em treinamento.

## Próximos passos sugeridos
1. Enviar as inferências para o tópico `order.monitoring` com o score e o payload original.
2. Usar embeddings de histórico de cliente como features adicionais.
3. Encaixar o modelo em um pipeline CI/CD automatizado descrito na documentação de pipelines.
