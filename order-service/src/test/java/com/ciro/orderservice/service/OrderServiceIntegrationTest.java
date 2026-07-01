package com.ciro.orderservice.service;

import com.ciro.orderservice.dto.OrderItemRequest;
import com.ciro.orderservice.dto.OrderRequest;
import com.ciro.orderservice.dto.OrderResponse;
import com.ciro.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class OrderServiceIntegrationTest
{
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_persistsWithItems()
    {
        // given: A real order with an item
        OrderRequest request = new OrderRequest("Ciro", List.of(
                new OrderItemRequest(42L, "Pizza Muzza", 2, new BigDecimal("5000.00"))
        ));

        // when: Create and reread the real database
        OrderResponse created = orderService.createOrder(request);
        OrderResponse fetched = orderService.getOrderById(created.id());

        // then: The chain works versus a real postgres db
        assertThat(fetched.customerName()).isEqualTo("Ciro");
        assertThat(fetched.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(fetched.items()).hasSize(1);
        assertThat(fetched.items().getFirst().productName()).isEqualTo("Pizza Muzza");
    }

    @Test
    void updateStatus_persistsNewStatus()
    {
        OrderResponse created = orderService.createOrder(
                new OrderRequest("Ciro", List.of(
                        new OrderItemRequest(42L, "Pizza", 1, new BigDecimal("5000.00")))));

        orderService.updateOrderStatus(created.id(), OrderStatus.CONFIRMED);

        OrderResponse fetched = orderService.getOrderById(created.id());
        assertThat(fetched.status()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
