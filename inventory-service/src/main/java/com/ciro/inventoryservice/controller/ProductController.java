package com.ciro.inventoryservice.controller;

import com.ciro.inventoryservice.dto.ProductRequest;
import com.ciro.inventoryservice.dto.ProductResponse;
import com.ciro.inventoryservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController
{
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request)
    {
        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts()
    {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id)
    {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProductById(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request
    )
    {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductById(@PathVariable Long id)
    {
        productService.deleteProduct(id);
    }
}
