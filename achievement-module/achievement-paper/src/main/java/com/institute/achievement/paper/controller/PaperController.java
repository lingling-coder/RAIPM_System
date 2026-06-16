package com.institute.achievement.paper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.paper.dto.PaperDTO;
import com.institute.achievement.paper.dto.PaperVO;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.service.PaperService;
import com.institute.achievement.draft.service.DraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for paper achievement CRUD, draft management,
 * submission, and DOI auto-complete integration.
 */
@Slf4j
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final DraftService draftService;

    // ── Paper CRUD ──────────────────────────────────────────────────

    /**
     * Create a new paper draft (status = DRAFT).
     */
    @PostMapping
    public Result<Long> createPaper(@Valid @RequestBody PaperDTO dto) {
        Long id = paperService.createPaper(dto);
        return Result.success(id);
    }

    /**
     * Update an existing paper.
     */
    @PutMapping("/{id}")
    public Result<Void> updatePaper(@PathVariable Long id, @Valid @RequestBody PaperDTO dto) {
        paperService.updatePaper(id, dto);
        return Result.success();
    }

    /**
     * Submit paper for approval (DRAFT → PENDING_DEPT_REVIEW).
     * Checks duplicate DOI before allowing submission.
     */
    @PostMapping("/submit")
    public Result<Void> submitPaper(@RequestParam Long id) {
        paperService.submitPaper(id);
        return Result.success();
    }

    // ── Draft Management ────────────────────────────────────────────

    /**
     * Save current form state as draft.
     */
    @PostMapping("/draft")
    public Result<Long> saveDraft(@RequestBody PaperDTO dto) {
        Long id = paperService.saveDraft(dto);
        return Result.success(id);
    }

    /**
     * List drafts for the current user.
     */
    @GetMapping("/draft")
    public Result<List<PaperVO>> listDrafts() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Paper> drafts = draftService.listDrafts(userId);
        List<PaperVO> voList = drafts.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * Load a specific draft by ID.
     */
    @GetMapping("/draft/{id}")
    public Result<PaperVO> loadDraft(@PathVariable Long id) {
        Paper paper = draftService.loadDraftById(id, SecurityUtils.getCurrentUserId());
        return Result.success(toVO(paper));
    }

    /**
     * Delete a draft.
     */
    @DeleteMapping("/draft/{id}")
    public Result<Void> deleteDraft(@PathVariable Long id) {
        draftService.deleteDraft(id, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    // ── Paper Queries ────────────────────────────────────────────────

    /**
     * Get a single paper by ID.
     */
    @GetMapping("/{id}")
    public Result<PaperVO> getPaperById(@PathVariable Long id) {
        Paper paper = paperService.getPaperById(id);
        return Result.success(toVO(paper));
    }

    /**
     * Paginated paper listing with filters.
     */
    @GetMapping("/page")
    public Result<Page<PaperVO>> pagePapers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<PaperVO> result = paperService.pagePapers(page, size, status, keyword);
        return Result.success(result);
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private PaperVO toVO(Paper paper) {
        PaperVO vo = new PaperVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setAuthors(paper.getAuthors());
        vo.setJournal(paper.getJournal());
        vo.setDoi(paper.getDoi());
        vo.setIssn(paper.getIssn());
        vo.setVolume(paper.getVolume());
        vo.setIssue(paper.getIssue());
        vo.setPages(paper.getPages());
        vo.setPublishYear(paper.getPublishYear());
        vo.setIndexStatus(paper.getIndexStatus());
        vo.setImpactFactor(paper.getImpactFactor());
        vo.setZone(paper.getZone());
        vo.setAbstractText(paper.getAbstractText());
        vo.setIsClassified(paper.getIsClassified());
        vo.setClassifiedLevel(paper.getClassifiedLevel());
        vo.setProjectRef(paper.getProjectRef());
        vo.setStatus(paper.getStatus());
        vo.setArchiveNo(paper.getArchiveNo());
        vo.setDeptId(paper.getDeptId());
        vo.setCreatedBy(paper.getCreatedBy());
        vo.setCreatedTime(paper.getCreatedTime());
        vo.setUpdatedBy(paper.getUpdatedBy());
        vo.setUpdatedTime(paper.getUpdatedTime());
        return vo;
    }
}
