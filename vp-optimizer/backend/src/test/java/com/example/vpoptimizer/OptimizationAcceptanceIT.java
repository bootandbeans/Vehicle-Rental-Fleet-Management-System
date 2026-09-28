package com.example.vpoptimizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end acceptance tests of the specification scenario, running against a real Spring context
 * and a database created by the Flyway migrations.
 *
 * <p>Scenario: products A (2000 / 50 VP), B (3000 / 100 VP), C (1500 / 40 VP) and D (5000 / 200 VP);
 * the user selects A, B and D but not C, with 20% discount, 18% GST, a 500 VP target and 10%
 * tolerance.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OptimizationAcceptanceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private long createProduct(String name, String sku, String mrp, int volumePoint,
                               Integer minQuantity, Integer maxQuantity, boolean active) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("sku", sku);
        payload.put("description", "Integration test product");
        payload.put("mrp", new BigDecimal(mrp));
        payload.put("volumePoint", volumePoint);
        payload.put("minQuantity", minQuantity);
        payload.put("maxQuantity", maxQuantity);
        payload.put("active", active);

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode runOptimization(Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static Map<String, Object> optimizationPayload(Object productIds, Object requiredIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productIds", productIds);
        payload.put("requiredProductIds", requiredIds);
        payload.put("discountPercent", new BigDecimal("20"));
        payload.put("gstPercent", new BigDecimal("18"));
        payload.put("targetVp", 500);
        payload.put("toleranceType", "PERCENTAGE");
        payload.put("toleranceValue", new BigDecimal("10"));
        payload.put("resultLimit", 3);
        payload.put("name", "Acceptance run");
        return payload;
    }

    private static Map<String, Object> optimizationPayload(Object productIds, Object requiredIds,
                                                          String discount, String gst, int targetVp,
                                                          String toleranceType, String tolerance) {
        Map<String, Object> payload = optimizationPayload(productIds, requiredIds);
        payload.put("discountPercent", new BigDecimal(discount));
        payload.put("gstPercent", new BigDecimal(gst));
        payload.put("targetVp", targetVp);
        payload.put("toleranceType", toleranceType);
        payload.put("toleranceValue", new BigDecimal(tolerance));
        return payload;
    }

    // ------------------------------------------------------------------
    // the acceptance scenario
    // ------------------------------------------------------------------

    @Test
    @DisplayName("selects A, B, D (not C) and returns the three best combinations for 500 VP")
    void acceptanceScenario() throws Exception {
        long productA = createProduct("IT Protein Powder", "IT-A", "2000", 50, null, null, true);
        long productB = createProduct("IT Mass Gainer", "IT-B", "3000", 100, null, null, true);
        long productC = createProduct("IT Omega 3", "IT-C", "1500", 40, null, null, true);
        long productD = createProduct("IT Pre Workout", "IT-D", "5000", 200, null, null, true);

        JsonNode body = runOptimization(optimizationPayload(
                java.util.List.of(productA, productB, productD), java.util.List.of()));

        // 1. the normalized window
        assertThat(body.get("minimumVp").asInt()).isEqualTo(450);
        assertThat(body.get("maximumVp").asInt()).isEqualTo(550);
        assertThat(body.get("status").asText()).isEqualTo("COMPLETED");

        // 2. exactly three ranked solutions
        JsonNode solutions = body.get("solutions");
        assertThat(solutions).hasSize(3);
        assertThat(body.get("closestAlternatives")).isEmpty();

        Set<Long> selectedIds = Set.of(productA, productB, productD);
        Set<String> canonicalKeys = new HashSet<>();
        int expectedRank = 1;
        for (JsonNode solution : solutions) {
            assertThat(solution.get("alternative").asBoolean()).isFalse();
            assertThat(solution.get("withinRange").asBoolean()).isTrue();
            assertThat(solution.get("solutionRank").asInt()).isEqualTo(expectedRank++);

            long totalVp = solution.get("totalVp").asLong();
            assertThat(totalVp).isBetween(450L, 550L);
            assertThat(solution.get("vpDifference").asLong()).isEqualTo(Math.abs(totalVp - 500));

            // 3. C is never used and nothing outside the selection appears
            for (JsonNode line : solution.get("products")) {
                long productId = line.get("productId").asLong();
                assertThat(selectedIds).contains(productId);
                assertThat(productId).isNotEqualTo(productC);
            }

            // 4. duplicate combinations are removed
            assertThat(canonicalKeys.add(solution.get("canonicalKey").asText())).isTrue();

            // 5. pricing consistency: MRP - discount + GST = final payable
            BigDecimal totalMrp = solution.get("totalMrp").decimalValue();
            BigDecimal totalDiscount = solution.get("totalDiscount").decimalValue();
            BigDecimal totalGst = solution.get("totalGst").decimalValue();
            BigDecimal payable = solution.get("finalPayableAmount").decimalValue();
            assertThat(totalMrp.subtract(totalDiscount).add(totalGst)).isEqualByComparingTo(payable);

            // 6. every line is priced with the documented formula
            BigDecimal lineSum = BigDecimal.ZERO;
            for (JsonNode line : solution.get("products")) {
                BigDecimal discounted = line.get("discountedUnitPrice").decimalValue();
                BigDecimal gst = line.get("gstUnitAmount").decimalValue();
                BigDecimal unit = line.get("finalUnitPrice").decimalValue();
                assertThat(discounted.add(gst)).isEqualByComparingTo(unit);
                assertThat(line.get("totalProductCost").decimalValue())
                        .isEqualByComparingTo(unit.multiply(BigDecimal.valueOf(line.get("quantity").asInt())));
                lineSum = lineSum.add(line.get("totalProductCost").decimalValue());
            }
            assertThat(lineSum).isEqualByComparingTo(payable);

            // 7. cost per VP is derived, never divided by zero
            if (totalVp > 0) {
                assertThat(solution.get("costPerVp").decimalValue())
                        .isEqualByComparingTo(payable.divide(BigDecimal.valueOf(totalVp), 4,
                                java.math.RoundingMode.HALF_UP).setScale(4,
                                java.math.RoundingMode.HALF_UP));
            }
            assertThat(solution.get("explanation").asText()).isNotBlank();
        }

        // 8. the ranking is the documented lexicographic one
        JsonNode best = solutions.get(0);
        assertThat(best.get("totalVp").asLong()).isEqualTo(500L);
        assertThat(best.get("vpDifference").asLong()).isZero();
        assertThat(best.get("exactTarget").asBoolean()).isTrue();
        assertThat(best.get("finalPayableAmount").decimalValue()).isEqualByComparingTo("12272.00");
        assertThat(best.get("totalMrp").decimalValue()).isEqualByComparingTo("13000.00");
        assertThat(best.get("totalDiscount").decimalValue()).isEqualByComparingTo("2600.00");
        assertThat(best.get("totalGst").decimalValue()).isEqualByComparingTo("1872.00");
        assertThat(best.get("numberOfUniqueProducts").asInt()).isEqualTo(2);
        assertThat(best.get("totalQuantity").asInt()).isEqualTo(3);

        JsonNode lineB = best.get("products").get(0);
        assertThat(lineB.get("productId").asLong()).isEqualTo(productB);
        assertThat(lineB.get("quantity").asInt()).isEqualTo(1);
        assertThat(lineB.get("discountedUnitPrice").decimalValue()).isEqualByComparingTo("2400.00");
        assertThat(lineB.get("gstUnitAmount").decimalValue()).isEqualByComparingTo("432.00");
        assertThat(lineB.get("finalUnitPrice").decimalValue()).isEqualByComparingTo("2832.00");

        JsonNode lineD = best.get("products").get(1);
        assertThat(lineD.get("productId").asLong()).isEqualTo(productD);
        assertThat(lineD.get("quantity").asInt()).isEqualTo(2);
        assertThat(lineD.get("finalUnitPrice").decimalValue()).isEqualByComparingTo("4720.00");
        assertThat(lineD.get("totalProductCost").decimalValue()).isEqualByComparingTo("9440.00");
        assertThat(lineD.get("totalVp").asLong()).isEqualTo(400);

        assertThat(solutions.get(1).get("totalVp").asLong()).isEqualTo(450L);
        assertThat(solutions.get(1).get("finalPayableAmount").decimalValue()).isEqualByComparingTo("11328.00");
        assertThat(solutions.get(2).get("totalVp").asLong()).isEqualTo(550L);

        // 9. the session is stored and reopens with identical numbers
        long sessionId = body.get("sessionId").asLong();
        JsonNode reopened = objectMapper.readTree(mockMvc.perform(get("/api/optimizations/" + sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(reopened.get("solutions")).hasSize(3);
        assertThat(reopened.get("solutions").get(0).get("canonicalKey").asText())
                .isEqualTo(best.get("canonicalKey").asText());
        assertThat(reopened.get("solutions").get(0).get("finalPayableAmount").decimalValue())
                .isEqualByComparingTo("12272.00");
        assertThat(reopened.get("selectedProducts")).hasSize(3);
        assertThat(reopened.get("diagnostics").get("selectedProductCount").asInt()).isEqualTo(3);

        // 10. the session shows up in the history
        mockMvc.perform(get("/api/optimizations").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetVp").value(500));
    }

    @Test
    @DisplayName("a cheaper product outside the selection never influences the result")
    void ignoresExcludedProductsEvenWhenTheyAreCheaper() throws Exception {
        long expensive = createProduct("IT Expensive", "IT-EXP", "10000", 200, null, null, true);
        long cheapExcluded = createProduct("IT Cheap But Excluded", "IT-CHEAP", "100", 200, null, null, true);

        JsonNode body = runOptimization(optimizationPayload(java.util.List.of(expensive), java.util.List.of(),
                "0", "0", 200, "ABSOLUTE", "0"));

        assertThat(body.get("solutions")).hasSize(1);
        JsonNode solution = body.get("solutions").get(0);
        assertThat(solution.get("finalPayableAmount").decimalValue()).isEqualByComparingTo("10000.00");
        assertThat(solution.get("products").get(0).get("productId").asLong()).isEqualTo(expensive);
        assertThat(solution.get("products").get(0).get("productId").asLong()).isNotEqualTo(cheapExcluded);
    }

    @Test
    @DisplayName("no valid combination is reported honestly, with labelled closest alternatives")
    void reportsNoValidSolutionWithAlternatives() throws Exception {
        long product = createProduct("IT Hundred", "IT-100", "1000", 100, null, null, true);

        JsonNode body = runOptimization(optimizationPayload(java.util.List.of(product), java.util.List.of(),
                "0", "0", 550, "ABSOLUTE", "0"));

        assertThat(body.get("solutions")).isEmpty();
        assertThat(body.get("status").asText()).isEqualTo("NO_VALID_SOLUTION");
        assertThat(body.get("message").asText()).contains("No valid combination found");

        JsonNode alternatives = body.get("closestAlternatives");
        assertThat(alternatives).isNotEmpty();
        for (JsonNode alternative : alternatives) {
            assertThat(alternative.get("withinRange").asBoolean()).isFalse();
            assertThat(alternative.get("alternative").asBoolean()).isTrue();
            assertThat(alternative.get("explanation").asText()).startsWith("Outside the requested range");
        }
        assertThat(alternatives.get(0).get("totalVp").asLong()).isEqualTo(500L);
    }

    @Test
    @DisplayName("REQUIRED products are part of every returned combination")
    void requiredProductsAlwaysAppear() throws Exception {
        long required = createProduct("IT Required", "IT-REQ", "1000", 50, 2, 5, true);
        long optional = createProduct("IT Optional", "IT-OPT", "500", 25, null, null, true);

        JsonNode body = runOptimization(optimizationPayload(java.util.List.of(required, optional),
                java.util.List.of(required), "0", "0", 100, "ABSOLUTE", "0"));

        assertThat(body.get("solutions")).isNotEmpty();
        for (JsonNode solution : body.get("solutions")) {
            long requiredQuantity = 0;
            for (JsonNode line : solution.get("products")) {
                if (line.get("productId").asLong() == required) {
                    requiredQuantity = line.get("quantity").asLong();
                    assertThat(line.get("required").asBoolean()).isTrue();
                }
            }
            assertThat(requiredQuantity).isGreaterThanOrEqualTo(2L);
        }
    }

    @Test
    @DisplayName("catalogue quantity limits are respected")
    void respectsCatalogueQuantityLimits() throws Exception {
        long limited = createProduct("IT Limited", "IT-LIM", "100", 50, null, 3, true);

        JsonNode body = runOptimization(optimizationPayload(java.util.List.of(limited), java.util.List.of(),
                "0", "0", 200, "ABSOLUTE", "0"));

        assertThat(body.get("solutions")).isEmpty();
        assertThat(body.get("closestAlternatives").get(0).get("totalVp").asLong()).isEqualTo(150L);

        JsonNode reachable = runOptimization(optimizationPayload(java.util.List.of(limited), java.util.List.of(),
                "0", "0", 150, "ABSOLUTE", "0"));
        assertThat(reachable.get("solutions").get(0).get("products").get(0).get("quantity").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("history stays reproducible when the catalogue changes later")
    void historyIsReproducibleAfterCatalogueChanges() throws Exception {
        long product = createProduct("IT Reproducible", "IT-REPRO", "1000", 100, null, null, true);

        JsonNode firstRun = runOptimization(optimizationPayload(java.util.List.of(product), java.util.List.of(),
                "0", "0", 100, "ABSOLUTE", "0"));
        long sessionId = firstRun.get("sessionId").asLong();
        BigDecimal originalPayable = firstRun.get("solutions").get(0).get("finalPayableAmount").decimalValue();

        // The MRP changes after the optimization was saved
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "IT Reproducible");
        update.put("sku", "IT-REPRO");
        update.put("mrp", new BigDecimal("2500"));
        update.put("volumePoint", 100);
        update.put("active", true);
        mockMvc.perform(put("/api/products/" + product)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        JsonNode reopened = objectMapper.readTree(mockMvc.perform(get("/api/optimizations/" + sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(reopened.get("solutions").get(0).get("finalPayableAmount").decimalValue())
                .isEqualByComparingTo(originalPayable);
        assertThat(reopened.get("solutions").get(0).get("totalMrp").decimalValue())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("the pricing preview uses the backend formula (including 0% GST)")
    void pricingPreviewUsesTheBackendFormula() throws Exception {
        long product = createProduct("IT Priced", "IT-PRICE", "1000", 10, null, null, true);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productIds", java.util.List.of(product));
        payload.put("requiredProductIds", java.util.List.of());
        payload.put("discountPercent", new BigDecimal("20"));
        payload.put("gstPercent", new BigDecimal("18"));

        JsonNode priced = objectMapper.readTree(mockMvc.perform(post("/api/pricing/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        JsonNode line = priced.get("products").get(0);
        assertThat(line.get("discountedPrice").decimalValue()).isEqualByComparingTo("800.00");
        assertThat(line.get("gstAmount").decimalValue()).isEqualByComparingTo("144.00");
        assertThat(line.get("finalUnitPrice").decimalValue()).isEqualByComparingTo("944.00");
        assertThat(line.get("effectiveMaxQuantity").asInt()).isEqualTo(10);

        payload.put("gstPercent", BigDecimal.ZERO);
        JsonNode zeroGst = objectMapper.readTree(mockMvc.perform(post("/api/pricing/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(zeroGst.get("products").get(0).get("finalUnitPrice").decimalValue())
                .isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("error responses carry stable codes and never leak stack traces")
    void errorResponsesAreStable() throws Exception {
        // no products selected
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optimizationPayload(
                                java.util.List.of(), java.util.List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NO_PRODUCTS_SELECTED"));

        // unknown product
        Map<String, Object> unknown = optimizationPayload(java.util.List.of(999_999L), java.util.List.of());
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));

        // negative discount
        Map<String, Object> negativeDiscount = optimizationPayload(java.util.List.of(1L), java.util.List.of());
        negativeDiscount.put("discountPercent", new BigDecimal("-5"));
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeDiscount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // negative target VP
        Map<String, Object> negativeTarget = optimizationPayload(java.util.List.of(1L), java.util.List.of());
        negativeTarget.put("targetVp", -10);
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeTarget)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // malformed body
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        // unknown session
        mockMvc.perform(get("/api/optimizations/424242"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OPTIMIZATION_SESSION_NOT_FOUND"));
    }

    @Test
    @DisplayName("a target VP beyond the supported capacity is rejected with a clear limit error")
    void rejectsUnsupportedVpWindows() throws Exception {
        long product = createProduct("IT Big", "IT-BIG", "1000", 100, null, null, true);

        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optimizationPayload(
                                java.util.List.of(product), java.util.List.of(),
                                "0", "0", 500_000, "ABSOLUTE", "0"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ENGINE_LIMIT_EXCEEDED"));
    }
}
