package com.ciro.orderservice.enums;

/**
 * Lifecycle states of an order. Modelled as an enum (not a free String) for type safety:
 * it's impossible to persist a typo like "pendinge".
 */
public enum OrderStatus
{
    PENDING,
    CONFIRMED,
    PREPARING,
    DELIVERED,
    CANCELLED
}
