package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.SelectionPricingResponse;
import com.example.vpoptimizer.optimization.calculator.Money;
import com.example.vpoptimizer.optimization.calculator.PricingCalculator;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.PricingDetails;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.QuantityRange;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Describes the current selection with authoritative pricing.
 *
 * <p>Both the "selection summary" screen and the optimization response use this service, so the UI
 * never re-implements the discount/GST formulas and the effective quantity limits shown next to a
 * product are exactly the ones the engine applies.</p>
 */
@Service
public class SelectionPricingService {

    private final PricingCalculator pricingCalculator;

    public SelectionPricingService(PricingCalculator pricingCalculator) {
        this.pricingCalculator = pricingCalculator;
    }

    public List<SelectionPricingResponse> describe(List<ProductOption> options,
                                                   PricingContext pricing,
                                                   OptimizationLimits limits) {
        return options.stream()
                .map(option -> describe(option, pricing, limits))
                .toList();
    }

    public SelectionPricingResponse describe(ProductOption option, PricingContext pricing, OptimizationLimits limits) {
        PricingDetails unit = pricingCalculator.priceUnit(option, pricing);
        QuantityRange range = QuantityRange.resolve(option, limits);
        return new SelectionPricingResponse(
                option.id(),
                option.name(),
                option.sku(),
                option.categoryName(),
                option.selectionType(),
                Money.scale(option.mrp()),
                option.volumePoint(),
                option.minQuantity(),
                option.maxQuantity(),
                range.minimumQuantity(),
                range.maximumQuantity(),
                unit.discountPercent(),
                unit.gstPercent(),
                unit.discountAmount(),
                unit.discountedPrice(),
                unit.gstAmount(),
                unit.finalPrice());
    }
}
