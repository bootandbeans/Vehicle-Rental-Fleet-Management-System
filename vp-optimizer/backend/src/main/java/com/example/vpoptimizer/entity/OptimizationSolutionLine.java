package com.example.vpoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** One product line of a persisted solution, including the pricing snapshot used at the time. */
@Entity
@Table(name = "optimization_result_lines")
public class OptimizationSolutionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "optimization_result_id", nullable = false)
    private OptimizationSolution solution;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "category_name", length = 80)
    private String categoryName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "mrp", nullable = false, precision = 14, scale = 2)
    private BigDecimal mrp;

    @Column(name = "volume_point", nullable = false)
    private int volumePoint;

    @Column(name = "total_vp", nullable = false)
    private int totalVp;

    @Column(name = "discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal discountPercent;

    @Column(name = "gst_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal gstPercent;

    @Column(name = "discounted_unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountedUnitPrice;

    @Column(name = "gst_unit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal gstUnitAmount;

    @Column(name = "final_unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal finalUnitPrice;

    @Column(name = "total_product_cost", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalProductCost;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    protected OptimizationSolutionLine() {
        // for JPA
    }

    public OptimizationSolutionLine(Long productId,
                                    String productName,
                                    String sku,
                                    String categoryName,
                                    boolean required,
                                    int quantity,
                                    BigDecimal mrp,
                                    int volumePoint,
                                    int totalVp,
                                    BigDecimal discountPercent,
                                    BigDecimal gstPercent,
                                    BigDecimal discountedUnitPrice,
                                    BigDecimal gstUnitAmount,
                                    BigDecimal finalUnitPrice,
                                    BigDecimal totalProductCost) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.categoryName = categoryName;
        this.required = required;
        this.quantity = quantity;
        this.mrp = mrp;
        this.volumePoint = volumePoint;
        this.totalVp = totalVp;
        this.discountPercent = discountPercent;
        this.gstPercent = gstPercent;
        this.discountedUnitPrice = discountedUnitPrice;
        this.gstUnitAmount = gstUnitAmount;
        this.finalUnitPrice = finalUnitPrice;
        this.totalProductCost = totalProductCost;
    }

    public Long getId() {
        return id;
    }

    public OptimizationSolution getSolution() {
        return solution;
    }

    public void setSolution(OptimizationSolution solution) {
        this.solution = solution;
    }

    public Long getProductId() {
        return productId;
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

    public boolean isRequired() {
        return required;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public int getVolumePoint() {
        return volumePoint;
    }

    public int getTotalVp() {
        return totalVp;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getGstPercent() {
        return gstPercent;
    }

    public BigDecimal getDiscountedUnitPrice() {
        return discountedUnitPrice;
    }

    public BigDecimal getGstUnitAmount() {
        return gstUnitAmount;
    }

    public BigDecimal getFinalUnitPrice() {
        return finalUnitPrice;
    }

    public BigDecimal getTotalProductCost() {
        return totalProductCost;
    }
}
