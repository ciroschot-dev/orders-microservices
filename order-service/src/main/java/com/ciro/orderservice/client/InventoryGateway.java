package com.ciro.orderservice.client;

import com.ciro.orderservice.dto.ProductResponse;
import com.ciro.orderservice.exception.InventoryUnavailableException;
import com.ciro.orderservice.exception.ProductNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryGateway
{
    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventory", fallbackMethod = "getProductFallback")
    public ProductResponse getProduct(Long id)
    {
        try
        {
            return inventoryClient.getProductById(id);
        }
        catch (FeignException.NotFound e)
        {
            throw new ProductNotFoundException(id); // business answer, not an outage
        }
    }

    private ProductResponse getProductFallback(Long id, Throwable t)
    {
        // Business answer that bubbled up (product doesn't exist): let it through → 404.
        if (t instanceof ProductNotFoundException)
        {
            throw new ProductNotFoundException(id);
        }
        // Anything else (down, timeout, breaker OPEN → CallNotPermittedException): 503.
        throw new InventoryUnavailableException();
    }
}
