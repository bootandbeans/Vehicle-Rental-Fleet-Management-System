package com.example.vpoptimizer.service;

import com.example.vpoptimizer.dto.OptimizationRunResponse;
import com.example.vpoptimizer.dto.OptimizationSessionSummaryResponse;
import com.example.vpoptimizer.dto.PageResponse;
import com.example.vpoptimizer.entity.OptimizationSession;
import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.ResourceNotFoundException;
import com.example.vpoptimizer.mapper.OptimizationResponseMapper;
import com.example.vpoptimizer.repository.OptimizationSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to saved optimization sessions.
 *
 * <p>Everything is served from the stored snapshot, so a session opened months later still shows the
 * prices and VP values it was calculated with.</p>
 */
@Service
@Transactional(readOnly = true)
public class OptimizationHistoryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final OptimizationSessionRepository sessionRepository;
    private final OptimizationResponseMapper responseMapper;

    public OptimizationHistoryService(OptimizationSessionRepository sessionRepository,
                                      OptimizationResponseMapper responseMapper) {
        this.sessionRepository = sessionRepository;
        this.responseMapper = responseMapper;
    }

    public PageResponse<OptimizationSessionSummaryResponse> findHistory(String search, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<OptimizationSession> sessions = search == null || search.isBlank()
                ? sessionRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : sessionRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(search.trim(), pageRequest);
        return PageResponse.of(sessions, responseMapper::toSummary);
    }

    public OptimizationRunResponse findById(Long id) {
        return responseMapper.toRunResponse(requireSession(id));
    }

    /** Loads a session or fails with {@code OPTIMIZATION_SESSION_NOT_FOUND}. */
    public OptimizationSession requireSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.OPTIMIZATION_SESSION_NOT_FOUND,
                        "Optimization session " + id + " does not exist."));
    }
}
