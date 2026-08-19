package com.shop.order.interfaces;

import com.shop.order.application.ConfirmOrderService;
import com.shop.order.application.CreateOrderService;
import com.shop.order.application.GetOrderService;
import com.shop.order.domain.Order;
import com.shop.order.infrastructure.OrderEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
  private final OrderEventPublisher eventPublisher;
  private final ConfirmOrderService confirmOrderService;
  private final GetOrderService getOrderService;

  public OrderController(
      CreateOrderService createOrderService,
      OrderEventPublisher eventPublisher,
      ConfirmOrderService confirmOrderService,
      GetOrderService getOrderService) {
    this.createOrderService = createOrderService;
    this.eventPublisher = eventPublisher;
    this.confirmOrderService = confirmOrderService;
    this.getOrderService = getOrderService;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    Order order = createOrderService.create(request.productId(), request.quantity());
    eventPublisher.publish(order);
    return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponse(order.getId()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderView> get(@PathVariable String id) {
    Order order = getOrderService.get(id);
    return ResponseEntity.ok(OrderView.from(order));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<Void> confirm(@PathVariable String id) {
    confirmOrderService.confirm(id);
    return ResponseEntity.ok().build();
  }

  public record CreateOrderRequest(@NotBlank String productId, @Min(1) int quantity) {}

  public record OrderResponse(String id) {}

  public record OrderView(String id, String productId, int quantity, String status) {
    static OrderView from(Order order) {
      return new OrderView(
          order.getId(), order.getProductId(), order.getQuantity(), order.getStatus());
    }
  }
}
