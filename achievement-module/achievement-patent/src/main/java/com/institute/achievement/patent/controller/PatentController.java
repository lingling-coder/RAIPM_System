package com.institute.achievement.patent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.patent.dto.PatentDTO;
import com.institute.achievement.patent.dto.PatentVO;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.service.PatentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for patent achievement CRUD, draft management,
 * submission, and lifecycle operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/patents")
@RequiredArgsConstructor
public class PatentController {

    private final PatentService patentService;

    // ── Patent CRUD ──────────────────────────────────────────────────

    /**
     * Create a new patent draft (status = DRAFT).
     */
    @PostMapping
    public Result<Long> createPatent(@Valid @RequestBody PatentDTO dto) {
        Long id = patentService.createPatent(dto);
        return Result.success(id);
    }

    /**
     * Update an existing patent.
     */
    @PutMapping("/{id}")
    public Result<Void> updatePatent(@PathVariable Long id, @Valid @RequestBody PatentDTO dto) {
        patentService.updatePatent(id, dto);
        return Result.success();
    }

    /**
     * Submit patent for approval (DRAFT -> PENDING_DEPT_REVIEW).
     * Checks duplicate applicationNo before allowing submission.
     */
    @PostMapping("/submit")
    public Result<Void> submitPatent(@RequestParam Long id) {
        patentService.submitPatent(id);
        return Result.success();
    }

    // ── Draft Management ────────────────────────────────────────────

    /**
     * Save current form state as draft.
     */
    @PostMapping("/draft")
    public Result<Long> saveDraft(@RequestBody PatentDTO dto) {
        Long id = patentService.saveDraft(dto);
        return Result.success(id);
    }

    /**
     * Load a patent by ID (draft or created).
     */
    @GetMapping("/{id}")
    public Result<PatentVO> getPatentById(@PathVariable Long id) {
        Patent patent = patentService.getPatentById(id);
        return Result.success(toVO(patent));
    }

    // ── Patent Queries ────────────────────────────────────────────────

    /**
     * Paginated patent listing with filters.
     */
    @GetMapping("/page")
    public Result<Page<PatentVO>> pagePatents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<PatentVO> result = patentService.pagePatents(page, size, status, keyword);
        return Result.success(result);
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private PatentVO toVO(Patent patent) {
        PatentVO vo = new PatentVO();
        vo.setId(patent.getId());
        vo.setPatentName(patent.getPatentName());
        vo.setInventors(patent.getInventors());
        vo.setApplicationNo(patent.getApplicationNo());
        vo.setAuthorizationNo(patent.getAuthorizationNo());
        vo.setApplicationDate(patent.getApplicationDate());
        vo.setAuthorizationDate(patent.getAuthorizationDate());
        vo.setPatentType(patent.getPatentType());
        vo.setCountry(patent.getCountry());
        vo.setNextFeeDate(patent.getNextFeeDate());
        vo.setLegalStatus(patent.getLegalStatus());
        vo.setIsClassified(patent.getIsClassified());
        vo.setClassifiedLevel(patent.getClassifiedLevel());
        vo.setProjectRef(patent.getProjectRef());
        vo.setStatus(patent.getStatus());
        vo.setArchiveNo(patent.getArchiveNo());
        vo.setDeptId(patent.getDeptId());
        vo.setCreatedBy(patent.getCreatedBy());
        vo.setCreatedTime(patent.getCreatedTime());
        vo.setUpdatedBy(patent.getUpdatedBy());
        vo.setUpdatedTime(patent.getUpdatedTime());
        return vo;
    }
}
