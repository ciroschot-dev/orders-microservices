package com.ciro.orderservice.event;

/**
 * Reverse event from inventory-service: stock was reserved OK for this order.
 * We consume it to move the order to CONFIRMED (closes the saga).
 */
public record OrderConfirmedEvent(Long orderId)
{
}
