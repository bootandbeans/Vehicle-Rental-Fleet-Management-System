package com.example.vpoptimizer.dto;

/** Simple liveness payload used by the frontend to decide between live API and demo mode. */
public record HealthResponse(String status, String application, String version) {
}
