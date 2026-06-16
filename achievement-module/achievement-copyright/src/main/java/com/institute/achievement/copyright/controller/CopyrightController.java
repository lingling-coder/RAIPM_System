package com.institute.achievement.copyright.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.copyright.dto.CopyrightDTO;
import com.institute.achievement.copyright.dto.CopyrightVO;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.service.CopyrightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for software copyright CRUD, draft management,
 * submission, and lifecycle operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/copyrights")
@RequiredArgsConstructor
public class CopyrightController {

    private final CopyrightService copyrightService;

    // ── Copyright CRUD ──────────────────────────────────────────────

    /**
     * Create a new copyright draft (status = DRAFT).
     */
    @PostMapping
    public Result<Long> createCopyright(@Valid @RequestBody CopyrightDTO dto) {
        Long id = copyrightService.createCopyright(dto);
        return Result.success(id);
    }

    /**
     * Update an existing copyright.
     */
    @PutMapping("/{id}")
    public Result<Void> updateCopyright(@PathVariable Long id, @Valid @RequestBody CopyrightDTO dto) {
        copyrightService.updateCopyright(id, dto);
        return Result.success();
    }

    /**
     * Submit copyright for approval (DRAFT -> PENDING_DEPT_REVIEW).
     * Checks duplicate registrationNo before allowing submission.
     */
    @PostMapping("/submit")
    public Result<Void> submitCopyright(@RequestParam Long id) {
        copyrightService.submitCopyright(id);
        return Result.success();
    }

    // ── Draft Management ────────────────────────────────────────────

    /**
     * Save current form state as draft.
     */
    @PostMapping("/draft")
    public Result<Long> saveDraft(@RequestBody CopyrightDTO dto) {
        Long id = copyrightService.saveDraft(dto);
        return Result.success(id);
    }

    /**
     * Load a copyright by ID (draft or created).
     */
    @GetMapping("/{id}")
    public Result<CopyrightVO> getCopyrightById(@PathVariable Long id) {
        Copyright copyright = copyrightService.getCopyrightById(id);
        return Result.success(toVO(copyright));
    }

    // ── Copyright Queries ───────────────────────────────────────────

    /**
     * Paginated copyright listing with filters.
     */
    @GetMapping("/page")
    public Result<Page<CopyrightVO>> pageCopyrights(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<CopyrightVO> result = copyrightService.pageCopyrights(page, size, status, keyword);
        return Result.success(result);
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private CopyrightVO toVO(Copyright copyright) {
        CopyrightVO vo = new CopyrightVO();
        vo.setId(copyright.getId());
        vo.setName(copyright.getName());
        vo.setCopyrightHolder(copyright.getCopyrightHolder());
        vo.setRegistrationNo(copyright.getRegistrationNo());
        vo.setRegistrationDate(copyright.getRegistrationDate());
        vo.setSoftwareVersion(copyright.getSoftwareVersion());
        vo.setSoftwareCategory(copyright.getSoftwareCategory());
        vo.setIsClassified(copyright.getIsClassified());
        vo.setClassifiedLevel(copyright.getClassifiedLevel());
        vo.setProjectRef(copyright.getProjectRef());
        vo.setStatus(copyright.getStatus());
        vo.setArchiveNo(copyright.getArchiveNo());
        vo.setDeptId(copyright.getDeptId());
        vo.setCreatedBy(copyright.getCreatedBy());
        vo.setCreatedTime(copyright.getCreatedTime());
        vo.setUpdatedBy(copyright.getUpdatedBy());
        vo.setUpdatedTime(copyright.getUpdatedTime());
        return vo;
    }
}
