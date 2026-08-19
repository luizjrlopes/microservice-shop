package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import com.shop.order.infrastructure.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderServiceTest {

  @Mock private OrderRepository repository;

  @InjectMocks private ConfirmOrderService service;

  @Test
  void confirmUpdatesStatusAndPersists() {
    Order existing = new Order("p1", 1);
    when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

    Order confirmed = service.confirm(existing.getId());

    assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
    verify(repository).update(existing);
  }

  @Test
  void confirmIsIdempotentWhenOrderIsAlreadyConfirmed() {
    Order existing = new Order("p1", 1);
    existing.setStatus("CONFIRMED");
    when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

    Order confirmed = service.confirm(existing.getId());

    assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
    verify(repository, never()).update(existing);
  }

  @Test
  void confirmThrowsWhenOrderIsMissing() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirm("missing"))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found: missing");
  }
}
