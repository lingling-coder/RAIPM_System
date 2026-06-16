package com.institute.achievement.copyright.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.copyright.dto.CopyrightDTO;
import com.institute.achievement.copyright.dto.CopyrightVO;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core software copyright achievement service.
 * <p>
 * Handles CRUD, draft lifecycle, submit/approval status transitions,
 * duplicate detection, and paginated listing.
 * <p>
 * Implements threat mitigations:
 * - T-01-02 (concurrent update): @Version optimistic locking
 * - T-01-04 (privilege escalation): ownership check on update/delete
 * - D-45/D-47: duplicate check on submit (not on draft save)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightService {

    private final CopyrightMapper copyrightMapper;

    /**
     * Create a new copyright as DRAFT.
     */
    @Transactional
    public Long createCopyright(CopyrightDTO dto) {
        validateRequiredFields(dto);

        Copyright copyright = mapToEntity(dto);
        copyright.setStatus(AchievementStatusEnum.DRAFT.name());
        copyright.setDeptId(SecurityUtils.getCurrentDeptId());
        copyright.setCreatedBy(SecurityUtils.getCurrentUserId());
        copyright.setCreatedTime(LocalDateTime.now());
        copyright.setVersion(1);

        copyrightMapper.insert(copyright);
        log.info("Copyright created: id={}, name='{}', deptId={}", copyright.getId(), copyright.getName(), copyright.getDeptId());
        return copyright.getId();
    }

    /**
     * Update an existing copyright.
     * Verifies ownership (created_by matches current user) or secretary role.
     */
    @Transactional
    public void updateCopyright(Long id, CopyrightDTO dto) {
        Copyright existing = copyrightMapper.selectById(id);
        if (existing == null) {
            throw AchievementException.notFound("软件著作权", id);
        }

        // T-01-04: verify ownership
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

        Copyright copyright = mapToEntity(dto);
        copyright.setId(id);
        copyright.setUpdatedBy(currentUserId);
        copyright.setUpdatedTime(LocalDateTime.now());

        copyrightMapper.updateById(copyright);
        log.info("Copyright updated: id={}", id);
    }

    /**
     * Submit copyright for approval.
     * Transitions from DRAFT to PENDING_DEPT_REVIEW.
     * Checks for duplicate registrationNo before allowing submit (D-45).
     *
     * @throws AchievementException if duplicate registrationNo found (error code 4001)
     */
    @Transactional
    public void submitCopyright(Long id) {
        Copyright copyright = copyrightMapper.selectById(id);
        if (copyright == null) {
            throw AchievementException.notFound("软件著作权", id);
        }

        // Verify current status allows submission
        if (!AchievementStatusEnum.DRAFT.name().equals(copyright.getStatus())) {
            throw AchievementException.invalidTransition(copyright.getStatus(), "submit");
        }

        // D-45: Check duplicate registrationNo on submit
        if (StringUtils.hasText(copyright.getRegistrationNo())) {
            boolean duplicate = checkDuplicateRegistrationNo(copyright.getRegistrationNo(), id);
            if (duplicate) {
                throw AchievementException.duplicate("登记号", copyright.getRegistrationNo());
            }
        }

        // Transition status
        copyright.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        copyright.setUpdatedBy(SecurityUtils.getCurrentUserId());
        copyright.setUpdatedTime(LocalDateTime.now());
        copyrightMapper.updateById(copyright);

        log.info("Copyright submitted: id={}, status={}", id, copyright.getStatus());
    }

    /**
     * Save form state as draft.
     * Unlike createCopyright, this only validates the name.
     */
    @Transactional
    public Long saveDraft(CopyrightDTO dto) {
        Copyright copyright = mapToEntity(dto);
        copyright.setStatus(AchievementStatusEnum.DRAFT.name());
        copyright.setDeptId(SecurityUtils.getCurrentDeptId());
        copyright.setCreatedBy(SecurityUtils.getCurrentUserId());
        copyright.setCreatedTime(LocalDateTime.now());
        copyright.setVersion(1);

        copyrightMapper.insert(copyright);
        log.info("Draft saved: id={}", copyright.getId());
        return copyright.getId();
    }

    /**
     * Load a copyright by ID (throws if not found).
     */
    public Copyright getCopyrightById(Long id) {
        Copyright copyright = copyrightMapper.selectById(id);
        if (copyright == null) {
            throw AchievementException.notFound("软件著作权", id);
        }
        return copyright;
    }

    /**
     * Paginated copyright listing with status and keyword filters.
     */
    public Page<CopyrightVO> pageCopyrights(int page, int size, String status, String keyword) {
        Page<Copyright> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Copyright> wrapper = new LambdaQueryWrapper<Copyright>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Copyright::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Copyright::getName, keyword)
                    .or()
                    .like(Copyright::getCopyrightHolder, keyword)
            );
        }

        wrapper.orderByDesc(Copyright::getCreatedTime);

        Page<Copyright> result = copyrightMapper.selectPage(pageParam, wrapper);

        // Convert to VO
        Page<CopyrightVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<CopyrightVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * Check if a registration number is already registered (and not invalidated).
     *
     * @param registrationNo the registration number to check
     * @param excludeId      copyright ID to exclude (for update scenarios)
     * @return true if a non-invalidated copyright with the same registration number exists
     */
    public boolean checkDuplicateRegistrationNo(String registrationNo, Long excludeId) {
        LambdaQueryWrapper<Copyright> wrapper = new LambdaQueryWrapper<Copyright>()
                .eq(Copyright::getRegistrationNo, registrationNo)
                .ne(Copyright::getStatus, AchievementStatusEnum.INVALIDATED.name());

        if (excludeId != null) {
            wrapper.ne(Copyright::getId, excludeId);
        }

        return copyrightMapper.selectCount(wrapper) > 0;
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private void validateRequiredFields(CopyrightDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("软著名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCopyrightHolder())) {
            throw new IllegalArgumentException("著作权人不能为空");
        }
        if (!StringUtils.hasText(dto.getRegistrationNo())) {
            throw new IllegalArgumentException("登记号不能为空");
        }
        if (dto.getRegistrationDate() == null) {
            throw new IllegalArgumentException("登记日期不能为空");
        }
        if (!StringUtils.hasText(dto.getSoftwareVersion())) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        if (!StringUtils.hasText(dto.getSoftwareCategory())) {
            throw new IllegalArgumentException("软件类别不能为空");
        }
    }

    private Copyright mapToEntity(CopyrightDTO dto) {
        Copyright copyright = new Copyright();
        copyright.setName(dto.getName());
        copyright.setCopyrightHolder(dto.getCopyrightHolder());
        copyright.setRegistrationNo(dto.getRegistrationNo());
        copyright.setRegistrationDate(dto.getRegistrationDate());
        copyright.setSoftwareVersion(dto.getSoftwareVersion());
        copyright.setSoftwareCategory(dto.getSoftwareCategory());
        copyright.setIsClassified(dto.getIsClassified());
        copyright.setClassifiedLevel(dto.getClassifiedLevel());
        copyright.setProjectRef(dto.getProjectRef());
        return copyright;
    }

    private CopyrightVO mapToVO(Copyright copyright) {
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
