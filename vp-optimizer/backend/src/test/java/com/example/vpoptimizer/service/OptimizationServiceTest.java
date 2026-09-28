package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.OptimizationRunRequest;
import com.example.vpoptimizer.dto.OptimizationRunResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.entity.OptimizationStatus;
import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.mapper.OptimizationResponseMapper;
import com.example.vpoptimizer.mapper.ProductOptionMapper;
import com.example.vpoptimizer.optimization.engine.OptimizationEngine;
import com.example.vpoptimizer.optimization.model.OptimizationRequest;
import com.example.vpoptimizer.optimization.model.OptimizationResult;
import com.example.vpoptimizer.optimization.model.PricingContext;
import com.example.vpoptimizer.optimization.model.SelectionType;
import com.example.vpoptimizer.optimization.model.ToleranceType;
import com.example.vpoptimizer.config.OptimizationProperties;
import com.example.vpoptimizer.repository.OptimizationSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orchestration: selection -> engine -> snapshot -> response. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OptimizationServiceTest {

    @Mock
    private OptimizationEngine optimizationEngine;

    @Mock
    private ProductService productService;

    @Mock
    private ProductOptionMapper productOptionMapper;

    @Mock
    private OptimizationResponseMapper responseMapper;

    @Mock
    private OptimizationSessionRepository sessionRepository;

    @Mock
    private OptimizationProperties optimizationProperties;

    @InjectMocks
    private OptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        when(optimizationProperties.toLimits()).thenReturn(
                com.example.vpoptimizer.optimization.model.OptimizationLimits.DEFAULT);
        when(sessionRepository.save(any(OptimizationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toRunResponse(any(OptimizationSession.class)))
                .thenReturn(mock(OptimizationRunResponse.class));
    }

    private static Product product(long id, String name) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getMrp()).thenReturn(new BigDecimal("2000.00"));
        when(product.getVolumePoint()).thenReturn(50);
        when(product.isActive()).thenReturn(true);
        return product;
    }

    private static OptimizationRunRequest request(List<Long> productIds) {
        return new OptimizationRunRequest(productIds, List.of(), new BigDecimal("20"), new BigDecimal("18"),
                500, ToleranceType.PERCENTAGE, new BigDecimal("10"), 3, "Monthly target");
    }

    private static OptimizationResult engineResult() {
        return new OptimizationResult(500, 450, 550, new BigDecimal("20"), new BigDecimal("18"),
                ToleranceType.PERCENTAGE, new BigDecimal("10"), 3, List.of(), List.of(),
                "No valid combination found within the requested VP range (450 - 550 VP).",
                new com.example.vpoptimizer.optimization.model.OptimizationDiagnostics(2, 550, 1200, 0, 3));
    }

    @Test
    @DisplayName("only the selected products are loaded and handed to the engine")
    void usesOnlyTheSelectedProducts() {
        List<Product> products = List.of(product(1L, "A"), product(2L, "B"));
        when(productService.requireSelectableProducts(List.of(1L, 2L))).thenReturn(products);
        when(productOptionMapper.toOption(any(Product.class), anyBoolean()))
                .thenAnswer(invocation -> com.example.vpoptimizer.optimization.model.ProductOption
                        .allowed(invocation.getArgument(0, Product.class).getId(),
                                invocation.getArgument(0, Product.class).getName(),
                                new BigDecimal("2000"), 50));
        when(optimizationEngine.optimize(any(OptimizationRequest.class))).thenReturn(engineResult());

        optimizationService.run(request(List.of(1L, 2L)));

        verify(productService).requireSelectableProducts(List.of(1L, 2L));
        ArgumentCaptor<OptimizationRequest> captor = ArgumentCaptor.forClass(OptimizationRequest.class);
        verify(optimizationEngine).optimize(captor.capture());
        assertThat(captor.getValue().products()).extracting(
                com.example.vpoptimizer.optimization.model.ProductOption::id).containsExactly(1L, 2L);
        assertThat(captor.getValue().targetVp()).isEqualTo(500);
        assertThat(captor.getValue().effectiveLimit()).isEqualTo(3);
    }

    @Test
    @DisplayName("required products reach the engine as REQUIRED")
    void marksRequiredProducts() {
        List<Product> products = List.of(product(1L, "A"), product(2L, "B"));
        when(productService.requireSelectableProducts(List.of(1L, 2L))).thenReturn(products);
        when(productOptionMapper.toOption(any(Product.class), anyBoolean()))
                .thenAnswer(invocation -> com.example.vpoptimizer.optimization.model.ProductOption
                        .allowed(invocation.getArgument(0, Product.class).getId(),
                                invocation.getArgument(0, Product.class).getName(),
                                new BigDecimal("2000"), 50));
        when(optimizationEngine.optimize(any(OptimizationRequest.class))).thenReturn(engineResult());

        optimizationService.run(new OptimizationRunRequest(List.of(1L, 2L), List.of(1L), new BigDecimal("20"),
                new BigDecimal("18"), 500, ToleranceType.PERCENTAGE, new BigDecimal("10"), 3, null));

        verify(productOptionMapper).toOption(any(Product.class), org.mockito.ArgumentMatchers.eq(true));
        verify(productOptionMapper).toOption(any(Product.class), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    @DisplayName("the run is persisted as a snapshot with status, counts and diagnostics")
    void persistsTheSnapshot() {
        List<Product> products = List.of(product(1L, "A"), product(2L, "B"));
        when(productService.requireSelectableProducts(anyList())).thenReturn(products);
        when(productOptionMapper.toOption(any(Product.class), anyBoolean()))
                .thenAnswer(invocation -> com.example.vpoptimizer.optimization.model.ProductOption
                        .allowed(invocation.getArgument(0, Product.class).getId(),
                                invocation.getArgument(0, Product.class).getName(),
                                new BigDecimal("2000"), 50));
        when(optimizationEngine.optimize(any(OptimizationRequest.class))).thenReturn(engineResult());

        optimizationService.run(request(List.of(1L, 2L)));

        ArgumentCaptor<OptimizationSession> captor = ArgumentCaptor.forClass(OptimizationSession.class);
        verify(sessionRepository).save(captor.capture());
        OptimizationSession session = captor.getValue();
        assertThat(session.getStatus()).isEqualTo(OptimizationStatus.NO_VALID_SOLUTION);
        assertThat(session.getMinVp()).isEqualTo(450);
        assertThat(session.getMaxVp()).isEqualTo(550);
        assertThat(session.getSelections()).hasSize(2);
        assertThat(session.getSelections()).allSatisfy(selection ->
                assertThat(selection.getSelectionType()).isEqualTo(SelectionType.ALLOWED));
        assertThat(session.getSelectedProductCount()).isEqualTo(2);
        assertThat(session.getDpCapacity()).isEqualTo(550);
        assertThat(session.getMessage()).contains("No valid combination");
    }

    @Test
    @DisplayName("an empty selection is rejected before anything is loaded")
    void rejectsEmptySelection() {
        assertThatThrownBy(() -> optimizationService.run(request(List.of())))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.NO_PRODUCTS_SELECTED);
    }

    @Test
    @DisplayName("a required product that was not selected is an inconsistent selection")
    void rejectsRequiredProductOutsideTheSelection() {
        OptimizationRunRequest request = new OptimizationRunRequest(List.of(1L), List.of(2L),
                new BigDecimal("20"), new BigDecimal("18"), 500, ToleranceType.PERCENTAGE,
                new BigDecimal("10"), 3, null);

        assertThatThrownBy(() -> optimizationService.run(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.INVALID_SELECTION);
    }

    @Test
    @DisplayName("the pricing context carries the global discount and GST")
    void passesPricingContextToTheEngine() {
        when(productService.requireSelectableProducts(anyList())).thenReturn(List.of(product(1L, "A")));
        when(productOptionMapper.toOption(any(Product.class), anyBoolean()))
                .thenAnswer(invocation -> com.example.vpoptimizer.optimization.model.ProductOption
                        .allowed(1L, "A", new BigDecimal("2000"), 50));
        when(optimizationEngine.optimize(any(OptimizationRequest.class))).thenReturn(engineResult());

        optimizationService.run(request(List.of(1L)));

        ArgumentCaptor<OptimizationRequest> captor = ArgumentCaptor.forClass(OptimizationRequest.class);
        verify(optimizationEngine).optimize(captor.capture());
        PricingContext pricing = captor.getValue().pricing();
        assertThat(pricing.discountPercent()).isEqualByComparingTo("20");
        assertThat(pricing.gstPercent()).isEqualByComparingTo("18");
    }
}
