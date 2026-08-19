package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

  @Mock private OrderRepository repository;

  @InjectMocks private GetOrderService service;

  @Test
  void returnsPersistedOrder() {
    Order order = new Order("SKU-1", 2);
    when(repository.findById(order.getId())).thenReturn(Optional.of(order));

    assertThat(service.get(order.getId())).isSameAs(order);
  }

  @Test
  void throwsWhenOrderDoesNotExist() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(id))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found: " + id);
  }
}
