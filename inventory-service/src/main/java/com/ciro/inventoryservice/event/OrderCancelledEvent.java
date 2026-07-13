package com.ciro.inventoryservice.event;

/**
 * Reverse event: inventory couldn't reserve stock for an order (a race lost the last units),
 * so order-service should move the order to CANCELLED — the compensating action of the saga.
 */
public record OrderCancelledEvent(Long orderId)
{
}
