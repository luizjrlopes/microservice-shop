package com.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.order.application.ports.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "outbox.publisher.initial-delay-ms=600000")
@Testcontainers
class CreateOrderTransactionalIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("microservice_shop")
          .withUsername("microservice_shop")
          .withPassword("microservice_shop");

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private CreateOrderService service;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockBean private OutboxEventPublisher outboxEventPublisher;

  @Test
  void persistsOrderAndOutboxTogetherAndRollsBackWhenOutboxCannotBeWritten() {
    service.create("SKU-ATOMIC", 2);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isEqualTo(1L);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class))
        .isEqualTo(1L);

    jdbcTemplate.update("DELETE FROM outbox_events");
    jdbcTemplate.update("DELETE FROM orders");
    jdbcTemplate.execute("DROP TABLE outbox_events");

    assertThatThrownBy(() -> service.create("SKU-ROLLBACK", 1))
        .isInstanceOf(RuntimeException.class);

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class)).isZero();
  }
}
