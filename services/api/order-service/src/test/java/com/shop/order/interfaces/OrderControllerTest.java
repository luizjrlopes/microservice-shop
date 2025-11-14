package com.shop.order.interfaces;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.application.ConfirmOrderService;
import com.shop.order.application.CreateOrderService;
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
  void confirmReturnsOkWhenServiceCompletes() throws Exception {
    var orderId = "order-123";

    mockMvc.perform(post("/orders/{id}/confirm", orderId)).andExpect(status().isOk());

    verify(confirmOrderService).confirm(orderId);
  }
}
