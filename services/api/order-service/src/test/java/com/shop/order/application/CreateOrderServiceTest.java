package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.shop.order.domain.Order;
import com.shop.order.infrastructure.OrderRepository;
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
    assertThat(order.getStatus()).isEqualTo("PENDING");
    assertThat(order.getId()).isNotBlank();
  }
}
