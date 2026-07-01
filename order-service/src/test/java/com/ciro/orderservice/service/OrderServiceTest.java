package com.ciro.orderservice.service;

import com.ciro.orderservice.dto.OrderRequest;
import com.ciro.orderservice.dto.OrderResponse;
import com.ciro.orderservice.enums.OrderStatus;
import com.ciro.orderservice.exception.OrderNotFoundException;
import com.ciro.orderservice.mapper.OrderMapper;
import com.ciro.orderservice.model.Order;
import com.ciro.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit test: no Spring context and no database. It exercises OrderService in isolation,
// which makes it fast and pinpoints failures to the service itself.
// MockitoExtension wires up the @Mock / @InjectMocks fields before each test.
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest
{
    // Fake collaborators — we control exactly what they return, so the DB and the real mapper
    // never run here.
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    // The real service under test, with the two mocks above injected into its constructor.
    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_returnsMappedResponse()
    {
        // given: build the inputs and program what each mock returns for this scenario
        OrderRequest orderRequest = new OrderRequest("Ciro", List.of());
        Order order = new Order();
        Order orderSaved = new Order();
        OrderResponse orderResponseExpected =
                new OrderResponse(1L, "Ciro", OrderStatus.PENDING, Instant.now(), List.of());

        // Stub the collaboration chain the service is expected to follow: map -> save -> map back.
        when(orderMapper.toEntity(orderRequest)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(orderSaved);
        when(orderMapper.toResponse(orderSaved)).thenReturn(orderResponseExpected);

        // when: run the actual method under test
        OrderResponse result = orderService.createOrder(orderRequest);

        // then: it returns what the mapper produced, and it did persist the entity
        assertThat(result).isEqualTo(orderResponseExpected);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_whenMissing_throwsNotFound()
    {
        // Simulate "id not in the DB": findById returns an empty Optional...
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // ...so the service must translate that into our domain exception (later mapped to a 404).
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateOrderStatus_whenMissing_throwsNotFound()
    {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, OrderStatus.PENDING))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
