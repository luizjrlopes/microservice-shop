# Trilhas de IA/LLM

Este diretório concentra notebooks e utilitários relacionados a experimentos de IA descritos em [`docs/experiments-notebooks.md`](../../docs/experiments-notebooks.md). Utilize-o como ponto único para dependências, datasets locais (não versionados) e resultados intermediários.

## Estrutura
```
ml/llm/
├── notebooks/        # Coloque notebooks numerados e limpos
├── requirements.txt  # Dependências compartilhadas
└── README.md         # Este guia
```

## Como usar
1. Crie (ou ative) um ambiente virtual Python 3.11.
2. Instale as dependências: `make llm-setup`.
3. Abra o ambiente interativo: `make llm-notebook` (usa Jupyter Lab apontando para `ml/llm/notebooks`).
4. Siga as diretrizes de versionamento descritas na documentação oficial.

> **Dica:** mantenha dados brutos em `ml/llm/data/` (adicione ao `.gitignore`) para evitar commits acidentais.
