# Experimentos de IA/LLM

Este diretório concentra notebooks, demos e scripts executáveis que exploram casos de uso de IA do Microservice Shop. Cada experimento possui o seu próprio subdiretório com:

- `README.md` com objetivo, datasets e checklist de validação.
- `data/` para amostras públicas (dados sensíveis devem permanecer fora do repositório).
- `notebooks/` para Jupyter notebooks limpos.
- Scripts `train.py` e `infer.py` para reproduzir resultados de forma automatizada.
- `artifacts/` para modelos serializados e saídas intermediárias.

## Experimentos atuais

| Diretório | Objetivo | Status |
|-----------|----------|--------|
| [`demand-forecasting`](./demand-forecasting) | Previsão diária de demanda combinando sinais tabulares com features sazonais. | Base de referência com scripts de treino e inferência. |
| [`order-anomaly-detection`](./order-anomaly-detection) | Identificação de pedidos com comportamento anômalo antes da confirmação. | Base de referência com scripts de treino e inferência. |

## Como executar um experimento

```bash
cd ml/experiments/<experimento>
python train.py --data data/<arquivo>.csv
python infer.py --model artifacts/<modelo.joblib> <args-específicos>
```

> **Dica:** mantenha os modelos exportados em `artifacts/` e publique métricas/resumos no README correspondente para facilitar auditorias.
