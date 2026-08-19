package com.shop.order.infrastructure.persistence;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresOrderRepositoryAdapter implements OrderRepository {
  private final SpringDataOrderJpaRepository repository;

  public PostgresOrderRepositoryAdapter(SpringDataOrderJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Order order) {
    repository.save(JpaOrderEntity.fromDomain(order));
  }

  @Override
  public Optional<Order> findById(UUID id) {
    return repository.findById(id).map(JpaOrderEntity::toDomain);
  }

  @Override
  public void update(Order order) {
    repository.save(JpaOrderEntity.fromDomain(order));
  }
}
