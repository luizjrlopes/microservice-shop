"""Script de inferência para previsão de demanda."""
from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any, Dict

import joblib
import pandas as pd


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Roda previsão diária de demanda")
    parser.add_argument("--model", type=Path, required=True, help="Caminho para o arquivo .joblib gerado no treino")
    parser.add_argument("--date", required=True, help="Data alvo no formato YYYY-MM-DD")
    parser.add_argument("--product-id", required=True, help="Identificador do SKU")
    parser.add_argument("--avg-basket", type=float, required=True, help="Ticket médio estimado para o dia")
    parser.add_argument("--promo-flag", type=int, choices=[0, 1], required=True, help="Se haverá promoção (0/1)")
    return parser.parse_args()


def load_model(path: Path) -> Dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"Modelo não encontrado em {path}")
    return joblib.load(path)


def main() -> None:
    args = parse_args()
    payload = load_model(args.model)
    model = payload["model"]
    feature_order = payload["feature_order"]

    df = pd.DataFrame(
        [
            {
                "date": pd.to_datetime(args.date),
                "avg_basket_value": args.avg_basket,
                "promo_flag": args.promo_flag,
            }
        ]
    )
    df["day_of_year"] = df["date"].dt.dayofyear
    df["day_of_week"] = df["date"].dt.dayofweek
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)

    features = df[feature_order]
    prediction = float(model.predict(features)[0])

    print("Contexto")
    print(
        {
            "date": args.date,
            "product_id": args.product_id,
            "avg_basket": args.avg_basket,
            "promo_flag": args.promo_flag,
        }
    )
    print(f"Previsão de demanda: {prediction:.2f} unidades")


if __name__ == "__main__":
    main()
