package com.example.vpoptimizer.controller;

import com.example.vpoptimizer.dto.PricingPreviewRequest;
import com.example.vpoptimizer.dto.PricingPreviewResponse;
import com.example.vpoptimizer.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pricing preview API.
 *
 * <p>Exposes the authoritative pricing calculation so the frontend can show the discounted price,
 * GST amount and final unit price of a selection without duplicating the formulas.</p>
 */
@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing", description = "Authoritative discount, GST and final price calculation")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/preview")
    @Operation(summary = "Price the current selection (per unit) without optimizing")
    public PricingPreviewResponse preview(@Valid @RequestBody PricingPreviewRequest request) {
        return pricingService.preview(request);
    }
}
