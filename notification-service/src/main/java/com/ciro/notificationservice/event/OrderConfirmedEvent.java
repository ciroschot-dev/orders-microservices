package com.ciro.notificationservice.event;

/**
 * Reverse event from inventory-service: the order's stock was reserved OK.
 * We consume it to notify the customer that their order is confirmed.
 */
public record OrderConfirmedEvent(Long orderId)
{
}
