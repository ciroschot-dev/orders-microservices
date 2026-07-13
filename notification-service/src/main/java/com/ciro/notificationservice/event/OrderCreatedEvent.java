package com.ciro.notificationservice.event;

import java.util.List;

/**
 * Local copy of the OrderCreated contract. Duplicated on purpose (no shared JAR): the contract between
 * services is the JSON on the wire, not a Java class — sharing one would couple deploys at compile time.
 */
public record OrderCreatedEvent(
        Long orderId,
        List<OrderItemEvent> items
)
{
}
