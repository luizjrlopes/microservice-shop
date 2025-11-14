package com.shop.order.interfaces;

import com.shop.order.application.ConfirmOrderService;
import com.shop.order.application.CreateOrderService;
import com.shop.order.domain.Order;
import com.shop.order.infrastructure.OrderEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final CreateOrderService createOrderService;
  private final OrderEventPublisher eventPublisher;
  private final ConfirmOrderService confirmOrderService;

  public OrderController(
      CreateOrderService createOrderService,
      OrderEventPublisher eventPublisher,
      ConfirmOrderService confirmOrderService) {
    this.createOrderService = createOrderService;
    this.eventPublisher = eventPublisher;
    this.confirmOrderService = confirmOrderService;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
    Order order = createOrderService.create(request.productId(), request.quantity());
    eventPublisher.publish(order);
    return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponse(order.getId()));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<Void> confirm(@PathVariable String id) {
    confirmOrderService.confirm(id);
    return ResponseEntity.ok().build();
  }

  public record CreateOrderRequest(String productId, int quantity) {}

  public record OrderResponse(String id) {}
}
