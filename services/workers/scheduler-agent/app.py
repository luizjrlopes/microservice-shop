import json
import logging
import os
import time

import pika
import requests

RABBIT_URL = os.environ.get("RABBIT_URL", "amqp://guest:guest@localhost:5672")
ORDER_URL = os.environ.get("ORDER_URL", "http://localhost:8080")
REQUEST_TIMEOUT_SECONDS = float(os.environ.get("REQUEST_TIMEOUT_SECONDS", "5"))
MAX_RETRIES = int(os.environ.get("MAX_RETRIES", "3"))
RETRY_DELAY_MS = int(os.environ.get("RETRY_DELAY_MS", "5000"))

ORDER_EXCHANGE = "order.exchange"
QUEUE_NAME = "order.created"
RETRY_EXCHANGE = "order.retry.exchange"
RETRY_QUEUE = "order.created.retry"
DLQ_EXCHANGE = "order.dlx"
DLQ_NAME = "order.created.dlq"

logger = logging.getLogger("scheduler-agent")
logging.basicConfig(level=os.environ.get("LOG_LEVEL", "INFO"), format="%(message)s")


def log_event(event: str, **fields) -> None:
    payload = {"event": event, **fields}
    logger.info(json.dumps(payload, ensure_ascii=False, default=str))


def _value(mapping, key, default=None):
    if not mapping:
        return default
    return mapping.get(key, mapping.get(key.encode(), default))


def retry_count(properties) -> int:
    headers = getattr(properties, "headers", None) or {}
    deaths = _value(headers, "x-death", []) or []
    counts = []
    for death in deaths:
        queue = _value(death, "queue")
        if isinstance(queue, bytes):
            queue = queue.decode()
        if queue == QUEUE_NAME:
            counts.append(int(_value(death, "count", 0)))
    return max(counts, default=0)


def message_properties(properties, reason: str):
    headers = dict(getattr(properties, "headers", None) or {})
    headers["deadLetterReason"] = reason
    return pika.BasicProperties(
        content_type=getattr(properties, "content_type", None) or "application/json",
        delivery_mode=2,
        message_id=getattr(properties, "message_id", None),
        correlation_id=getattr(properties, "correlation_id", None),
        headers=headers,
    )


def send_to_dlq(channel, body, properties, reason: str) -> None:
    channel.basic_publish(
        exchange=DLQ_EXCHANGE,
        routing_key=DLQ_NAME,
        body=body,
        properties=message_properties(properties, reason),
    )


def declare_topology(channel) -> None:
    channel.exchange_declare(exchange=ORDER_EXCHANGE, exchange_type="topic", durable=True)
    channel.exchange_declare(exchange=RETRY_EXCHANGE, exchange_type="direct", durable=True)
    channel.exchange_declare(exchange=DLQ_EXCHANGE, exchange_type="direct", durable=True)

    channel.queue_declare(
        queue=QUEUE_NAME,
        durable=True,
        arguments={
            "x-dead-letter-exchange": RETRY_EXCHANGE,
            "x-dead-letter-routing-key": RETRY_QUEUE,
        },
    )
    channel.queue_bind(queue=QUEUE_NAME, exchange=ORDER_EXCHANGE, routing_key=QUEUE_NAME)

    channel.queue_declare(
        queue=RETRY_QUEUE,
        durable=True,
        arguments={
            "x-message-ttl": RETRY_DELAY_MS,
            "x-dead-letter-exchange": ORDER_EXCHANGE,
            "x-dead-letter-routing-key": QUEUE_NAME,
        },
    )
    channel.queue_bind(queue=RETRY_QUEUE, exchange=RETRY_EXCHANGE, routing_key=RETRY_QUEUE)

    channel.queue_declare(queue=DLQ_NAME, durable=True)
    channel.queue_bind(queue=DLQ_NAME, exchange=DLQ_EXCHANGE, routing_key=DLQ_NAME)


def on_message(channel, method, properties, body):
    started_at = time.monotonic()
    current_retry = retry_count(properties)

    try:
        data = json.loads(body)
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        send_to_dlq(channel, body, properties, "invalid_json")
        channel.basic_ack(delivery_tag=method.delivery_tag)
        log_event("message_dead_lettered", reason="invalid_json", error=str(exc))
        return

    order_id = data.get("id")
    if not order_id:
        send_to_dlq(channel, body, properties, "missing_order_id")
        channel.basic_ack(delivery_tag=method.delivery_tag)
        log_event("message_dead_lettered", reason="missing_order_id")
        return

    event_id = data.get("eventId") or getattr(properties, "message_id", None) or order_id
    correlation_id = getattr(properties, "correlation_id", None) or order_id
    url = f"{ORDER_URL}/orders/{order_id}/confirm"

    try:
        response = requests.post(
            url,
            headers={
                "Idempotency-Key": str(event_id),
                "X-Correlation-Id": str(correlation_id),
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )

        if 400 <= response.status_code < 500:
            send_to_dlq(channel, body, properties, f"http_{response.status_code}")
            channel.basic_ack(delivery_tag=method.delivery_tag)
            log_event(
                "message_dead_lettered",
                order_id=order_id,
                event_id=event_id,
                reason=f"http_{response.status_code}",
                duration_ms=round((time.monotonic() - started_at) * 1000, 2),
            )
            return

        response.raise_for_status()
        channel.basic_ack(delivery_tag=method.delivery_tag)
        log_event(
            "order_confirmation_succeeded",
            order_id=order_id,
            event_id=event_id,
            attempt=current_retry + 1,
            duration_ms=round((time.monotonic() - started_at) * 1000, 2),
        )
    except requests.RequestException as exc:
        if current_retry >= MAX_RETRIES:
            send_to_dlq(channel, body, properties, "retry_exhausted")
            channel.basic_ack(delivery_tag=method.delivery_tag)
            log_event(
                "message_dead_lettered",
                order_id=order_id,
                event_id=event_id,
                reason="retry_exhausted",
                retries=current_retry,
                error=str(exc),
            )
            return

        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        log_event(
            "order_confirmation_retry_scheduled",
            order_id=order_id,
            event_id=event_id,
            retry=current_retry + 1,
            retry_delay_ms=RETRY_DELAY_MS,
            error=str(exc),
        )


def main():
    params = pika.URLParameters(RABBIT_URL)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    declare_topology(channel)
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=on_message, auto_ack=False)
    log_event(
        "consumer_started",
        queue=QUEUE_NAME,
        max_retries=MAX_RETRIES,
        retry_delay_ms=RETRY_DELAY_MS,
        request_timeout_seconds=REQUEST_TIMEOUT_SECONDS,
    )
    channel.start_consuming()


if __name__ == "__main__":
    main()
