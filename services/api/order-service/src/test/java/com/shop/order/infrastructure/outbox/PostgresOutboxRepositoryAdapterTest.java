package com.shop.order.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.application.events.OrderCreatedEventV1;
import com.shop.order.application.ports.OutboxRepository;
import com.shop.order.domain.Order;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  PostgresOutboxRepositoryAdapter.class,
  PostgresOutboxRepositoryAdapterTest.JacksonConfig.class
})
class PostgresOutboxRepositoryAdapterTest {

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

  @Autowired private OutboxRepository repository;

  @Test
  void storesVersionedEnvelopeAndTracksPublicationState() {
    Order order = new Order("SKU-1", 2);
    var event = OrderCreatedEventV1.from(order);

    repository.save(event);

    var pending = repository.findPending(10);
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).id()).isEqualTo(event.eventId());
    assertThat(pending.get(0).eventType()).isEqualTo("order.created");
    assertThat(pending.get(0).eventVersion()).isEqualTo(1);
    assertThat(pending.get(0).correlationId()).isEqualTo(order.getId());
    assertThat(pending.get(0).payload()).contains("\"eventVersion\":1");
    assertThat(pending.get(0).payload()).contains(order.getId().toString());

    repository.recordFailure(event.eventId(), "broker unavailable");
    assertThat(repository.findPending(10).get(0).attempts()).isEqualTo(1);

    repository.markPublished(event.eventId(), Instant.now());
    assertThat(repository.findPending(10)).isEmpty();
  }

  @TestConfiguration
  static class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }
}
