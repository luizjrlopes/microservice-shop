package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

  @Mock private OrderRepository repository;

  @InjectMocks private CreateOrderService service;

  @Test
  void createPersistsOrderAndReturnsInstance() {
    Order order = service.create("p1", 2);

    verify(repository).save(order);
    assertThat(order.getProductId()).isEqualTo("p1");
    assertThat(order.getQuantity()).isEqualTo(2);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getId()).isNotNull();
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
