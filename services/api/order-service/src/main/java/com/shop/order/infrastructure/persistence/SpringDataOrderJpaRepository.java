package com.shop.order.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderJpaRepository extends JpaRepository<JpaOrderEntity, UUID> {}
