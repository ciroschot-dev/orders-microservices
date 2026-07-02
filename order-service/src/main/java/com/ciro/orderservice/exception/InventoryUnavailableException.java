package com.ciro.orderservice.exception;

public class InventoryUnavailableException extends RuntimeException
{
    public InventoryUnavailableException()
    {
        super("Inventory service is unavailable.");
    }
}
