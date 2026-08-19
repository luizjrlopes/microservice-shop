package com.shop.order.interfaces;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.application.ConfirmOrderService;
import com.shop.order.application.CreateOrderService;
import com.shop.order.application.GetOrderService;
import com.shop.order.domain.Order;
import com.shop.order.infrastructure.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CreateOrderService createOrderService;

  @MockBean private OrderEventPublisher orderEventPublisher;

  @MockBean private ConfirmOrderService confirmOrderService;

  @MockBean private GetOrderService getOrderService;

  @Test
  void createReturnsCreatedAndPublishesEvent() throws Exception {
    Order order = new Order("p1", 1);
    when(createOrderService.create(eq("p1"), eq(1))).thenReturn(order);

    var request = new OrderController.CreateOrderRequest("p1", 1);

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(order.getId()));

    verify(orderEventPublisher).publish(order);
  }

  @Test
  void createRejectsInvalidQuantity() throws Exception {
    var request = new OrderController.CreateOrderRequest("p1", 0);

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRejectsBlankProductId() throws Exception {
    var request = new OrderController.CreateOrderRequest("", 1);

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getReturnsCurrentOrderState() throws Exception {
    Order order = new Order("p1", 2);
    when(getOrderService.get(order.getId())).thenReturn(order);

    mockMvc
        .perform(get("/orders/{id}", order.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.getId()))
        .andExpect(jsonPath("$.productId").value("p1"))
        .andExpect(jsonPath("$.quantity").value(2))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void confirmReturnsOkWhenServiceCompletes() throws Exception {
    var orderId = "order-123";

    mockMvc.perform(post("/orders/{id}/confirm", orderId)).andExpect(status().isOk());

    verify(confirmOrderService).confirm(orderId);
  }
}
