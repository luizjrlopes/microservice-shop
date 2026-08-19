package com.shop.order.infrastructure.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxJpaRepository extends JpaRepository<JpaOutboxEventEntity, UUID> {
  List<JpaOutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);
}
