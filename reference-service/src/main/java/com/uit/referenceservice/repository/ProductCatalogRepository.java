package com.uit.referenceservice.repository;

import com.uit.referenceservice.entity.ProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, Integer> {
    List<ProductCatalog> findByStatus(String status);
    List<ProductCatalog> findByCategoryAndStatus(String category, String status);
}

