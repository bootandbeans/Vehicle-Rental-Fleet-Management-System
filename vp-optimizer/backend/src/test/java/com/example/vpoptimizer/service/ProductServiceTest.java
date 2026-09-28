package com.example.vpoptimizer.service;

import com.example.vpoptimizer.entity.Product;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import com.example.vpoptimizer.exception.ResourceNotFoundException;
import com.example.vpoptimizer.repository.OptimizationSessionProductRepository;
import com.example.vpoptimizer.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Product use cases: soft deletion, SKU uniqueness and selection loading. */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private OptimizationSessionProductRepository sessionProductRepository;

    @InjectMocks
    private ProductService productService;

    private static Product product(long id, String name, boolean active) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.isActive()).thenReturn(active);
        when(product.getMrp()).thenReturn(new BigDecimal("1000.00"));
        when(product.getVolumePoint()).thenReturn(50);
        return product;
    }

    @Test
    @DisplayName("deleting a product deactivates it instead of removing history relevant data")
    void softDeletesByDefault() {
        Product existing = product(1L, "Protein Powder", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.delete(1L, false);

        verify(existing).setActive(false);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("a permanent delete is refused while optimization history references the product")
    void refusesPermanentDeleteWhenReferenced() {
        Product existing = product(1L, "Protein Powder", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sessionProductRepository.existsByProductId(1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(1L, true))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.PRODUCT_IN_USE);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("a permanent delete succeeds when nothing references the product")
    void allowsPermanentDeleteWhenUnreferenced() {
        Product existing = product(1L, "Protein Powder", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sessionProductRepository.existsByProductId(1L)).thenReturn(false);

        productService.delete(1L, true);

        verify(productRepository).delete(existing);
    }

    @Test
    @DisplayName("a duplicate SKU is rejected")
    void rejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("P001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new com.example.vpoptimizer.dto.ProductRequest(
                "Protein", "P001", null, new BigDecimal("1000"), 50, null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("no selection means NO_PRODUCTS_SELECTED")
    void rejectsEmptySelection() {
        assertThatThrownBy(() -> productService.requireSelectableProducts(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.NO_PRODUCTS_SELECTED);
        assertThatThrownBy(() -> productService.requireSelectableProducts(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.NO_PRODUCTS_SELECTED);
    }

    @Test
    @DisplayName("only the requested products are loaded from the database")
    void loadsOnlySelectedProducts() {
        Product first = product(1L, "A", true);
        Product second = product(2L, "B", true);
        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(second, first));

        List<Product> loaded = productService.requireSelectableProducts(List.of(1L, 2L));

        assertThat(loaded).containsExactly(first, second);
        verify(productRepository).findAllByIdIn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("an unknown product id fails with PRODUCT_NOT_FOUND")
    void rejectsUnknownProducts() {
        when(productRepository.findAllByIdIn(List.of(1L, 9L))).thenReturn(List.of(product(1L, "A", true)));

        assertThatThrownBy(() -> productService.requireSelectableProducts(List.of(1L, 9L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("an inactive product cannot participate in an optimization")
    void rejectsInactiveProducts() {
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product(1L, "A", false)));

        assertThatThrownBy(() -> productService.requireSelectableProducts(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.PRODUCT_INACTIVE);
    }

    @Test
    @DisplayName("activating and deactivating a product updates the flag")
    void togglesActiveFlag() {
        Product existing = product(1L, "A", false);
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(existing));

        productService.setActive(1L, true);

        verify(existing).setActive(true);
    }
}
