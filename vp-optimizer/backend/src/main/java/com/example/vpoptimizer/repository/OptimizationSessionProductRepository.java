package com.example.vpoptimizer.repository;

import com.example.vpoptimizer.entity.OptimizationSessionProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OptimizationSessionProductRepository extends JpaRepository<OptimizationSessionProduct, Long> {

    /** Used to protect products from hard deletion when history references them. */
    boolean existsByProductId(Long productId);

    List<OptimizationSessionProduct> findAllByProductIdIn(Collection<Long> productIds);
}
