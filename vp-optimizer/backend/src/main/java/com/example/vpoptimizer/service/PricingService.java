package com.example.vpoptimizer.service;

import com.example.vpoptimizer.config.OptimizationProperties;
import com.example.vpoptimizer.dto.PricingPreviewRequest;
import com.example.vpoptimizer.dto.PricingPreviewResponse;
import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.mapper.ProductOptionMapper;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.ProductOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pricing preview for the optimization form: "what will each selected product actually cost?"
 *
 * <p>Read-only counterpart of the optimization run and the single place that turns a selection into
 * priced rows.</p>
 */
@Service
public class PricingService {

    private final ProductService productService;
    private final ProductOptionMapper productOptionMapper;
    private final SelectionPricingService selectionPricingService;
    private final OptimizationProperties optimizationProperties;

    public PricingService(ProductService productService,
                          ProductOptionMapper productOptionMapper,
                          SelectionPricingService selectionPricingService,
                          OptimizationProperties optimizationProperties) {
        this.productService = productService;
        this.productOptionMapper = productOptionMapper;
        this.selectionPricingService = selectionPricingService;
        this.optimizationProperties = optimizationProperties;
    }

    @Transactional(readOnly = true)
    public PricingPreviewResponse preview(PricingPreviewRequest request) {
        OptimizationLimits limits = optimizationProperties.toLimits();
        Set<Long> requiredIds = new LinkedHashSet<>(request.requiredProductIds());
        List<Product> products = productService.requireSelectableProducts(request.productIds());
        List<ProductOption> options = products.stream()
                .map(product -> productOptionMapper.toOption(product, requiredIds.contains(product.getId())))
                .toList();
        PricingContext pricing = PricingContext.global(request.discountPercent(), request.gstPercent());
        return new PricingPreviewResponse(
                pricing.discountPercent(),
                pricing.gstPercent(),
                selectionPricingService.describe(options, pricing, limits));
    }
}
