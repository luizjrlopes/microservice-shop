package com.shop.order.interfaces;

import com.shop.order.application.ConfirmOrderService;
import com.shop.order.application.CreateOrderService;
import com.shop.order.application.GetOrderService;
import com.shop.order.domain.Order;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final CreateOrderService createOrderService;
  private final ConfirmOrderService confirmOrderService;
  private final GetOrderService getOrderService;

  public OrderController(
      CreateOrderService createOrderService,
      ConfirmOrderService confirmOrderService,
      GetOrderService getOrderService) {
    this.createOrderService = createOrderService;
    this.confirmOrderService = confirmOrderService;
    this.getOrderService = getOrderService;
  }

  @PostMapping
  public ResponseEntity<OrderCreatedResponse> create(@RequestBody CreateOrderRequest request) {
    Order order = createOrderService.create(request.productId(), request.quantity());
    return ResponseEntity.status(HttpStatus.CREATED).body(new OrderCreatedResponse(order.getId()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(OrderResponse.from(getOrderService.get(id)));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<Void> confirm(@PathVariable UUID id) {
    confirmOrderService.confirm(id);
    return ResponseEntity.ok().build();
  }

  public record CreateOrderRequest(String productId, int quantity) {}

  public record OrderCreatedResponse(UUID id) {}

  public record OrderResponse(
      UUID id,
      String productId,
      int quantity,
      String status,
      Instant createdAt,
      Instant updatedAt) {
    static OrderResponse from(Order order) {
      return new OrderResponse(
          order.getId(),
          order.getProductId(),
          order.getQuantity(),
          order.getStatus().name(),
          order.getCreatedAt(),
          order.getUpdatedAt());
    }
  }
}
