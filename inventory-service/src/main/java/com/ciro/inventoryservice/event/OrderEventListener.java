package com.ciro.inventoryservice.event;

import com.ciro.inventoryservice.config.RabbitMQConfig;
import com.ciro.inventoryservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes OrderCreated events from the queue. Like a controller, but for a queue instead of a URL:
 * it only receives and delegates — the business logic lives in the service.
 */
@Component
@RequiredArgsConstructor
public class OrderEventListener
{
    private final ProductService productService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event)
    {
        productService.applyOrderCreated(event);
    }
}
