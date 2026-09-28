package com.example.vpoptimizer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A persisted solution (or closest alternative) of a session.
 *
 * <p>Every monetary and VP figure is stored as a snapshot: historical results never change when
 * the underlying product is edited.</p>
 */
@Entity
@Table(name = "optimization_results")
public class OptimizationSolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "optimization_session_id", nullable = false)
    private OptimizationSession session;

    @Column(name = "solution_rank", nullable = false)
    private int solutionRank;

    @Column(name = "is_alternative", nullable = false)
    private boolean alternative;

    @Column(name = "within_range", nullable = false)
    private boolean withinRange;

    @Column(name = "exact_target", nullable = false)
    private boolean exactTarget;

    @Column(name = "canonical_key", nullable = false, length = 1000)
    private String canonicalKey;

    @Column(name = "total_vp", nullable = false)
    private int totalVp;

    @Column(name = "vp_difference", nullable = false)
    private int vpDifference;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "number_of_unique_products", nullable = false)
    private int numberOfUniqueProducts;

    @Column(name = "total_mrp", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalMrp;

    @Column(name = "total_discount", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalDiscount;

    @Column(name = "total_gst", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalGst;

    @Column(name = "final_payable_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal finalPayableAmount;

    @Column(name = "cost_per_vp", precision = 16, scale = 4)
    private BigDecimal costPerVp;

    @Column(name = "explanation", nullable = false, length = 1000)
    private String explanation;

    @OneToMany(mappedBy = "solution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OptimizationSolutionLine> lines = new ArrayList<>();

    protected OptimizationSolution() {
        // for JPA
    }

    public OptimizationSolution(int solutionRank,
                                boolean alternative,
                                boolean withinRange,
                                boolean exactTarget,
                                String canonicalKey,
                                int totalVp,
                                int vpDifference,
                                int totalQuantity,
                                int numberOfUniqueProducts,
                                BigDecimal totalMrp,
                                BigDecimal totalDiscount,
                                BigDecimal totalGst,
                                BigDecimal finalPayableAmount,
                                BigDecimal costPerVp,
                                String explanation) {
        this.solutionRank = solutionRank;
        this.alternative = alternative;
        this.withinRange = withinRange;
        this.exactTarget = exactTarget;
        this.canonicalKey = canonicalKey;
        this.totalVp = totalVp;
        this.vpDifference = vpDifference;
        this.totalQuantity = totalQuantity;
        this.numberOfUniqueProducts = numberOfUniqueProducts;
        this.totalMrp = totalMrp;
        this.totalDiscount = totalDiscount;
        this.totalGst = totalGst;
        this.finalPayableAmount = finalPayableAmount;
        this.costPerVp = costPerVp;
        this.explanation = explanation;
    }

    public void addLine(OptimizationSolutionLine line) {
        line.setSolution(this);
        lines.add(line);
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

    public int getSolutionRank() {
        return solutionRank;
    }

    public boolean isAlternative() {
        return alternative;
    }

    public boolean isWithinRange() {
        return withinRange;
    }

    public boolean isExactTarget() {
        return exactTarget;
    }

    public String getCanonicalKey() {
        return canonicalKey;
    }

    public int getTotalVp() {
        return totalVp;
    }

    public int getVpDifference() {
        return vpDifference;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getNumberOfUniqueProducts() {
        return numberOfUniqueProducts;
    }

    public BigDecimal getTotalMrp() {
        return totalMrp;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public BigDecimal getTotalGst() {
        return totalGst;
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public BigDecimal getCostPerVp() {
        return costPerVp;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<OptimizationSolutionLine> getLines() {
        return lines;
    }
}
