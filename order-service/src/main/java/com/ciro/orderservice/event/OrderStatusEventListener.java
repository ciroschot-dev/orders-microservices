package com.ciro.orderservice.event;

import com.ciro.orderservice.config.RabbitMQConfig;
import com.ciro.orderservice.enums.OrderStatus;
import com.ciro.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume los eventos de vuelta de inventory-service y mueve la orden a su estado final.
 * Es el cierre de la saga: order.created (ida) → confirmed/cancelled (vuelta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusEventListener
{
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event)
    {
        log.info("Order {} confirmed by inventory", event.orderId());
        orderService.updateOrderStatus(event.orderId(), OrderStatus.CONFIRMED);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event)
    {
        log.warn("Order {} cancelled by inventory (no stock)", event.orderId());
        orderService.updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
    }
}
