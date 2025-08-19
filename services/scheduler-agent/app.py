import json
import os

import pika
import requests

RABBIT_URL = os.environ.get("RABBIT_URL", "amqp://guest:guest@localhost:5672")
ORDER_URL = os.environ.get("ORDER_URL", "http://localhost:8080")
QUEUE_NAME = "order.created"


def on_message(channel, method, properties, body):
    try:
        data = json.loads(body)
        order_id = data.get("id")
        if not order_id:
            return
        requests.post(f"{ORDER_URL}/orders/{order_id}/confirm")
    finally:
        channel.basic_ack(delivery_tag=method.delivery_tag)


def main():
    params = pika.URLParameters(RABBIT_URL)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    channel.queue_declare(queue=QUEUE_NAME, durable=True)
    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=on_message)
    print(f"Listening on {QUEUE_NAME} queue...")
    channel.start_consuming()


if __name__ == "__main__":
    main()
