package com.institute.achievement.paper.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.integration.doi.DoiAutoFillService;
import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for DOI auto-complete operations.
 * GET /api/doi/lookup?doi=10.xxxx/xxxx
 */
@Slf4j
@RestController
@RequestMapping("/api/doi")
@RequiredArgsConstructor
public class DoiController {

    private final DoiAutoFillService doiAutoFillService;

    /**
     * Look up a DOI via configured sources (Crossref → OpenAlex with fallback).
     * <p>
     * Implements D-10 (on-blur trigger), D-11 (global priority config),
     * D-13 (failure does not block), D-14 (auto-fallback), D-15 (inline loading).
     */
    @GetMapping("/lookup")
    public Result<DoiLookupResult> lookupDoi(@RequestParam String doi) {
        DoiLookupResult result = doiAutoFillService.lookup(doi);
        return Result.success(result);
    }
}
