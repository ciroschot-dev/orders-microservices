package com.ciro.orderservice.exception;

public class ProductNotFoundException extends RuntimeException
{
    public ProductNotFoundException(Long productId)
    {
        super("Product does not exist: id " + productId);
    }
}
