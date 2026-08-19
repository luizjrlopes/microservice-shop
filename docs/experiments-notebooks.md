# Experimentos de ML e LLM

Este documento descreve a trilha experimental separada do núcleo transacional do Microservice Shop.

## Estado real

### Implementado

O repositório possui dois experimentos de ML clássico com scripts executáveis de treino e inferência:

- [`../ml/experiments/demand-forecasting`](../ml/experiments/demand-forecasting) — baseline de previsão de demanda;
- [`../ml/experiments/order-anomaly-detection`](../ml/experiments/order-anomaly-detection) — baseline de detecção de anomalias.

Os experimentos usam dados de exemplo e servem como baselines técnicos. Eles não são executados pelo fluxo principal de pedidos.

### Ainda não implementado

A área [`../ml/llm`](../ml/llm) possui dependências e estrutura para experimentação, mas `ml/llm/notebooks` não contém atualmente uma feature ou notebook LLM funcional versionado.

Portanto, o projeto **não apresenta LLM como capacidade integrada** no estado atual.

## Organização

```text
ml/
├── experiments/
│   ├── demand-forecasting/
│   │   ├── data/
│   │   ├── train.py
│   │   ├── infer.py
│   │   └── README.md
│   └── order-anomaly-detection/
│       ├── data/
│       ├── train.py
│       ├── infer.py
│       └── README.md
└── llm/
    ├── notebooks/
    ├── requirements.txt
    └── README.md
```

## Princípios

1. experimento deve ser reproduzível por script sempre que possível;
2. dados sensíveis não entram no repositório;
3. métricas devem acompanhar o artefato/modelo;
4. experimento não é promovido a feature apenas porque funciona em notebook;
5. integração ao sistema exige contrato, testes, observabilidade e justificativa arquitetural.

## Demand forecasting

O baseline atual usa regressão linear com features temporais e de cesta. A avaliação inclui MAE e R² em partição de teste.

Evoluções possíveis:

- validação temporal em vez de split aleatório;
- comparação com baselines sazonais;
- tracking de experimentos;
- integração somente se houver decisão explícita na Onda 4.

## Order anomaly detection

O baseline atual usa Isolation Forest sobre atributos tabulares de pedidos.

A implementação deve continuar tratada como experimental: a avaliação atual ainda pode ser fortalecida com conjunto de validação independente, análise de recall/precision e limiar operacional.

## LLM

LLM permanece uma possibilidade futura, não uma obrigação arquitetural.

Um caso de uso só deve ser implementado quando responder claramente:

- qual decisão ou tarefa ele melhora;
- por que um modelo tradicional/regra não basta;
- quais dados/contexto são usados;
- como a saída será avaliada;
- como custo, latência, fallback e segurança serão tratados.

Até lá, não há `AI Advisor` ou copiloto no roadmap imediato.

## Relação com a evolução principal

A confiabilidade distribuída do fluxo de pedidos tem prioridade sobre IA. Depois das Ondas 1–3, a Onda 4 pode escolher entre cloud/IaC real ou integração de um experimento de ML, conforme [`../ROADMAP.md`](../ROADMAP.md).