package com.institute.achievement.draft.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Draft management service for paper achievements.
 * <p>
 * Drafts are stored in the paper table itself with status=DRAFT.
 * This service provides draft-specific operations: list, load, and delete.
 * <p>
 * Implements D-09 (draft save functionality) and D-48 (draft editing).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftService {

    private final PaperMapper paperMapper;

    /**
     * List all drafts for a specific user.
     */
    public List<Paper> listDrafts(Long userId) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getCreatedBy, userId)
                .eq(Paper::getStatus, AchievementStatusEnum.DRAFT.name())
                .orderByDesc(Paper::getUpdatedTime);

        return paperMapper.selectList(wrapper);
    }

    /**
     * Load a specific draft by ID, verifying ownership.
     *
     * @param draftId the draft ID
     * @param userId  the current user's ID
     * @return the draft paper entity
     * @throws com.institute.achievement.common.exception.AchievementException
     *         if not found or not owned by user
     */
    public Paper loadDraftById(Long draftId, Long userId) {
        Paper paper = paperMapper.selectById(draftId);
        if (paper == null) {
            throw com.institute.achievement.common.exception.AchievementException.notFound("草稿", draftId);
        }
        if (!AchievementStatusEnum.DRAFT.name().equals(paper.getStatus())) {
            throw com.institute.achievement.common.exception.AchievementException.invalidTransition(
                    paper.getStatus(), "loadDraft");
        }
        if (!paper.getCreatedBy().equals(userId)) {
            throw com.institute.achievement.common.exception.AchievementException.notAuthorized("无权访问此草稿");
        }
        return paper;
    }

    /**
     * Delete a draft (soft-delete is not used — drafts are physically deleted).
     */
    @Transactional
    public void deleteDraft(Long draftId, Long userId) {
        Paper paper = paperMapper.selectById(draftId);
        if (paper == null) {
            throw com.institute.achievement.common.exception.AchievementException.notFound("草稿", draftId);
        }
        if (!paper.getCreatedBy().equals(userId)) {
            throw com.institute.achievement.common.exception.AchievementException.notAuthorized("无权删除此草稿");
        }
        if (!AchievementStatusEnum.DRAFT.name().equals(paper.getStatus())) {
            throw com.institute.achievement.common.exception.AchievementException.invalidTransition(
                    paper.getStatus(), "deleteDraft");
        }

        paperMapper.deleteById(draftId);
        log.info("Draft {} deleted by user {}", draftId, userId);
    }
}
