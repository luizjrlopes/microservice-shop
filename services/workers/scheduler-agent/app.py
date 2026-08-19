import json
import logging
import os
import time
from typing import Any

import pika
import requests
from prometheus_client import Counter, Histogram, start_http_server

RABBIT_URL = os.environ.get("RABBIT_URL", "amqp://guest:guest@localhost:5672")
ORDER_URL = os.environ.get("ORDER_URL", "http://localhost:8080")
REQUEST_TIMEOUT_SECONDS = float(os.environ.get("REQUEST_TIMEOUT_SECONDS", "5"))
EXCHANGE_NAME = os.environ.get("ORDERS_EXCHANGE", "orders.events")
ROUTING_KEY = os.environ.get("ORDER_CREATED_ROUTING_KEY", "order.created.v1")
QUEUE_NAME = os.environ.get("SCHEDULER_QUEUE", "scheduler.order-created.v1")
RETRY_QUEUE_NAME = os.environ.get(
    "SCHEDULER_RETRY_QUEUE", "scheduler.order-created.retry.v1"
)
DLQ_NAME = os.environ.get("SCHEDULER_DLQ", "scheduler.order-created.dlq.v1")
RETRY_DELAY_MS = int(os.environ.get("SCHEDULER_RETRY_DELAY_MS", "5000"))
MAX_RETRIES = int(os.environ.get("SCHEDULER_MAX_RETRIES", "3"))
METRICS_PORT = int(os.environ.get("METRICS_PORT", "9100"))

LOGGER = logging.getLogger("scheduler-agent")
logging.basicConfig(level=os.environ.get("LOG_LEVEL", "INFO"), format="%(message)s")

MESSAGES_PROCESSED = Counter(
    "scheduler_messages_processed_total",
    "Order-created messages successfully processed.",
)
MESSAGE_RETRIES = Counter(
    "scheduler_message_retries_total",
    "Order-created messages scheduled for retry.",
)
MESSAGES_DLQ = Counter(
    "scheduler_messages_dlq_total",
    "Order-created messages sent to the dead-letter queue.",
)
CONFIRM_LATENCY = Histogram(
    "scheduler_order_confirmation_seconds",
    "Latency of the HTTP order confirmation operation.",
)


def _log(event: str, **fields: Any) -> None:
    LOGGER.info(json.dumps({"event": event, **fields}, default=str, sort_keys=True))


def configure_topology(channel) -> None:
    channel.exchange_declare(
        exchange=EXCHANGE_NAME,
        exchange_type="topic",
        durable=True,
    )
    channel.queue_declare(queue=QUEUE_NAME, durable=True)
    channel.queue_bind(
        queue=QUEUE_NAME,
        exchange=EXCHANGE_NAME,
        routing_key=ROUTING_KEY,
    )
    channel.queue_declare(
        queue=RETRY_QUEUE_NAME,
        durable=True,
        arguments={
            "x-message-ttl": RETRY_DELAY_MS,
            "x-dead-letter-exchange": EXCHANGE_NAME,
            "x-dead-letter-routing-key": ROUTING_KEY,
        },
    )
    channel.queue_declare(queue=DLQ_NAME, durable=True)


def _headers(properties) -> dict[str, Any]:
    if properties is None or properties.headers is None:
        return {}
    return dict(properties.headers)


def _retry_count(properties) -> int:
    raw = _headers(properties).get("x-retry-count", 0)
    try:
        return int(raw)
    except (TypeError, ValueError):
        return 0


def _event_context(data: dict[str, Any], properties) -> tuple[str, str, str]:
    payload = data.get("payload")
    order_id = None
    if isinstance(payload, dict):
        order_id = payload.get("orderId")

    event_id = data.get("eventId") or getattr(properties, "message_id", None)
    correlation_id = data.get("correlationId") or getattr(
        properties, "correlation_id", None
    )

    if data.get("eventType") != "order.created":
        raise ValueError("unsupported eventType")
    if data.get("eventVersion") != 1:
        raise ValueError("unsupported eventVersion")
    if not event_id:
        raise ValueError("eventId is required")
    if not order_id:
        raise ValueError("payload.orderId is required")

    return str(event_id), str(correlation_id or order_id), str(order_id)


