package com.example.vpoptimizer.entity;

import com.example.vpoptimizer.optimization.model.SelectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A product that took part in an optimization session, with the values that were used at the time.
 *
 * <p>Excluded products are not stored: they simply do not appear. The snapshot columns keep old
 * sessions readable after the catalogue changes.</p>
 */
@Entity
@Table(name = "optimization_session_products")
public class OptimizationSessionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "optimization_session_id", nullable = false)
    private OptimizationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "category_name", length = 80)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, length = 20)
    private SelectionType selectionType;

    @Column(name = "mrp", nullable = false, precision = 14, scale = 2)
    private BigDecimal mrp;

    @Column(name = "volume_point", nullable = false)
    private int volumePoint;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;

    protected OptimizationSessionProduct() {
        // for JPA
    }

    public OptimizationSessionProduct(Product product,
                                      String productName,
                                      String sku,
                                      String categoryName,
                                      SelectionType selectionType,
                                      BigDecimal mrp,
                                      int volumePoint,
                                      int minQuantity,
                                      int maxQuantity) {
        this.product = product;
        this.productName = productName;
        this.sku = sku;
        this.categoryName = categoryName;
        this.selectionType = selectionType;
        this.mrp = mrp;
        this.volumePoint = volumePoint;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
    }

    public Long getId() {
        return id;
    }

    public OptimizationSession getSession() {
        return session;
    }

    public void setSession(OptimizationSession session) {
        this.session = session;
    }

    public Product getProduct() {
        return product;
    }

    public Long getProductId() {
        return product == null ? null : product.getId();
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public int getVolumePoint() {
        return volumePoint;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }
}
