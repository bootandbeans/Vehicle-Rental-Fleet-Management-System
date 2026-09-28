package com.example.vpoptimizer.controller;

import com.example.vpoptimizer.dto.OptimizationRunRequest;
import com.example.vpoptimizer.dto.OptimizationRunResponse;
import com.example.vpoptimizer.dto.OptimizationSessionSummaryResponse;
import com.example.vpoptimizer.dto.PageResponse;
import com.example.vpoptimizer.service.OptimizationHistoryService;
import com.example.vpoptimizer.service.OptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Optimization API.
 *
 * <p>{@code POST} runs an optimization over the explicitly selected products and stores the session;
 * the {@code GET} endpoints reopen stored sessions from their snapshot.</p>
 */
@RestController
@RequestMapping("/api/optimizations")
@Tag(name = "Optimization", description = "Run optimizations over a product selection and read the ranked results")
public class OptimizationController {

    private final OptimizationService optimizationService;
    private final OptimizationHistoryService historyService;

    public OptimizationController(OptimizationService optimizationService,
                                  OptimizationHistoryService historyService) {
        this.optimizationService = optimizationService;
        this.historyService = historyService;
    }

    @PostMapping
    @Operation(summary = "Run an optimization for a selection of products")
    public ResponseEntity<OptimizationRunResponse> run(@Valid @RequestBody OptimizationRunRequest request) {
        OptimizationRunResponse response = optimizationService.run(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/optimizations/" + response.sessionId()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List saved optimization sessions (paged, newest first)")
    public PageResponse<OptimizationSessionSummaryResponse> history(@RequestParam(required = false) String search,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size) {
        return historyService.findHistory(search, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Reopen a saved optimization session")
    public OptimizationRunResponse findById(@PathVariable Long id) {
        return historyService.findById(id);
    }
}
