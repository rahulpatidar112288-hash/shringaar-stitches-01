package com.shringaar.store.repository;

import java.util.List;
import java.util.Optional;

import com.shringaar.store.domain.CustomerOrder;
import com.shringaar.store.domain.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByReference(String reference);
    boolean existsByReference(String reference);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);
}
