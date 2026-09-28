package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.DashboardSummaryResponse;
import com.example.vpoptimizer.dto.OptimizationSessionSummaryResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.mapper.OptimizationResponseMapper;
import com.example.vpoptimizer.repository.CategoryRepository;
import com.example.vpoptimizer.repository.OptimizationSessionRepository;
import com.example.vpoptimizer.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Aggregated numbers for the dashboard page. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OptimizationSessionRepository sessionRepository;
    private final OptimizationResponseMapper responseMapper;

    public DashboardService(ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            OptimizationSessionRepository sessionRepository,
                            OptimizationResponseMapper responseMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.sessionRepository = sessionRepository;
        this.responseMapper = responseMapper;
    }

    public DashboardSummaryResponse summary() {
        List<OptimizationSession> recent = sessionRepository.findTop5ByOrderByCreatedAtDesc();
        List<OptimizationSessionSummaryResponse> recentSessions = recent.stream()
                .map(responseMapper::toSummary)
                .toList();
        Instant lastOptimizationAt = recent.isEmpty() ? null : recent.get(0).getCreatedAt();
        return new DashboardSummaryResponse(
                productRepository.count(),
                productRepository.countByActiveTrue(),
                categoryRepository.count(),
                sessionRepository.count(),
                lastOptimizationAt,
                recentSessions);
    }
}
