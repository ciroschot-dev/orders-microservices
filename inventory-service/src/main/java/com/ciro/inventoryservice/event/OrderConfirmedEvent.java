package com.ciro.inventoryservice.event;

/**
 * Reverse event: inventory tells order-service that the stock for an order was reserved OK,
 * so the order can move to CONFIRMED. Closes the saga started by OrderCreatedEvent.
 */
public record OrderConfirmedEvent(Long orderId)
{
}
