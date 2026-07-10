package com.ciro.orderservice.service;

import com.ciro.orderservice.client.InventoryGateway;
import com.ciro.orderservice.dto.OrderRequest;
import com.ciro.orderservice.dto.OrderResponse;
import com.ciro.orderservice.dto.ProductResponse;
import com.ciro.orderservice.enums.OrderStatus;
import com.ciro.orderservice.event.OrderCreatedEvent;
import com.ciro.orderservice.event.OrderItemEvent;
import com.ciro.orderservice.exception.InsufficientStockException;
import com.ciro.orderservice.exception.OrderNotFoundException;
import com.ciro.orderservice.mapper.OrderMapper;
import com.ciro.orderservice.model.Order;
import com.ciro.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService
{
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryGateway inventoryGateway;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest)
    {
        // Guard: check every line item against inventory BEFORE persisting, so we bail
        // out without touching the DB if any product is short on stock.
        // We only *check* stock here — the real decrement happens asynchronously via a
        // RabbitMQ event (Phase 5). Keeping order/inventory decoupled is the whole point:
        // a synchronous decrement would couple both services into one transaction.
        orderRequest.items().forEach(item ->
        {
            ProductResponse product = inventoryGateway.getProduct(item.productId());
            if (product.availableQuantity() < item.quantity())
            {
                throw new InsufficientStockException(item.productId(), product.availableQuantity());
            }
        });

        Order order = orderMapper.toEntity(orderRequest);
        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(savedOrder.getId(), savedOrder
                .getItems()
                .stream()
                .map(item -> new OrderItemEvent(item.getProductId(), item.getQuantity())).toList());

        applicationEventPublisher.publishEvent(event);

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
