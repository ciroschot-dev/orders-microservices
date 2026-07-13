package com.ciro.notificationservice.event;

public record OrderItemEvent(
        Long productId,
        int quantity
)
{
}
