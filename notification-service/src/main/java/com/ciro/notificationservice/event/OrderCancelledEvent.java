package com.ciro.notificationservice.event;

/**
 * Reverse event from inventory-service: the order's stock couldn't be reserved.
 * We consume it to notify the customer that their order was cancelled.
 */
public record OrderCancelledEvent(Long orderId)
{
}
