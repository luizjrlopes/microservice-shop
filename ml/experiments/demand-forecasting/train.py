"""Script de treinamento para o baseline de previsão de demanda."""
from __future__ import annotations

import argparse
from pathlib import Path

import joblib
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["date"] = pd.to_datetime(df["date"])
    df["day_of_year"] = df["date"].dt.dayofyear
    df["day_of_week"] = df["date"].dt.dayofweek
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
    return df[["day_of_year", "day_of_week", "is_weekend", "avg_basket_value", "promo_flag"]]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Treina baseline de previsão de demanda")
    parser.add_argument("--data", type=Path, required=True, help="Caminho para o CSV de treinamento limpo")
    parser.add_argument(
        "--model-out",
        type=Path,
        default=Path("artifacts/demand_forecasting.joblib"),
        help="Destino do modelo treinado",
    )
    parser.add_argument(
        "--test-size",
        type=float,
        default=0.2,
        help="Percentual reservado para validação (0-1)",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    df = pd.read_csv(args.data)
    X = build_features(df)
    y = df["demand"]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=args.test_size, random_state=42)

    model = LinearRegression()
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)

    print(f"MAE: {mae:.2f}")
    print(f"R²: {r2:.3f}")

    args.model_out.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "model": model,
        "feature_order": ["day_of_year", "day_of_week", "is_weekend", "avg_basket_value", "promo_flag"],
        "metadata": {
            "mae": mae,
            "r2": r2,
            "rows": len(df),
        },
    }
    joblib.dump(payload, args.model_out)
    print(f"Modelo salvo em {args.model_out}")


if __name__ == "__main__":
    main()
