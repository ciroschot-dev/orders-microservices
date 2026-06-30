package com.ciro.orderservice.dto;

import com.ciro.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
)
{
}
