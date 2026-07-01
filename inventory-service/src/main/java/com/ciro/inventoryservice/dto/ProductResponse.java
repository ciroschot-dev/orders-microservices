package com.ciro.inventoryservice.dto;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        Integer availableQuantity
)
{
}
