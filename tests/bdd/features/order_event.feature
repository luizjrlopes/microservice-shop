Feature: Event-driven order processing
  Scenario: publishing order.created after creating an order
    Given the order service is running
    When I create an order
    Then an "order.created" event should be published

  Scenario: confirming an order asynchronously through the scheduler
    Given the order service is running
    When I create an order
    Then the order should eventually become "CONFIRMED"

  Scenario: rejecting an order with an invalid quantity
    Given the order service is running
    When I try to create an order with product "p1" and quantity 0
    Then the response status should be 400

  Scenario: rejecting an order without a product
    Given the order service is running
    When I try to create an order with product "" and quantity 1
    Then the response status should be 400
