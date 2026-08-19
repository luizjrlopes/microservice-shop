import { After, Given, Then, When } from '@cucumber/cucumber';
import axios from 'axios';
import * as amqp from 'amqplib';
import assert from 'assert';

const ORDER_URL = process.env.ORDER_URL || 'http://localhost:8080';
const RABBIT_URL =
  process.env.RABBIT_URL ||
  'amqp://microservice_shop:microservice_shop@localhost:5672';
const EXCHANGE = 'orders.events';
const ROUTING_KEY = 'order.created.v1';
const AUDIT_QUEUE = 'bdd.order-created.audit.v1';

let connection: any;
let lastOrderId: string;
let lastResponse: any;

async function waitForStatus(
  orderId: string,
  expectedStatus: string,
  timeoutMs = 15000,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let lastStatus = '';

  while (Date.now() < deadline) {
    const response = await axios.get(`${ORDER_URL}/orders/${orderId}`);
    lastStatus = response.data.status;
    if (lastStatus === expectedStatus) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 300));
  }

  assert.fail(
    `Order ${orderId} did not reach ${expectedStatus}; last status=${lastStatus}`,
  );
}

Given('the distributed order stack is running', async function () {
  connection = await amqp.connect(RABBIT_URL);
  const channel = await connection.createChannel();
  await channel.assertExchange(EXCHANGE, 'topic', { durable: true });
  await channel.assertQueue(AUDIT_QUEUE, { durable: true });
  await channel.bindQueue(AUDIT_QUEUE, EXCHANGE, ROUTING_KEY);
  await channel.purgeQueue(AUDIT_QUEUE);
  await channel.close();

  const health = await axios.get(`${ORDER_URL}/actuator/health`);
  assert.strictEqual(health.status, 200);
});

When('I create an order', async function () {
  const response = await axios.post(`${ORDER_URL}/orders`, {
    productId: 'p1',
    quantity: 1,
  });
  assert.strictEqual(response.status, 201);
  lastOrderId = response.data.id;
});

When(
  'I try to create an order with product {string} and quantity {int}',
  async function (productId: string, quantity: number) {
    try {
      lastResponse = await axios.post(`${ORDER_URL}/orders`, {
        productId,
        quantity,
      });
    } catch (error: any) {
      if (error.response) {
        lastResponse = error.response;
      } else {
        throw error;
      }
    }
  },
);

Then(
  'the order should eventually be {string}',
  async function (expectedStatus: string) {
    await waitForStatus(lastOrderId, expectedStatus);
  },
);

Then(
  'an {string} event should be observable',
  async function (eventName: string) {
    const channel = await connection.createChannel();
    const message = await new Promise<any>((resolve) => {
      let consumerTag: string | undefined;
      const timer = setTimeout(async () => {
        if (consumerTag) {
          await channel.cancel(consumerTag);
        }
        resolve(null);
      }, 10000);

      channel
        .consume(
          AUDIT_QUEUE,
          async (candidate: any) => {
            if (!candidate) {
              return;
            }
            const content = JSON.parse(candidate.content.toString());
            if (content.payload?.orderId === lastOrderId) {
              clearTimeout(timer);
              channel.ack(candidate);
              resolve(candidate);
            } else {
              channel.ack(candidate);
            }
          },
          { noAck: false },
        )
        .then((result) => {
          consumerTag = result.consumerTag;
        });
    });

    assert.ok(message, 'No matching order-created event received');
    const content = JSON.parse(message.content.toString());
    assert.strictEqual(eventName, 'order.created.v1');
    assert.strictEqual(content.eventType, 'order.created');
    assert.strictEqual(content.eventVersion, 1);
    assert.strictEqual(content.payload.orderId, lastOrderId);
    assert.ok(content.eventId);
    assert.ok(content.correlationId);
    await channel.close();
  },
);

Then('the response status should be {int}', async function (status: number) {
  assert.strictEqual(lastResponse?.status, status);
});

After(async function () {
  if (connection) {
    await connection.close();
    connection = undefined;
  }
});
