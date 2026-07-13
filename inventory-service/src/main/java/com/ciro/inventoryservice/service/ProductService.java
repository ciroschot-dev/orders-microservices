package com.ciro.inventoryservice.service;

import com.ciro.inventoryservice.dto.ProductRequest;
import com.ciro.inventoryservice.dto.ProductResponse;
import com.ciro.inventoryservice.event.OrderCreatedEvent;
import com.ciro.inventoryservice.exception.DuplicateSkuException;
import com.ciro.inventoryservice.exception.ProductNotFoundException;
import com.ciro.inventoryservice.mapper.ProductMapper;
import com.ciro.inventoryservice.model.Product;
import com.ciro.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // wouldn't help: the stock won't reappear). The compensating event (cancel the order) comes later.
    @Transactional
    public void applyOrderCreated(OrderCreatedEvent event)
    {
        event.items().forEach(item ->
        {
            int updated = productRepository.decrementStock(item.productId(), item.quantity());
            if (updated == 0)
            {
                log.warn("Insufficient stock for product {} (order {}): stock not decremented",
                        item.productId(), event.orderId());
            }
        });
    }
}
