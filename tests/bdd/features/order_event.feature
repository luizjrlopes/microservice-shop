Feature: Order events
  Scenario: publishing order.created after creating order
    Given the order service is running
    When I create an order
    Then an "order.created" event should be published

  Scenario: failing to create order with nonexistent product
    Given the order service is running
    When I try to create an order with product "unknown" and quantity 1
    Then the response status should be 404

  Scenario: failing to create order with invalid quantity
    Given the order service is running
    When I try to create an order with product "p1" and quantity 0
    Then the response status should be 400
