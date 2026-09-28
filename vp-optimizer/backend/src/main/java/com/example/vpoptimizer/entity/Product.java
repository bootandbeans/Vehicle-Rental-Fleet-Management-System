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

/**
 * Catalogue product.
 *
 * <p>Products are never hard deleted once they appear in optimization history: {@code active} is
 * used to take a product out of the selectable catalogue (soft deletion).</p>
 */
@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "sku", length = 64, unique = true)
    private String sku;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "mrp", nullable = false, precision = 14, scale = 2)
    private BigDecimal mrp;

    @Column(name = "volume_point", nullable = false)
    private int volumePoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Catalogue minimum quantity; only enforced when the product is REQUIRED in a session. */
    @Column(name = "min_quantity")
    private Integer minQuantity;

    /** Catalogue maximum quantity; {@code null} means "unbounded, subject to engine limits". */
    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Product() {
        // for JPA
    }

    public Product(String name, BigDecimal mrp, int volumePoint) {
        this.name = name;
        this.mrp = mrp;
        this.volumePoint = volumePoint;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public int getVolumePoint() {
        return volumePoint;
    }

    public void setVolumePoint(int volumePoint) {
        this.volumePoint = volumePoint;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
