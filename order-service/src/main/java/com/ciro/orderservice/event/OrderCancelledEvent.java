package com.ciro.orderservice.event;

/**
 * Reverse event from inventory-service: stock couldn't be reserved (race lost the last units).
 * We consume it to move the order to CANCELLED (the saga's compensating action).
 */
public record OrderCancelledEvent(Long orderId)
{
}
