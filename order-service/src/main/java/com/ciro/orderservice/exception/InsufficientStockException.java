package com.ciro.orderservice.exception;

public class InsufficientStockException extends RuntimeException
{
    public InsufficientStockException(Long id, int availableQuantity)
    {
        super("Insufficient stock for product with id: " + id + ". The available stock is: " + availableQuantity);
    }
}
