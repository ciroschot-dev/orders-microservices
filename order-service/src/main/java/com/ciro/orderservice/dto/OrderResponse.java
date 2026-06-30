package com.ciro.orderservice.dto;

import com.ciro.orderservice.enums.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponse> items
)
{
}
