"""Inferência para o modelo de anomalias em pedidos."""
from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any, Dict

import joblib
import numpy as np
import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Executa detecção de anomalias para um pedido")
    parser.add_argument("--model", type=Path, required=True, help="Arquivo .joblib salvo no treinamento")
    parser.add_argument("--order-id", required=True)
    parser.add_argument("--avg-processing-ms", type=float, required=True)
    parser.add_argument("--item-count", type=int, required=True)
    parser.add_argument("--payment-score", type=float, required=True)
    return parser.parse_args()


def load_model(path: Path) -> Dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"Modelo não encontrado: {path}")
    return joblib.load(path)


def main() -> None:
    args = parse_args()
    payload = load_model(args.model)
    model = payload["model"]
    feature_order = payload["feature_order"]

    df = pd.DataFrame(
        [
            {
                "avg_processing_ms": args.avg_processing_ms,
                "item_count": args.item_count,
                "payment_score": args.payment_score,
            }
        ]
    )
    features = df[feature_order]

    raw_prediction = model.predict(features)[0]
    anomaly_flag = int(raw_prediction == -1)
    score = model.decision_function(features)[0]
    prob = float(1 / (1 + np.exp(score)))

    print(
        {
            "order_id": args.order_id,
            "anomaly": bool(anomaly_flag),
            "score": score,
            "approx_probability": prob,
        }
    )


if __name__ == "__main__":
    main()
