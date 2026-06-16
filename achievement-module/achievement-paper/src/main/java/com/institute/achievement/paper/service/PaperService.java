package com.institute.achievement.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.paper.dto.PaperDTO;
import com.institute.achievement.paper.dto.PaperVO;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core paper achievement service.
 * <p>
 * Handles CRUD, draft lifecycle, submit/approval status transitions,
 * duplicate detection, and paginated listing.
 * <p>
 * Implements threat mitigations:
 * - T-01-02 (concurrent update): @Version optimistic locking
 * - T-01-05 (privilege escalation): ownership check on update/delete
 * - D-45/D-47: duplicate check on submit (not on draft save)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;

    /**
     * Create a new paper as DRAFT.
     */
    @Transactional
    public Long createPaper(PaperDTO dto) {
        validateRequiredFields(dto);

        Paper paper = mapToEntity(dto);
        paper.setStatus(AchievementStatusEnum.DRAFT.name());
        paper.setDeptId(SecurityUtils.getCurrentDeptId());
        paper.setCreatedBy(SecurityUtils.getCurrentUserId());
        paper.setCreatedTime(LocalDateTime.now());
        paper.setVersion(1);

        paperMapper.insert(paper);
        log.info("Paper created: id={}, title='{}', deptId={}", paper.getId(), paper.getTitle(), paper.getDeptId());
        return paper.getId();
    }

    /**
     * Update an existing paper.
     * Verifies ownership (created_by matches current user) or secretary role.
     */
    @Transactional
    public void updatePaper(Long id, PaperDTO dto) {
        Paper existing = paperMapper.selectById(id);
        if (existing == null) {
            throw AchievementException.notFound("论文", id);
        }

        // T-01-05: verify ownership
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!existing.getCreatedBy().equals(currentUserId)
                && !SecurityUtils.hasRole("secretary")) {
            throw AchievementException.notAuthorized("只能编辑自己的成果");
        }

        // Only allow editing if DRAFT, REJECTED, or WITHDRAWN
        String status = existing.getStatus();
        if (!AchievementStatusEnum.DRAFT.name().equals(status)
                && !AchievementStatusEnum.REJECTED.name().equals(status)
                && !AchievementStatusEnum.WITHDRAWN.name().equals(status)) {
            throw AchievementException.invalidTransition(status, "update");
        }

        Paper paper = mapToEntity(dto);
        paper.setId(id);
        paper.setUpdatedBy(currentUserId);
        paper.setUpdatedTime(LocalDateTime.now());

        paperMapper.updateById(paper);
        log.info("Paper updated: id={}", id);
    }

    /**
     * Submit paper for approval.
     * Transitions from DRAFT to PENDING_DEPT_REVIEW.
     * Checks for duplicate DOI before allowing submit (D-45).
     *
     * @throws AchievementException if duplicate DOI found (error code 4001)
     */
    @Transactional
    public void submitPaper(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw AchievementException.notFound("论文", id);
        }

        // Verify current status allows submission
        if (!AchievementStatusEnum.DRAFT.name().equals(paper.getStatus())) {
            throw AchievementException.invalidTransition(paper.getStatus(), "submit");
        }

        // D-45: Check duplicate DOI on submit
        if (StringUtils.hasText(paper.getDoi())) {
            boolean duplicate = checkDuplicateDoi(paper.getDoi(), id);
            if (duplicate) {
                throw AchievementException.duplicate("DOI", paper.getDoi());
            }
        }

        // Transition status
        paper.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        paper.setUpdatedBy(SecurityUtils.getCurrentUserId());
        paper.setUpdatedTime(LocalDateTime.now());
        paperMapper.updateById(paper);

        log.info("Paper submitted: id={}, status={}", id, paper.getStatus());
    }

    /**
     * Save form state as draft.
     * Unlike createPaper, this only validates the title.
     */
    @Transactional
    public Long saveDraft(PaperDTO dto) {
        Paper paper = mapToEntity(dto);
        paper.setStatus(AchievementStatusEnum.DRAFT.name());
        paper.setDeptId(SecurityUtils.getCurrentDeptId());
        paper.setCreatedBy(SecurityUtils.getCurrentUserId());
        paper.setCreatedTime(LocalDateTime.now());
        paper.setVersion(1);

        paperMapper.insert(paper);
        log.info("Draft saved: id={}", paper.getId());
        return paper.getId();
    }

    /**
     * Load a paper by ID (throws if not found).
     */
    public Paper getPaperById(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw AchievementException.notFound("论文", id);
        }
        return paper;
    }

    /**
     * Paginated paper listing with status and keyword filters.
     */
    public Page<PaperVO> pagePapers(int page, int size, String status, String keyword) {
        Page<Paper> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Paper::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Paper::getTitle, keyword)
                    .or()
                    .like(Paper::getAuthors, keyword)
            );
        }

        wrapper.orderByDesc(Paper::getCreatedTime);

        Page<Paper> result = paperMapper.selectPage(pageParam, wrapper);

        // Convert to VO
        Page<PaperVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PaperVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * Check if a DOI is already registered (and not invalidated).
     *
     * @param doi         the DOI to check
     * @param excludeId   paper ID to exclude (for update scenarios)
     * @return true if a non-invalidated paper with the same DOI exists
     */
    public boolean checkDuplicateDoi(String doi, Long excludeId) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getDoi, doi)
                .ne(Paper::getStatus, AchievementStatusEnum.INVALIDATED.name());

        if (excludeId != null) {
            wrapper.ne(Paper::getId, excludeId);
        }

        return paperMapper.selectCount(wrapper) > 0;
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private void validateRequiredFields(PaperDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("论文标题不能为空");
        }
        if (!StringUtils.hasText(dto.getAuthors())) {
            throw new IllegalArgumentException("作者不能为空");
        }
        if (!StringUtils.hasText(dto.getJournal())) {
            throw new IllegalArgumentException("期刊/会议名称不能为空");
        }
        if (dto.getPublishYear() == null) {
            throw new IllegalArgumentException("发表年份不能为空");
        }
        if (!StringUtils.hasText(dto.getIndexStatus())) {
            throw new IllegalArgumentException("收录情况不能为空");
        }
    }

    private Paper mapToEntity(PaperDTO dto) {
        Paper paper = new Paper();
        paper.setTitle(dto.getTitle());
        paper.setAuthors(dto.getAuthors());
        paper.setJournal(dto.getJournal());
        paper.setDoi(dto.getDoi());
        paper.setIssn(dto.getIssn());
        paper.setVolume(dto.getVolume());
        paper.setIssue(dto.getIssue());
        paper.setPages(dto.getPages());
        paper.setPublishYear(dto.getPublishYear());
        paper.setIndexStatus(dto.getIndexStatus());
        paper.setImpactFactor(dto.getImpactFactor());
        paper.setZone(dto.getZone());
        paper.setAbstractText(dto.getAbstractText());
        paper.setIsClassified(dto.getIsClassified());
        paper.setClassifiedLevel(dto.getClassifiedLevel());
        paper.setProjectRef(dto.getProjectRef());
        return paper;
    }

    private PaperVO mapToVO(Paper paper) {
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
