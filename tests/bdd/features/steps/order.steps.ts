import { After, Given, When, Then } from '@cucumber/cucumber';
import axios from 'axios';
import * as amqp from 'amqplib';
import assert from 'assert';

const ORDER_URL = process.env.ORDER_URL || 'http://localhost:8080';
const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';

let connection: any;
let auditQueue: string;
let lastOrderId: string;
let lastResponse: any;

After(async function () {
  if (connection) {
    await connection.close();
    connection = undefined;
  }
});

Given('the order service is running', async function () {
  connection = await amqp.connect(RABBIT_URL);
  const channel = await connection.createChannel();
  await channel.assertExchange('order.exchange', 'topic', { durable: true });
  const queue = await channel.assertQueue('', { exclusive: true, autoDelete: true });
  auditQueue = queue.queue;
  await channel.bindQueue(auditQueue, 'order.exchange', 'order.created');
  await channel.close();
});

When('I create an order', async function () {
  const res = await axios.post(`${ORDER_URL}/orders`, { productId: 'p1', quantity: 1 });
  lastOrderId = res.data.id;
  lastResponse = res;
});

When(
  'I try to create an order with product {string} and quantity {int}',
  async function (productId: string, quantity: number) {
    try {
      const res = await axios.post(`${ORDER_URL}/orders`, { productId, quantity });
      lastResponse = res;
    } catch (err: any) {
      if (err.response) {
        lastResponse = err.response;
      } else {
        throw err;
      }
    }
  },
);

Then('an {string} event should be published', async function (routingKey: string) {
  const channel = await connection.createChannel();
  const msg = await new Promise<any>((resolve) => {
    const timer = setTimeout(() => resolve(null), 5000);
    channel.consume(
      auditQueue,
      (message: any) => {
        if (message) {
          clearTimeout(timer);
          channel.ack(message);
          resolve(message);
        }
      },
      { noAck: false },
    );
  });
  await channel.close();

  assert.ok(msg, 'No message received');
  assert.strictEqual(msg.fields.routingKey, routingKey);
  const content = JSON.parse(msg.content.toString());
  assert.strictEqual(content.id, lastOrderId);
  assert.ok(content.eventId, 'Event ID should be present');
  assert.ok(content.occurredAt, 'Event timestamp should be present');
});

Then('the order should eventually become {string}', async function (status: string) {
  const deadline = Date.now() + 15000;
  let observedStatus = '';

  while (Date.now() < deadline) {
    const response = await axios.get(`${ORDER_URL}/orders/${lastOrderId}`);
    observedStatus = response.data.status;
    if (observedStatus === status) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }

  assert.strictEqual(observedStatus, status);
});

Then('the response status should be {int}', async function (status: number) {
  assert.strictEqual(lastResponse?.status, status);
});
