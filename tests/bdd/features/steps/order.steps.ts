import { Given, When, Then } from '@cucumber/cucumber';
import axios from 'axios';
import * as amqp from 'amqplib';
import assert from 'assert';

let connection: any;
let lastOrderId: string;

Given('the order service is running', async function () {
  connection = await amqp.connect(process.env.RABBIT_URL || 'amqp://localhost');
  const channel = await connection.createChannel();
  await channel.purgeQueue('order.created');
  await channel.close();
});

When('I create an order', async function () {
  const res = await axios.post('http://localhost:8080/orders', { productId: 'p1', quantity: 1 });
  lastOrderId = res.data.id;
});

Then('an {string} event should be published', async function (routingKey: string) {
  const channel = await connection.createChannel();
  const msg = await new Promise<any>((resolve) => {
    channel.consume('order.created', (m: any) => {
      if (m) {
        channel.ack(m);
        resolve(m);
      }
    });
    setTimeout(() => resolve(null), 2000);
  });
  await channel.close();
  await connection.close();
  assert.ok(msg, 'No message received');
  const content = JSON.parse(msg!.content.toString());
  assert.strictEqual(content.id, lastOrderId);
});
