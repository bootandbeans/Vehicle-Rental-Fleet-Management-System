package com.example.vpoptimizer.service;

import com.example.vpoptimizer.config.OptimizationProperties;
import com.example.vpoptimizer.dto.OptimizationRunRequest;
import com.example.vpoptimizer.dto.OptimizationRunResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.entity.OptimizationSessionProduct;
import com.example.vpoptimizer.entity.OptimizationSolution;
import com.example.vpoptimizer.entity.OptimizationSolutionLine;
import com.example.vpoptimizer.entity.OptimizationStatus;
import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.mapper.OptimizationResponseMapper;
import com.example.vpoptimizer.mapper.ProductOptionMapper;
import com.example.vpoptimizer.optimization.engine.OptimizationEngine;
import com.example.vpoptimizer.optimization.model.OptimizationLimits;
import com.example.vpoptimizer.optimization.model.OptimizationRequest;
import com.example.vpoptimizer.optimization.model.OptimizationResult;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.ProductOption;
import com.example.vpoptimizer.optimization.model.ProductQuantity;
import com.example.vpoptimizer.optimization.model.QuantityRange;
import com.example.vpoptimizer.optimization.model.Solution;
import com.example.vpoptimizer.repository.OptimizationSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates one optimization run:
 *
 * <pre>
 *   selection (ids)  ->  products from the database  ->  pricing context  ->  engine
 *                    ->  candidates  ->  ranking  ->  persisted snapshot  ->  API response
 * </pre>
 *
 * <p>The service owns persistence and translation; every business calculation stays in the engine,
 * the pricing calculator and the ranker.</p>
 */
@Service
@Transactional
public class OptimizationService {

    private static final Logger log = LoggerFactory.getLogger(OptimizationService.class);
    private static final int MAX_EXPLANATION_LENGTH = 1000;

    private final OptimizationEngine optimizationEngine;
    private final ProductService productService;
    private final ProductOptionMapper productOptionMapper;
    private final OptimizationResponseMapper responseMapper;
    private final OptimizationSessionRepository sessionRepository;
    private final OptimizationProperties optimizationProperties;

    public OptimizationService(OptimizationEngine optimizationEngine,
                               ProductService productService,
                               ProductOptionMapper productOptionMapper,
                               OptimizationResponseMapper responseMapper,
                               OptimizationSessionRepository sessionRepository,
                               OptimizationProperties optimizationProperties) {
        this.optimizationEngine = optimizationEngine;
        this.productService = productService;
        this.productOptionMapper = productOptionMapper;
        this.responseMapper = responseMapper;
        this.sessionRepository = sessionRepository;
        this.optimizationProperties = optimizationProperties;
    }

    /** Runs an optimization over exactly the products the user selected and stores the session. */
    public OptimizationRunResponse run(OptimizationRunRequest request) {
        OptimizationLimits limits = optimizationProperties.toLimits();
        List<Long> productIds = request.productIds().stream().distinct().toList();
        Set<Long> requiredIds = new LinkedHashSet<>(request.requiredProductIds());

        if (productIds.isEmpty()) {
            throw new BusinessException(ApiErrorCode.NO_PRODUCTS_SELECTED);
        }
        if (!productIds.containsAll(requiredIds)) {
            throw new BusinessException(ApiErrorCode.INVALID_SELECTION,
                    "A product can only be marked as REQUIRED when it is part of the selection.");
        }

        List<Product> selectedProducts = productService.requireSelectableProducts(productIds);
        List<ProductOption> options = selectedProducts.stream()
                .map(product -> productOptionMapper.toOption(product, requiredIds.contains(product.getId())))
                .toList();

        PricingContext pricing = PricingContext.global(request.discountPercent(), request.gstPercent());
        OptimizationRequest engineRequest = new OptimizationRequest(
                options,
                pricing,
                request.targetVp(),
                request.toleranceType(),
                request.toleranceValue(),
                request.resultLimitOrDefault(limits.defaultResultLimit()),
                limits);

        OptimizationResult result = optimizationEngine.optimize(engineRequest);
        log.info("Optimization over {} product(s) produced {} solution(s) and {} alternative(s) in {} ms",
                options.size(), result.solutions().size(), result.closestAlternatives().size(),
                result.diagnostics().elapsedMillis());

        OptimizationSession session = persist(request, selectedProducts, engineRequest, result, limits);
        return responseMapper.toRunResponse(session);
    }

    private OptimizationSession persist(OptimizationRunRequest request,
                                        List<Product> selectedProducts,
                                        OptimizationRequest engineRequest,
                                        OptimizationResult result,
                                        OptimizationLimits limits) {
        OptimizationSession session = new OptimizationSession(
                engineRequest.discountPercent(),
                engineRequest.gstPercent(),
                engineRequest.targetVp(),
                engineRequest.toleranceType(),
                engineRequest.toleranceValue(),
                result.minimumVp(),
                result.maximumVp(),
                result.requestedLimit());

        session.setName(request.name());
        session.setMessage(result.message());
        session.setStatus(result.hasSolutions() ? OptimizationStatus.COMPLETED : OptimizationStatus.NO_VALID_SOLUTION);
        session.setSolutionCount(result.solutions().size());
        session.setAlternativeCount(result.closestAlternatives().size());
        session.setDpCapacity(result.diagnostics().dpCapacity());
        session.setEvaluatedStates(result.diagnostics().exploredStates());
        session.setEngineMillis(result.diagnostics().elapsedMillis());

        Map<Long, Product> productsById = selectedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (ProductOption option : engineRequest.products()) {
            QuantityRange range = QuantityRange.resolve(option, limits);
            session.addSelection(new OptimizationSessionProduct(
                    productsById.get(option.id()),
                    option.name(),
                    option.sku(),
                    option.categoryName(),
                    option.selectionType(),
                    option.mrp(),
                    option.volumePoint(),
                    range.minimumQuantity(),
                    range.maximumQuantity()));
        }

        result.solutions().forEach(solution -> session.addSolution(toEntity(solution)));
        result.closestAlternatives().forEach(solution -> session.addSolution(toEntity(solution)));

        return sessionRepository.save(session);
    }

    /** Copies a calculated solution into its persisted (snapshot) representation. */
    private OptimizationSolution toEntity(Solution solution) {
        OptimizationSolution entity = new OptimizationSolution(
                solution.solutionRank(),
                solution.alternative(),
                solution.withinRange(),
                solution.exactTarget(),
                solution.canonicalKey(),
                Math.toIntExact(solution.totalVp()),
                Math.toIntExact(solution.vpDifference()),
                solution.totalQuantity(),
                solution.numberOfUniqueProducts(),
                solution.totalMrp(),
                solution.totalDiscount(),
                solution.totalGst(),
                solution.finalPayableAmount(),
                solution.costPerVp(),
                truncate(solution.explanation()));

        for (ProductQuantity line : solution.products()) {
            entity.addLine(new OptimizationSolutionLine(
                    line.productId(),
                    line.productName(),
                    line.sku(),
                    line.categoryName(),
                    line.required(),
                    line.quantity(),
                    line.mrp(),
                    line.volumePoint(),
                    Math.toIntExact(line.totalProductVp()),
                    line.discountPercent(),
                    line.gstPercent(),
                    line.discountedUnitPrice(),
                    line.gstUnitAmount(),
                    line.finalUnitPrice(),
                    line.totalProductCost()));
        }
        return entity;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_EXPLANATION_LENGTH ? value : value.substring(0, MAX_EXPLANATION_LENGTH);
    }
}
