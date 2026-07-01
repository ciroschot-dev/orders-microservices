package com.ciro.inventoryservice.mapper;

import com.ciro.inventoryservice.dto.ProductRequest;
import com.ciro.inventoryservice.dto.ProductResponse;
import com.ciro.inventoryservice.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper
{
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);
}
