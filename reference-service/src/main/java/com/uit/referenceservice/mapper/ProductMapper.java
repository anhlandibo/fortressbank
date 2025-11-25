package com.uit.referenceservice.mapper;

import com.uit.referenceservice.dto.response.ProductResponse;
import com.uit.referenceservice.entity.ProductCatalog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toDto(ProductCatalog product);
    List<ProductResponse> toDtoList(List<ProductCatalog> products);
}

