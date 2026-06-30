package com.ciro.orderservice.service;

import com.ciro.orderservice.dto.OrderRequest;
import com.ciro.orderservice.dto.OrderResponse;
import com.ciro.orderservice.enums.OrderStatus;
import com.ciro.orderservice.exception.OrderNotFoundException;
import com.ciro.orderservice.mapper.OrderMapper;
import com.ciro.orderservice.model.Order;
import com.ciro.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService
{
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest)
    {
        Order order = orderMapper.toEntity(orderRequest);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders()
    {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId)
    {
        return orderRepository.findById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus)
    {
        return orderRepository.findById(id)
                .map(order ->
                {
                    order.setStatus(newStatus);
                    return orderMapper.toResponse(order);
                })
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
