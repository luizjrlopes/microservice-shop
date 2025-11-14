import json
from types import SimpleNamespace
from unittest.mock import MagicMock

import app


def _method(tag: str) -> SimpleNamespace:
    return SimpleNamespace(delivery_tag=tag)


def test_on_message_posts_confirmation_request(monkeypatch):
    channel = MagicMock()
    method = _method("delivery")
    order_id = "order-1"
    body = json.dumps({"id": order_id}).encode()

    post_mock = MagicMock()
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, method, None, body)

    post_mock.assert_called_once_with(f"{app.ORDER_URL}/orders/{order_id}/confirm")
    channel.basic_ack.assert_called_once_with(delivery_tag="delivery")


def test_on_message_acknowledges_when_order_id_is_missing(monkeypatch):
    channel = MagicMock()
    method = _method("tag")
    body = b"{}"

    post_mock = MagicMock()
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, method, None, body)

    post_mock.assert_not_called()
    channel.basic_ack.assert_called_once_with(delivery_tag="tag")
