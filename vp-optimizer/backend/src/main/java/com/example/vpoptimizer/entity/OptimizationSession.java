package com.example.vpoptimizer.entity;

import com.example.vpoptimizer.optimization.model.ToleranceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One optimization run, persisted together with its inputs, the product selection snapshot and the
 * ranked results, so that history stays reproducible even if the catalogue changes later.
 */
@Entity
@Table(name = "optimization_sessions")
public class OptimizationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OptimizationStatus status;

    @Column(name = "discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal discountPercent;

    @Column(name = "gst_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal gstPercent;

    @Column(name = "target_vp", nullable = false)
    private int targetVp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tolerance_type", nullable = false, length = 20)
    private ToleranceType toleranceType;

    @Column(name = "tolerance_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal toleranceValue;

    @Column(name = "min_vp", nullable = false)
    private int minVp;

    @Column(name = "max_vp", nullable = false)
    private int maxVp;

    @Column(name = "result_limit", nullable = false)
    private int resultLimit;

    @Column(name = "selected_product_count", nullable = false)
    private int selectedProductCount;

    @Column(name = "solution_count", nullable = false)
    private int solutionCount;

    @Column(name = "alternative_count", nullable = false)
    private int alternativeCount;

    @Column(name = "dp_capacity")
    private Integer dpCapacity;

    @Column(name = "evaluated_states")
    private Long evaluatedStates;

    @Column(name = "engine_millis")
    private Long engineMillis;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OptimizationSessionProduct> selections = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OptimizationSolution> solutions = new ArrayList<>();

    protected OptimizationSession() {
        // for JPA
    }

    public OptimizationSession(BigDecimal discountPercent,
                               BigDecimal gstPercent,
                               int targetVp,
                               ToleranceType toleranceType,
                               BigDecimal toleranceValue,
                               int minVp,
                               int maxVp,
                               int resultLimit) {
        this.discountPercent = discountPercent;
        this.gstPercent = gstPercent;
        this.targetVp = targetVp;
        this.toleranceType = toleranceType;
        this.toleranceValue = toleranceValue;
        this.minVp = minVp;
        this.maxVp = maxVp;
        this.resultLimit = resultLimit;
        this.status = OptimizationStatus.NO_VALID_SOLUTION;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Adds a selection snapshot line and keeps both sides of the relation in sync. */
    public void addSelection(OptimizationSessionProduct selection) {
        selection.setSession(this);
        selections.add(selection);
        selectedProductCount = selections.size();
    }

    /** Adds a ranked solution (or closest alternative) to the session. */
    public void addSolution(OptimizationSolution solution) {
        solution.setSession(this);
        solutions.add(solution);
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

    public OptimizationStatus getStatus() {
        return status;
    }

    public void setStatus(OptimizationStatus status) {
        this.status = status;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getGstPercent() {
        return gstPercent;
    }

    public int getTargetVp() {
        return targetVp;
    }

    public ToleranceType getToleranceType() {
        return toleranceType;
    }

    public BigDecimal getToleranceValue() {
        return toleranceValue;
    }

    public int getMinVp() {
        return minVp;
    }

    public int getMaxVp() {
        return maxVp;
    }

    public int getResultLimit() {
        return resultLimit;
    }

    public int getSelectedProductCount() {
        return selectedProductCount;
    }

    public int getSolutionCount() {
        return solutionCount;
    }

    public void setSolutionCount(int solutionCount) {
        this.solutionCount = solutionCount;
    }

    public int getAlternativeCount() {
        return alternativeCount;
    }

    public void setAlternativeCount(int alternativeCount) {
        this.alternativeCount = alternativeCount;
    }

    public Integer getDpCapacity() {
        return dpCapacity;
    }

    public void setDpCapacity(Integer dpCapacity) {
        this.dpCapacity = dpCapacity;
    }

    public Long getEvaluatedStates() {
        return evaluatedStates;
    }

    public void setEvaluatedStates(Long evaluatedStates) {
        this.evaluatedStates = evaluatedStates;
    }

    public Long getEngineMillis() {
        return engineMillis;
    }

    public void setEngineMillis(Long engineMillis) {
        this.engineMillis = engineMillis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OptimizationSessionProduct> getSelections() {
        return selections;
    }

    public List<OptimizationSolution> getSolutions() {
        return solutions;
    }
}
