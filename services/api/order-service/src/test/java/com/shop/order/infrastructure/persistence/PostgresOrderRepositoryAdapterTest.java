package com.shop.order.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresOrderRepositoryAdapter.class)
class PostgresOrderRepositoryAdapterTest {

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

  @Autowired private OrderRepository repository;

  @Test
  void savesReadsAndUpdatesOrder() {
    Order order = new Order("SKU-1", 2);
    repository.save(order);

    Order persisted = repository.findById(order.getId()).orElseThrow();
    assertThat(persisted.getProductId()).isEqualTo("SKU-1");
    assertThat(persisted.getQuantity()).isEqualTo(2);
    assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PENDING);

    persisted.confirm();
    repository.update(persisted);

    Order confirmed = repository.findById(order.getId()).orElseThrow();
    assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
  }
}
