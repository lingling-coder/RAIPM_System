package com.institute.achievement.paper.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.integration.doi.DoiAutoFillService;
import com.institute.achievement.integration.doi.dto.DoiLookupResult;
import com.institute.achievement.paper.dto.LiteratureSearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * Search for publications by title + optional authors.
     * Returns up to 5 candidate matches from CrossRef for user selection.
     * <p>
     * The user picks one candidate, and its metadata is used to auto-fill
     * the paper registration form (title, authors, journal, DOI, etc.).
     */
    @PostMapping("/search")
    public Result<List<DoiLookupResult>> searchLiterature(
            @RequestBody @Valid LiteratureSearchRequest request) {
        List<DoiLookupResult> results = doiAutoFillService.search(
                request.getTitle(), request.getAuthors(), 5);
        return Result.success(results);
    }
}
