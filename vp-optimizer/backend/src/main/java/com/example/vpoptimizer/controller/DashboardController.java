package com.example.vpoptimizer.controller;

import com.example.vpoptimizer.dto.DashboardSummaryResponse;
import com.example.vpoptimizer.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard API. */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Catalogue and optimization statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Product, category and optimization counters plus the latest sessions")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }
}
