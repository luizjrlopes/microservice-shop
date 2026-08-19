package com.shop.order.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
  public static final String ORDER_EXCHANGE = "order.exchange";
  public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
  public static final String ORDER_CREATED_QUEUE = "order.created";
  public static final String ORDER_RETRY_EXCHANGE = "order.retry.exchange";
  public static final String ORDER_RETRY_QUEUE = "order.created.retry";
  public static final String ORDER_DLQ_EXCHANGE = "order.dlx";
  public static final String ORDER_DLQ_QUEUE = "order.created.dlq";

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange orderRetryExchange() {
    return new DirectExchange(ORDER_RETRY_EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange orderDeadLetterExchange() {
    return new DirectExchange(ORDER_DLQ_EXCHANGE, true, false);
  }

  @Bean
  public Queue orderCreatedQueue() {
    return QueueBuilder.durable(ORDER_CREATED_QUEUE)
        .deadLetterExchange(ORDER_RETRY_EXCHANGE)
        .deadLetterRoutingKey(ORDER_RETRY_QUEUE)
        .build();
  }

  @Bean
  public Queue orderCreatedRetryQueue(@Value("${order.retry.delay-ms:5000}") int retryDelayMs) {
    return QueueBuilder.durable(ORDER_RETRY_QUEUE)
        .ttl(retryDelayMs)
        .deadLetterExchange(ORDER_EXCHANGE)
        .deadLetterRoutingKey(ORDER_CREATED_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue orderCreatedDeadLetterQueue() {
    return QueueBuilder.durable(ORDER_DLQ_QUEUE).build();
  }

  @Bean
  public Binding orderCreatedBinding(
      @Qualifier("orderCreatedQueue") Queue queue, TopicExchange orderExchange) {
    return BindingBuilder.bind(queue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
  }

  @Bean
  public Binding orderRetryBinding(
      @Qualifier("orderCreatedRetryQueue") Queue queue, DirectExchange orderRetryExchange) {
    return BindingBuilder.bind(queue).to(orderRetryExchange).with(ORDER_RETRY_QUEUE);
  }

  @Bean
  public Binding orderDeadLetterBinding(
      @Qualifier("orderCreatedDeadLetterQueue") Queue queue,
      DirectExchange orderDeadLetterExchange) {
    return BindingBuilder.bind(queue).to(orderDeadLetterExchange).with(ORDER_DLQ_QUEUE);
  }

  @Bean
  public Jackson2JsonMessageConverter jacksonConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(cf);
    template.setMessageConverter(converter);
    return template;
  }
}