def _publish_with_headers(channel, queue_name: str, body: bytes, properties, headers):
    channel.basic_publish(
        exchange="",
        routing_key=queue_name,
        body=body,
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
            message_id=getattr(properties, "message_id", None),
            correlation_id=getattr(properties, "correlation_id", None),
            headers=headers,
        ),
        mandatory=True,
    )


def _send_to_dlq(channel, body: bytes, properties, reason: str, retry_count: int):
    headers = _headers(properties)
    headers.update(
        {
            "x-retry-count": retry_count,
            "x-dead-letter-reason": reason[:256],
        }
    )
    _publish_with_headers(channel, DLQ_NAME, body, properties, headers)
    MESSAGES_DLQ.inc()


def _schedule_retry(channel, body: bytes, properties, reason: str, retry_count: int):
    headers = _headers(properties)
    headers.update(
        {
            "x-retry-count": retry_count,
            "x-last-error": reason[:256],
        }
    )
    _publish_with_headers(channel, RETRY_QUEUE_NAME, body, properties, headers)
    MESSAGE_RETRIES.inc()


def on_message(channel, method, properties, body):
    delivery_tag = method.delivery_tag

    try:
        data = json.loads(body)
        if not isinstance(data, dict):
            raise ValueError("event payload must be a JSON object")
        event_id, correlation_id, order_id = _event_context(data, properties)
    except (json.JSONDecodeError, UnicodeDecodeError, ValueError) as exception:
        try:
            _send_to_dlq(
                channel,
                body,
                properties,
                f"invalid_event:{exception}",
                _retry_count(properties),
            )
            channel.basic_ack(delivery_tag=delivery_tag)
            _log("message_dead_lettered", reason=str(exception))
        except Exception:
            channel.basic_nack(delivery_tag=delivery_tag, requeue=True)
            raise
        return

    started_at = time.monotonic()
    try:
        response = requests.post(
            f"{ORDER_URL}/orders/{order_id}/confirm",
            headers={
                "X-Correlation-Id": correlation_id,
                "X-Event-Id": event_id,
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        CONFIRM_LATENCY.observe(time.monotonic() - started_at)
        MESSAGES_PROCESSED.inc()
        channel.basic_ack(delivery_tag=delivery_tag)
        _log(
            "order_confirmed",
            orderId=order_id,
            eventId=event_id,
            correlationId=correlation_id,
        )
    except requests.RequestException as exception:
        retry_count = _retry_count(properties) + 1
        try:
            if retry_count <= MAX_RETRIES:
                _schedule_retry(
                    channel,
                    body,
                    properties,
                    str(exception),
                    retry_count,
                )
                _log(
                    "message_retry_scheduled",
                    orderId=order_id,
                    eventId=event_id,
                    correlationId=correlation_id,
                    retry=retry_count,
                )
            else:
                _send_to_dlq(
                    channel,
                    body,
                    properties,
                    str(exception),
                    retry_count,
                )
                _log(
                    "message_dead_lettered",
                    orderId=order_id,
                    eventId=event_id,
                    correlationId=correlation_id,
                    retry=retry_count,
                    reason=str(exception),
                )
            channel.basic_ack(delivery_tag=delivery_tag)
        except Exception:
            channel.basic_nack(delivery_tag=delivery_tag, requeue=True)
            raise


def main():
    params = pika.URLParameters(RABBIT_URL)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    configure_topology(channel)
    channel.confirm_delivery()
    channel.basic_qos(prefetch_count=10)
    start_http_server(METRICS_PORT)
    channel.basic_consume(
        queue=QUEUE_NAME,
        on_message_callback=on_message,
        auto_ack=False,
    )
    _log(
        "consumer_started",
        queue=QUEUE_NAME,
        exchange=EXCHANGE_NAME,
        routingKey=ROUTING_KEY,
        retryQueue=RETRY_QUEUE_NAME,
        dlq=DLQ_NAME,
        maxRetries=MAX_RETRIES,
    )
    channel.start_consuming()


if __name__ == "__main__":
    main()
