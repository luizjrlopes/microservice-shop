package com.shop.order.domain;

import java.util.UUID;

public class Order {
  private final String id;
  private final String productId;
  private final int quantity;
  private String status;

  public Order(String productId, int quantity) {
    this.id = UUID.randomUUID().toString();
    this.productId = productId;
    this.quantity = quantity;
    this.status = "PENDING";
  }

  public String getId() {
    return id;
  }

  public String getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
