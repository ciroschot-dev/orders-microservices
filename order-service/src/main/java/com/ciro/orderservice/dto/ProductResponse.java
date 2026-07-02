package com.ciro.orderservice.dto;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        Integer availableQuantity
)
{
}
