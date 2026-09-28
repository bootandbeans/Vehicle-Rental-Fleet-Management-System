package com.example.vpoptimizer.mapper;

import com.example.vpoptimizer.config.OptimizationProperties;
import com.example.vpoptimizer.dto.OptimizationRunResponse;
import com.example.vpoptimizer.dto.OptimizationSessionSummaryResponse;
import com.example.vpoptimizer.dto.SelectionPricingResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.service.SelectionPricingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps a persisted session to the API representation.
 *
 * <p>Results are always read back from the snapshot (never recalculated), which is what makes
 * historical sessions reproducible; only the "what does a unit cost" preview is derived again from
 * the stored rates.</p>
 */
@Component
public class OptimizationResponseMapper {

    private final SelectionPricingService selectionPricingService;
    private final ProductOptionMapper productOptionMapper;
    private final OptimizationProperties optimizationProperties;

    public OptimizationResponseMapper(SelectionPricingService selectionPricingService,
                                      ProductOptionMapper productOptionMapper,
                                      OptimizationProperties optimizationProperties) {
        this.selectionPricingService = selectionPricingService;
        this.productOptionMapper = productOptionMapper;
        this.optimizationProperties = optimizationProperties;
    }

    public OptimizationRunResponse toRunResponse(OptimizationSession session) {
        OptimizationLimits limits = optimizationProperties.toLimits();
        PricingContext pricing = PricingContext.global(session.getDiscountPercent(), session.getGstPercent());
        List<ProductOption> options = session.getSelections().stream()
                .map(productOptionMapper::toOption)
                .toList();
        List<SelectionPricingResponse> selection = selectionPricingService.describe(options, pricing, limits);

        return new OptimizationRunResponse(
                session.getId(),
                session.getName(),
                session.getCreatedAt(),
                session.getStatus(),
                session.getTargetVp(),
                session.getMinVp(),
                session.getMaxVp(),
                session.getToleranceType(),
                session.getToleranceValue(),
                session.getDiscountPercent(),
                session.getGstPercent(),
                session.getResultLimit(),
                session.getSolutionCount(),
                session.getAlternativeCount(),
                session.getMessage(),
                selection,
                SolutionMapper.ranked(session.getSolutions()),
                SolutionMapper.alternatives(session.getSolutions()),
                OptimizationSessionMapper.toDiagnostics(session));
    }

    public OptimizationSessionSummaryResponse toSummary(OptimizationSession session) {
        return OptimizationSessionMapper.toSummary(session);
    }
}
