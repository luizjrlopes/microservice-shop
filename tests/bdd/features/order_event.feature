Feature: Reliable order processing

  Scenario: an order is confirmed end to end
    Given the distributed order stack is running
    When I create an order
    Then the order should eventually be "CONFIRMED"

  Scenario: order.created.v1 is published with the versioned envelope
    Given the distributed order stack is running
    When I create an order
    Then an "order.created.v1" event should be observable

  Scenario: invalid quantity is rejected
    Given the distributed order stack is running
    When I try to create an order with product "p1" and quantity 0
    Then the response status should be 400

  Scenario: blank product id is rejected
    Given the distributed order stack is running
    When I try to create an order with product "" and quantity 1
    Then the response status should be 400
