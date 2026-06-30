package com.ciro.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String productName,
        Long productId,
        int quantity,
        BigDecimal unitPrice
)
{
}
