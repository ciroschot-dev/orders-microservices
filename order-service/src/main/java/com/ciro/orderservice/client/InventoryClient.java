package com.ciro.orderservice.client;

import com.ciro.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service")
public interface InventoryClient
{
    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);
}
