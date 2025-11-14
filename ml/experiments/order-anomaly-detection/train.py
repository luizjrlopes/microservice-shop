"""Treina modelo baseline para detecção de anomalias em pedidos."""
from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.metrics import precision_score


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Treina Isolation Forest para pedidos")
    parser.add_argument("--data", type=Path, required=True, help="CSV com métricas de pedidos")
    parser.add_argument(
        "--model-out",
        type=Path,
        default=Path("artifacts/order_anomaly.joblib"),
        help="Destino do modelo serializado",
    )
    parser.add_argument("--contamination", type=float, default=0.1, help="Taxa esperada de anomalias")
    parser.add_argument("--random-state", type=int, default=42, help="Semente para reprodutibilidade")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    df = pd.read_csv(args.data)
    features = df[["avg_processing_ms", "item_count", "payment_score"]]

    model = IsolationForest(
        contamination=args.contamination,
        random_state=args.random_state,
        n_estimators=200,
    )
    model.fit(features)

    # Converte saída (-1 normal, 1 anômalo) para comparar com labels 0/1
    predictions = model.predict(features)
    predictions = (predictions == -1).astype(int)
    precision = precision_score(df["label"], predictions, zero_division=0)

    print(f"Precisão (classe anômala): {precision:.2f}")

    args.model_out.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(
        {
            "model": model,
            "feature_order": ["avg_processing_ms", "item_count", "payment_score"],
            "precision": precision,
            "contamination": args.contamination,
        },
        args.model_out,
    )
    print(f"Modelo salvo em {args.model_out}")


if __name__ == "__main__":
    main()
