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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catalogue behaviour: CRUD, filtering, soft deletion, guards and categories. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductCatalogueIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long createCategory(String name) throws Exception {
        var result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"IT category\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Map<String, Object> productPayload(String name, String sku, String mrp, int vp, Long categoryId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("sku", sku);
        payload.put("mrp", new BigDecimal(mrp));
        payload.put("volumePoint", vp);
        payload.put("categoryId", categoryId);
        payload.put("active", true);
        return payload;
    }

    private long createProduct(Map<String, Object> payload) throws Exception {
        var result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("a product can be created, read, updated and soft deleted")
    void supportsTheProductLifecycle() throws Exception {
        long categoryId = createCategory("IT Lifecycle Category");
        long productId = createProduct(productPayload("IT Lifecycle", "IT-LIFE", "1500", 40, categoryId));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT Lifecycle"))
                .andExpect(jsonPath("$.volumePoint").value(40))
                .andExpect(jsonPath("$.categoryName").value("IT Lifecycle Category"))
                .andExpect(jsonPath("$.active").value(true));

        Map<String, Object> update = productPayload("IT Lifecycle v2", "IT-LIFE", "1800", 45, categoryId);
        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrp").value(1800.00))
                .andExpect(jsonPath("$.volumePoint").value(45));

        mockMvc.perform(patch("/api/products/" + productId + "/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // deactivate through DELETE is the soft-delete path
        mockMvc.perform(delete("/api/products/" + productId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // a permanent delete is allowed while nothing references the product
        mockMvc.perform(delete("/api/products/" + productId).param("permanent", "true"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("products can be searched, filtered and sorted")
    void supportsSearchFilterAndSort() throws Exception {
        long categoryId = createCategory("IT Filter Category");
        createProduct(productPayload("IT Filter Alpha", "IT-FILTER-A", "100", 10, categoryId));
        createProduct(productPayload("IT Filter Beta", "IT-FILTER-B", "900", 90, categoryId));
        createProduct(productPayload("IT Filter Gamma", "IT-FILTER-C", "500", 50, null));

        mockMvc.perform(get("/api/products").param("search", "IT Filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/products").param("search", "IT Filter").param("sortBy", "mrp")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("IT-FILTER-B"));

        mockMvc.perform(get("/api/products").param("search", "IT Filter").param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/products").param("search", "IT Filter").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/products/selection").param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("a SKU must be unique and quantities must be consistent")
    void validatesProductInput() throws Exception {
        createProduct(productPayload("IT Unique", "IT-UNIQUE", "100", 10, null));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                productPayload("IT Duplicate", "IT-UNIQUE", "100", 10, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SKU_ALREADY_EXISTS"));

        Map<String, Object> inconsistent = productPayload("IT Inconsistent", "IT-BAD", "100", 10, null);
        inconsistent.put("minQuantity", 5);
        inconsistent.put("maxQuantity", 2);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inconsistent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        Map<String, Object> negativeMrp = productPayload("IT Negative", "IT-NEG", "-5", 10, null);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeMrp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("products referenced by history cannot be deleted permanently")
    void protectsProductsWithHistory() throws Exception {
        long productId = createProduct(productPayload("IT Referenced", "IT-REF", "1000", 100, null));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productIds", java.util.List.of(productId));
        payload.put("requiredProductIds", java.util.List.of());
        payload.put("discountPercent", BigDecimal.ZERO);
        payload.put("gstPercent", BigDecimal.ZERO);
        payload.put("targetVp", 100);
        payload.put("toleranceType", "ABSOLUTE");
        payload.put("toleranceValue", BigDecimal.ZERO);
        mockMvc.perform(post("/api/optimizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/products/" + productId).param("permanent", "true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_IN_USE"));

        // soft deletion still works and the session remains readable
        mockMvc.perform(delete("/api/products/" + productId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/optimizations").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].solutionCount").value(1));
    }

    @Test
    @DisplayName("categories can be listed, updated and are protected while in use")
    void managesCategories() throws Exception {
        long categoryId = createCategory("IT Managed Category");
        long productId = createProduct(productPayload("IT Managed Product", "IT-MANAGED", "100", 10, categoryId));

        JsonNode categories = objectMapper.readTree(mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode managed = null;
        for (JsonNode category : categories) {
            if (category.get("id").asLong() == categoryId) {
                managed = category;
            }
        }
        assertThat(managed).isNotNull();
        assertThat(managed.get("productCount").asLong()).isEqualTo(1L);

        mockMvc.perform(delete("/api/categories/" + categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));

        mockMvc.perform(put("/api/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT Managed Category Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT Managed Category Renamed"));

        // move the product out of the category, then the category can be deleted
        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                productPayload("IT Managed Product", "IT-MANAGED", "100", 10, null))))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/categories/" + categoryId)).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT Managed Category Renamed\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("the dashboard summary exposes catalogue and optimization counters")
    void exposesDashboardSummary() throws Exception {
        long productId = createProduct(productPayload("IT Dashboard", "IT-DASH", "1000", 100, null));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productIds", java.util.List.of(productId));
        payload.put("requiredProductIds", java.util.List.of());
        payload.put("discountPercent", BigDecimal.ZERO);
        payload.put("gstPercent", BigDecimal.ZERO);
        payload.put("targetVp", 100);
        payload.put("toleranceType", "ABSOLUTE");
        payload.put("toleranceValue", BigDecimal.ZERO);
        mockMvc.perform(post("/api/optimizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(status().isCreated());

        JsonNode summary = objectMapper.readTree(mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(summary.get("totalProducts").asLong()).isPositive();
        assertThat(summary.get("activeProducts").asLong()).isPositive();
        assertThat(summary.get("categories").asLong()).isPositive();
        assertThat(summary.get("optimizationSessions").asLong()).isPositive();
        assertThat(summary.get("recentSessions")).isNotEmpty();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
