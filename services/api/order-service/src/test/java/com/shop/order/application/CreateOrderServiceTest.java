package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.shop.order.application.events.OrderCreatedEventV1;
import com.shop.order.application.ports.OrderRepository;
import com.shop.order.application.ports.OutboxRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OutboxRepository outboxRepository;

  @InjectMocks private CreateOrderService service;

  @Test
  void createPersistsOrderAndOutboxEvent() {
    Order order = service.create("p1", 2);

    verify(orderRepository).save(order);
    var eventCaptor = ArgumentCaptor.forClass(OrderCreatedEventV1.class);
    verify(outboxRepository).save(eventCaptor.capture());

    var event = eventCaptor.getValue();
    assertThat(event.eventType()).isEqualTo("order.created");
    assertThat(event.eventVersion()).isEqualTo(1);
    assertThat(event.correlationId()).isEqualTo(order.getId());
    assertThat(event.payload().orderId()).isEqualTo(order.getId());
    assertThat(event.payload().productId()).isEqualTo("p1");
    assertThat(event.payload().quantity()).isEqualTo(2);
    assertThat(event.payload().status()).isEqualTo("PENDING");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void createRejectsBlankProductId() {
    assertThatThrownBy(() -> service.create(" ", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("productId must not be blank");
  }

  @Test
  void createRejectsNonPositiveQuantity() {
    assertThatThrownBy(() -> service.create("p1", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }
}
