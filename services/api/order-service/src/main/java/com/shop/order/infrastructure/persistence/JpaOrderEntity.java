package com.shop.order.infrastructure.persistence;

import com.shop.order.domain.Order;
import com.shop.order.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class JpaOrderEntity {
  @Id private UUID id;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected JpaOrderEntity() {}

  private JpaOrderEntity(
      UUID id,
      String productId,
      int quantity,
      OrderStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.productId = productId;
    this.quantity = quantity;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static JpaOrderEntity fromDomain(Order order) {
    return new JpaOrderEntity(
        order.getId(),
        order.getProductId(),
        order.getQuantity(),
        order.getStatus(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }

  public Order toDomain() {
    return Order.restore(id, productId, quantity, status, createdAt, updatedAt);
  }
}
