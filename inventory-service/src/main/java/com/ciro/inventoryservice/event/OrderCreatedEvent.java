package com.ciro.inventoryservice.event;

import java.util.List;

/**
 * Message contract published to RabbitMQ when an order is created.
 * Deliberately a flat DTO, not the JPA entity: consumers must not depend on our domain model.
 */
public record OrderCreatedEvent(
        Long orderId,
        List<OrderItemEvent> items
)
{
}
