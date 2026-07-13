package com.ciro.notificationservice.event;

import com.ciro.notificationservice.config.RabbitMQConfig;
import com.ciro.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes OrderCreated from this service's own queue and delegates to the service.
 * The same event also lands in inventory's queue — that's the fan-out.
 */
@Component
@RequiredArgsConstructor
public class OrderEventListener
{
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event)
    {
        notificationService.notifyOrderCreated(event);
    }
}
