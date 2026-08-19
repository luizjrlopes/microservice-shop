import json
from types import SimpleNamespace
from unittest.mock import MagicMock

import requests

import app


def _method(tag: str) -> SimpleNamespace:
    return SimpleNamespace(delivery_tag=tag)


def _properties(retry_count=0):
    return SimpleNamespace(
        headers={"x-retry-count": retry_count},
        message_id="event-1",
        correlation_id="corr-1",
    )


def _event(order_id="order-1"):
    return json.dumps(
        {
            "eventId": "event-1",
            "eventType": "order.created",
            "eventVersion": 1,
            "correlationId": "corr-1",
            "payload": {
                "orderId": order_id,
                "productId": "SKU-1",
                "quantity": 2,
                "status": "PENDING",
            },
        }
    ).encode()


def test_configure_topology_declares_consumer_owned_queues():
    channel = MagicMock()

    app.configure_topology(channel)

    channel.exchange_declare.assert_called_once_with(
        exchange=app.EXCHANGE_NAME,
        exchange_type="topic",
        durable=True,
    )
    channel.queue_bind.assert_called_once_with(
        queue=app.QUEUE_NAME,
        exchange=app.EXCHANGE_NAME,
        routing_key=app.ROUTING_KEY,
    )
    assert channel.queue_declare.call_count == 3


def test_on_message_acks_only_after_successful_confirmation(monkeypatch):
    channel = MagicMock()
    response = MagicMock()
    post_mock = MagicMock(return_value=response)
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, _method("delivery"), _properties(), _event())

    post_mock.assert_called_once_with(
        f"{app.ORDER_URL}/orders/order-1/confirm",
        headers={
            "X-Correlation-Id": "corr-1",
            "X-Event-Id": "event-1",
        },
        timeout=app.REQUEST_TIMEOUT_SECONDS,
    )
    response.raise_for_status.assert_called_once_with()
    channel.basic_ack.assert_called_once_with(delivery_tag="delivery")
    channel.basic_nack.assert_not_called()


def test_transient_failure_schedules_retry_before_ack(monkeypatch):
    channel = MagicMock()
    post_mock = MagicMock(side_effect=requests.Timeout("timeout"))
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, _method("retry"), _properties(), _event())

    channel.basic_publish.assert_called_once()
    publish_call = channel.basic_publish.call_args.kwargs
    assert publish_call["routing_key"] == app.RETRY_QUEUE_NAME
    assert publish_call["properties"].headers["x-retry-count"] == 1
    channel.basic_ack.assert_called_once_with(delivery_tag="retry")
    channel.basic_nack.assert_not_called()


def test_exhausted_retries_send_message_to_dlq(monkeypatch):
    channel = MagicMock()
    post_mock = MagicMock(side_effect=requests.ConnectionError("offline"))
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(
        channel,
        _method("dlq"),
        _properties(app.MAX_RETRIES),
        _event(),
    )

    channel.basic_publish.assert_called_once()
    publish_call = channel.basic_publish.call_args.kwargs
    assert publish_call["routing_key"] == app.DLQ_NAME
    assert publish_call["properties"].headers["x-retry-count"] == app.MAX_RETRIES + 1
    channel.basic_ack.assert_called_once_with(delivery_tag="dlq")


def test_invalid_event_goes_directly_to_dlq(monkeypatch):
    channel = MagicMock()
    post_mock = MagicMock()
    monkeypatch.setattr("app.requests.post", post_mock)

    app.on_message(channel, _method("invalid"), _properties(), b"{}")

    post_mock.assert_not_called()
    channel.basic_publish.assert_called_once()
    assert channel.basic_publish.call_args.kwargs["routing_key"] == app.DLQ_NAME
    channel.basic_ack.assert_called_once_with(delivery_tag="invalid")


def test_publish_failure_requeues_original_message(monkeypatch):
    channel = MagicMock()
    channel.basic_publish.side_effect = RuntimeError("rabbit publish failed")
    post_mock = MagicMock(side_effect=requests.Timeout("timeout"))
    monkeypatch.setattr("app.requests.post", post_mock)

    try:
        app.on_message(channel, _method("requeue"), _properties(), _event())
    except RuntimeError:
        pass

    channel.basic_ack.assert_not_called()
    channel.basic_nack.assert_called_once_with(
        delivery_tag="requeue",
        requeue=True,
    )
