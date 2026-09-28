package com.example.vpoptimizer.controller;

import com.example.vpoptimizer.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness endpoint the frontend uses to detect whether a live backend is available. */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Service liveness")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Service status")
    public HealthResponse health() {
        return new HealthResponse("UP", "vp-optimizer", "1.0.0");
    }
}
