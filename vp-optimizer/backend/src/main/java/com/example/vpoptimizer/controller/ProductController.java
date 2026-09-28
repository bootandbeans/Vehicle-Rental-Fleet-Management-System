package com.example.vpoptimizer.controller;

import com.example.vpoptimizer.dto.PageResponse;
import com.example.vpoptimizer.dto.ProductActiveRequest;
import com.example.vpoptimizer.dto.ProductQuery;
import com.example.vpoptimizer.dto.ProductRequest;
import com.example.vpoptimizer.dto.ProductResponse;
import com.example.vpoptimizer.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Product catalogue API. */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Manage the product catalogue (MRP, volume points, limits, categories)")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Search / filter / sort products (paged)")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return productService.search(ProductQuery.of(search, categoryId, active, sortBy, direction, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a product")
    public ProductResponse setActive(@PathVariable Long id, @Valid @RequestBody ProductActiveRequest request) {
        return productService.setActive(id, request.active());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product",
            description = "By default the product is deactivated (soft delete) so that history keeps working. "
                    + "Use ?permanent=true to remove the row when no optimization ever referenced it.")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean permanent) {
        productService.delete(id, permanent);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/selection")
    @Operation(summary = "Products available for selection (optionally by category)")
    public List<ProductResponse> selectable(@RequestParam(required = false) Long categoryId) {
        return productService.search(ProductQuery.of(null, categoryId, true, "name", "asc", 0,
                ProductQuery.MAX_PAGE_SIZE)).content();
    }
}
