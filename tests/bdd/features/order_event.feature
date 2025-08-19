Feature: Order events
  Scenario: publishing order.created after creating order
    Given the order service is running
    When I create an order
    Then an "order.created" event should be published
