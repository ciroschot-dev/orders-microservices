package com.ciro.inventoryservice.event;

/**
 * One line of the order — just what inventory needs to decrement stock.
 */
public record OrderItemEvent(
        Long productId,
        int quantity
)
{
}
