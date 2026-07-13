package com.ciro.inventoryservice.service;

import com.ciro.inventoryservice.dto.ProductRequest;
import com.ciro.inventoryservice.dto.ProductResponse;
import com.ciro.inventoryservice.config.RabbitMQConfig;
import com.ciro.inventoryservice.event.OrderCancelledEvent;
import com.ciro.inventoryservice.event.OrderConfirmedEvent;
import com.ciro.inventoryservice.event.OrderCreatedEvent;
import com.ciro.inventoryservice.exception.DuplicateSkuException;
import com.ciro.inventoryservice.exception.ProductNotFoundException;
import com.ciro.inventoryservice.mapper.ProductMapper;
import com.ciro.inventoryservice.model.Product;
import com.ciro.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService
{
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ProductResponse createProduct(ProductRequest request)
    {
        if (productRepository.findBySku(request.sku()).isPresent())
        {
            throw new DuplicateSkuException(request.sku());
        }
        Product product = productMapper.toEntity(request);
        product = productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id)
    {
        return productMapper.toResponse(productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts()
    {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request)
    {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equals(request.sku())
                && productRepository.findBySku(request.sku()).isPresent())
        {
            throw new DuplicateSkuException(request.sku());
        }

        product.setName(request.name());
        product.setSku(request.sku());
        product.setAvailableQuantity(request.availableQuantity());

        return productMapper.toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id)
    {
        if (!productRepository.existsById(id))
        {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    // Reacts to an OrderCreated event: decrements stock for every line, atomically.
    // A 0 rows-affected means "not enough stock" — a business failure, so we log it (retrying
    // wouldn't help: the stock won't reappear).
    // Then closes the saga by publishing a reverse event so order-service can move the order to
    // CONFIRMED (all lines reserved) or CANCELLED (a race lost the last units).
    // Nota: si una línea falla pero otras ya se descontaron, no revertimos ese stock parcial
    // (la compensación completa quedó fuera de alcance); el pre-check de createOrder hace que
    // este caso solo aparezca en una carrera por las últimas unidades.
    @Transactional
    public void applyOrderCreated(OrderCreatedEvent event)
    {
        boolean allFulfilled = true;
        for (var item : event.items())
        {
            int updated = productRepository.decrementStock(item.productId(), item.quantity());
            if (updated == 0)
            {
                allFulfilled = false;
                log.warn("Insufficient stock for product {} (order {}): stock not decremented",
                        item.productId(), event.orderId());
            }
        }

        if (allFulfilled)
        {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CONFIRMED_ROUTING_KEY, new OrderConfirmedEvent(event.orderId()));
        }
        else
        {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY, new OrderCancelledEvent(event.orderId()));
        }
    }
}
