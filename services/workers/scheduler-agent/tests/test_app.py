import json
from types import SimpleNamespace
from unittest.mock import MagicMock

import requests

import app


def _method(tag: str) -> SimpleNamespace:
    return SimpleNamespace(delivery_tag=tag)


def _properties(headers=None) -> SimpleNamespace:
    return SimpleNamespace(
        headers=headers or {},
        message_id="event-1",
        correlation_id="order-1",
        content_type="application/json",
    )


def test_on_message_posts_confirmation_request_and_acknowledges(monkeypatch):
    channel = MagicMock()
    method = _method("delivery")
    body = json.dumps({"id": "order-1", "eventId": "event-1"}).encode()
    response = MagicMock(status_code=200)
    post_mock = MagicMock(return_value=response)
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, method, _properties(), body)

    post_mock.assert_called_once_with(
        f"{app.ORDER_URL}/orders/order-1/confirm",
        headers={
            "Idempotency-Key": "event-1",
            "X-Correlation-Id": "order-1",
        },
        timeout=app.REQUEST_TIMEOUT_SECONDS,
    )
    response.raise_for_status.assert_called_once_with()
    channel.basic_ack.assert_called_once_with(delivery_tag="delivery")
    channel.basic_nack.assert_not_called()


def test_on_message_sends_missing_order_id_to_dlq(monkeypatch):
    channel = MagicMock()
    method = _method("tag")
    properties = _properties()
    body = b"{}"
    post_mock = MagicMock()
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, method, properties, body)

    post_mock.assert_not_called()
    channel.basic_publish.assert_called_once()
    publish = channel.basic_publish.call_args.kwargs
    assert publish["exchange"] == app.DLQ_EXCHANGE
    assert publish["routing_key"] == app.DLQ_NAME
    channel.basic_ack.assert_called_once_with(delivery_tag="tag")


def test_on_message_retries_transient_http_failure(monkeypatch):
    channel = MagicMock()
    method = _method("tag")
    properties = _properties()
    body = json.dumps({"id": "order-1"}).encode()
    monkeypatch.setattr(
        "app.requests.post", MagicMock(side_effect=requests.Timeout("timed out"))
    )

    app.on_message(channel, method, properties, body)

    channel.basic_nack.assert_called_once_with(delivery_tag="tag", requeue=False)
    channel.basic_ack.assert_not_called()
    channel.basic_publish.assert_not_called()


def test_on_message_dead_letters_after_retry_budget(monkeypatch):
    channel = MagicMock()
    method = _method("tag")
    properties = _properties(
        {"x-death": [{"queue": app.QUEUE_NAME, "count": app.MAX_RETRIES}]}
    )
    body = json.dumps({"id": "order-1"}).encode()
    monkeypatch.setattr(
        "app.requests.post", MagicMock(side_effect=requests.Timeout("timed out"))
    )

    app.on_message(channel, method, properties, body)

    channel.basic_publish.assert_called_once()
    publish = channel.basic_publish.call_args.kwargs
    assert publish["exchange"] == app.DLQ_EXCHANGE
    assert publish["routing_key"] == app.DLQ_NAME
    channel.basic_ack.assert_called_once_with(delivery_tag="tag")
    channel.basic_nack.assert_not_called()


def test_on_message_dead_letters_non_retryable_http_error(monkeypatch):
    channel = MagicMock()
    method = _method("tag")
    properties = _properties()
    body = json.dumps({"id": "order-1"}).encode()
    response = MagicMock(status_code=400)
    monkeypatch.setattr("app.requests.post", MagicMock(return_value=response))

    app.on_message(channel, method, properties, body)

    channel.basic_publish.assert_called_once()
    channel.basic_ack.assert_called_once_with(delivery_tag="tag")
    channel.basic_nack.assert_not_called()
