package com.ciro.orderservice.controller;


import com.ciro.orderservice.dto.OrderRequest;
import com.ciro.orderservice.dto.OrderResponse;
import com.ciro.orderservice.dto.UpdateOrderStatusRequest;
import com.ciro.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController
{
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request)
    {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderResponse> getAll()
    {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id)
    {
        return orderService.getOrderById(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request)
    {
        return orderService.updateOrderStatus(id, request.status());
    }
}
