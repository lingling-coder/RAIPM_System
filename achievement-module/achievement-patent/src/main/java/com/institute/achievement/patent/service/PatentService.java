package com.institute.achievement.patent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.patent.dto.PatentDTO;
import com.institute.achievement.patent.dto.PatentVO;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core patent achievement service.
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
public class PatentService {

    private final PatentMapper patentMapper;
    private final INotificationService notificationService;


    /**
     * Create a new patent as DRAFT.
     */
    @Transactional
    public Long createPatent(PatentDTO dto) {
        validateRequiredFields(dto);

        Patent patent = mapToEntity(dto);
        patent.setStatus(AchievementStatusEnum.DRAFT.name());
        patent.setDeptId(SecurityUtils.getCurrentDeptId());
        patent.setCreatedBy(SecurityUtils.getCurrentUserId());
        patent.setCreatedTime(LocalDateTime.now());
        patent.setVersion(1);

        patentMapper.insert(patent);
        log.info("Patent created: id={}, patentName='{}', deptId={}", patent.getId(), patent.getPatentName(), patent.getDeptId());
        return patent.getId();
    }

    /**
     * Update an existing patent.
     * Verifies ownership (created_by matches current user) or secretary role.
     */
    @Transactional
    public void updatePatent(Long id, PatentDTO dto) {
        Patent existing = patentMapper.selectById(id);
        if (existing == null) {
            throw AchievementException.notFound("专利", id);
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

        Patent patent = mapToEntity(dto);
        patent.setId(id);
        patent.setUpdatedBy(currentUserId);
        patent.setUpdatedTime(LocalDateTime.now());

        patentMapper.updateById(patent);
        log.info("Patent updated: id={}", id);
    }

    /**
     * Submit patent for approval.
     * Transitions from DRAFT to PENDING_DEPT_REVIEW.
     * Checks for duplicate applicationNo before allowing submit (D-45).
     *
     * @throws AchievementException if duplicate applicationNo found (error code 4001)
     */
    @Transactional
    public void submitPatent(Long id) {
        Patent patent = patentMapper.selectById(id);
        if (patent == null) {
            throw AchievementException.notFound("专利", id);
        }

        // Verify current status allows submission
        if (!AchievementStatusEnum.DRAFT.name().equals(patent.getStatus())) {
            throw AchievementException.invalidTransition(patent.getStatus(), "submit");
        }

        // D-45: Check duplicate applicationNo on submit
        if (StringUtils.hasText(patent.getApplicationNo())) {
            boolean duplicate = checkDuplicateApplicationNo(patent.getApplicationNo(), id);
            if (duplicate) {
                throw AchievementException.duplicate("申请号", patent.getApplicationNo());
            }
        }

        // Transition status
        patent.setStatus(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        patent.setUpdatedBy(SecurityUtils.getCurrentUserId());
        patent.setUpdatedTime(LocalDateTime.now());
        patentMapper.updateById(patent);

        log.info("Patent submitted: id={}, status={}", id, patent.getStatus());
    }

    /**
     * Save form state as draft.
     * Unlike createPatent, this only validates the patent name.
     */
    @Transactional
    public Long saveDraft(PatentDTO dto) {
        Patent patent = mapToEntity(dto);
        patent.setStatus(AchievementStatusEnum.DRAFT.name());
        patent.setDeptId(SecurityUtils.getCurrentDeptId());
        patent.setCreatedBy(SecurityUtils.getCurrentUserId());
        patent.setCreatedTime(LocalDateTime.now());
        patent.setVersion(1);

        patentMapper.insert(patent);
        log.info("Draft saved: id={}", patent.getId());
        return patent.getId();
    }

    /**
     * Load a patent by ID (throws if not found).
     */
    public Patent getPatentById(Long id) {
        Patent patent = patentMapper.selectById(id);
        if (patent == null) {
            throw AchievementException.notFound("专利", id);
        }
        return patent;
    }

    /**
     * Paginated patent listing with status and keyword filters.
     */
    public Page<PatentVO> pagePatents(int page, int size, String status, String keyword) {
        Page<Patent> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<Patent>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Patent::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Patent::getPatentName, keyword)
                    .or()
                    .like(Patent::getInventors, keyword)
            );
        }

        wrapper.orderByDesc(Patent::getCreatedTime);

        Page<Patent> result = patentMapper.selectPage(pageParam, wrapper);

        // Convert to VO
        Page<PatentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PatentVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .collect(Collectors.toList());

        // Batch populate submitter names
        Set<Long> userIds = voList.stream()
                .map(PatentVO::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = notificationService.resolveUserNames(userIds);
            voList.forEach(vo -> vo.setSubmitterName(nameMap.get(vo.getCreatedBy())));
        }

        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * Check if an application number is already registered (and not invalidated).
     *
     * @param applicationNo the application number to check
     * @param excludeId     patent ID to exclude (for update scenarios)
     * @return true if a non-invalidated patent with the same application number exists
     */
    public boolean checkDuplicateApplicationNo(String applicationNo, Long excludeId) {
        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<Patent>()
                .eq(Patent::getApplicationNo, applicationNo)
                .ne(Patent::getStatus, AchievementStatusEnum.INVALIDATED.name());

        if (excludeId != null) {
            wrapper.ne(Patent::getId, excludeId);
        }

        return patentMapper.selectCount(wrapper) > 0;
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private void validateRequiredFields(PatentDTO dto) {
        if (!StringUtils.hasText(dto.getPatentName())) {
            throw new IllegalArgumentException("专利名称不能为空");
        }
        if (!StringUtils.hasText(dto.getInventors())) {
            throw new IllegalArgumentException("发明人不能为空");
        }
        if (!StringUtils.hasText(dto.getApplicationNo())) {
            throw new IllegalArgumentException("申请号不能为空");
        }
        if (dto.getApplicationDate() == null) {
            throw new IllegalArgumentException("申请日不能为空");
        }
        if (!StringUtils.hasText(dto.getPatentType())) {
            throw new IllegalArgumentException("专利类型不能为空");
        }
        if (!StringUtils.hasText(dto.getCountry())) {
            throw new IllegalArgumentException("国别不能为空");
        }
        if (!StringUtils.hasText(dto.getLegalStatus())) {
            throw new IllegalArgumentException("法律状态不能为空");
        }
    }

    private Patent mapToEntity(PatentDTO dto) {
        Patent patent = new Patent();
        patent.setPatentName(dto.getPatentName());
        patent.setInventors(dto.getInventors());
        patent.setApplicationNo(dto.getApplicationNo());
        patent.setAuthorizationNo(dto.getAuthorizationNo());
        patent.setApplicationDate(dto.getApplicationDate());
        patent.setAuthorizationDate(dto.getAuthorizationDate());
        patent.setPatentType(dto.getPatentType());
        patent.setCountry(dto.getCountry());
        patent.setNextFeeDate(dto.getNextFeeDate());
        patent.setLegalStatus(dto.getLegalStatus());
        patent.setIsClassified(dto.getIsClassified());
        patent.setClassifiedLevel(dto.getClassifiedLevel());
        patent.setProjectRef(dto.getProjectRef());
        return patent;
    }

    private PatentVO mapToVO(Patent patent) {
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
