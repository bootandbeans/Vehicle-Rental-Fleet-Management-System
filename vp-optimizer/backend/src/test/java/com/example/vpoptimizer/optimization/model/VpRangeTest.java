package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Normalization of the target VP tolerance into an explicit window. */
class VpRangeTest {

    @Test
    @DisplayName("500 VP with 10% tolerance becomes 450 - 550")
    void normalizesPercentageTolerance() {
        VpRange range = VpRange.of(500, ToleranceType.PERCENTAGE, new BigDecimal("10"));

        assertThat(range.minimumVp()).isEqualTo(450);
        assertThat(range.maximumVp()).isEqualTo(550);
        assertThat(range.width()).isEqualTo(100);
    }

    @Test
    @DisplayName("500 VP with an absolute tolerance of 50 becomes 450 - 550")
    void normalizesAbsoluteTolerance() {
        VpRange range = VpRange.of(500, ToleranceType.ABSOLUTE, new BigDecimal("50"));

        assertThat(range.minimumVp()).isEqualTo(450);
        assertThat(range.maximumVp()).isEqualTo(550);
    }

    @Test
    @DisplayName("the percentage delta is rounded HALF_UP (333 +/- 10% -> 300 - 366)")
    void roundsPercentageDelta() {
        VpRange range = VpRange.of(333, ToleranceType.PERCENTAGE, new BigDecimal("10"));

        assertThat(range.minimumVp()).isEqualTo(300);
        assertThat(range.maximumVp()).isEqualTo(366);
    }

    @Test
    @DisplayName("the minimum never becomes negative")
    void clampsAtZero() {
        VpRange range = VpRange.of(10, ToleranceType.ABSOLUTE, new BigDecimal("50"));

        assertThat(range.minimumVp()).isZero();
        assertThat(range.maximumVp()).isEqualTo(60);
    }

    @Test
    @DisplayName("zero target with zero tolerance is the exact window [0, 0]")
    void supportsZeroTarget() {
        VpRange range = VpRange.exact(0);

        assertThat(range.minimumVp()).isZero();
        assertThat(range.maximumVp()).isZero();
        assertThat(range.contains(0)).isTrue();
    }

    @Test
    @DisplayName("contains and distance are consistent")
    void reportsDistanceToRange() {
        VpRange range = VpRange.of(500, ToleranceType.PERCENTAGE, new BigDecimal("10"));

        assertThat(range.contains(450)).isTrue();
        assertThat(range.contains(550)).isTrue();
        assertThat(range.distanceTo(500)).isZero();
        assertThat(range.distanceTo(420)).isEqualTo(30);
        assertThat(range.distanceTo(600)).isEqualTo(50);
    }

    @Test
    @DisplayName("negative tolerances and percentage tolerances above 100% are rejected")
    void rejectsInvalidTolerances() {
        assertThatThrownBy(() -> VpRange.of(500, ToleranceType.ABSOLUTE, new BigDecimal("-1")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.INVALID_TOLERANCE);
        assertThatThrownBy(() -> VpRange.of(500, ToleranceType.PERCENTAGE, new BigDecimal("101")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.INVALID_TOLERANCE);
    }

    @Test
    @DisplayName("a negative target VP is rejected")
    void rejectsNegativeTarget() {
        assertThatThrownBy(() -> VpRange.of(-5, ToleranceType.ABSOLUTE, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.INVALID_TARGET_VP);
    }
}
