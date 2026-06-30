package com.ciro.orderservice.repository;

import com.ciro.orderservice.enums.OrderStatus;
import com.ciro.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>
{
    List<Order> findOrderByStatus(OrderStatus status);
}
