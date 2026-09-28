package com.example.vpoptimizer.repository;

import com.example.vpoptimizer.entity.OptimizationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OptimizationSessionRepository extends JpaRepository<OptimizationSession, Long> {

    @Override
    Optional<OptimizationSession> findById(Long id);

    Page<OptimizationSession> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<OptimizationSession> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);

    List<OptimizationSession> findTop5ByOrderByCreatedAtDesc();
}
