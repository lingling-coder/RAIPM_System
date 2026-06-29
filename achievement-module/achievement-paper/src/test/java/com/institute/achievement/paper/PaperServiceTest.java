package com.institute.achievement.paper;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.paper.dto.PaperDTO;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.paper.service.PaperService;
import com.institute.achievement.framework.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.stubbing.Answer;

/**
 * Unit tests for PaperService covering creation, submission, draft lifecycle,
 * duplicate detection, and pagination.
 */
@ExtendWith(MockitoExtension.class)
class PaperServiceTest {

    @Mock
    private PaperMapper paperMapper;

    @Mock
    private INotificationService notificationService;

    private PaperService paperService;

    @BeforeEach
    void setUp() {
        paperService = new PaperService(paperMapper, notificationService);
    }

    @Test
    void testCreatePaper_shouldSetDraftStatusAndDeptId() {
        // Arrange
        PaperDTO dto = new PaperDTO();
        dto.setTitle("Test Paper Title");
        dto.setAuthors("Author A; Author B");
        dto.setJournal("Test Journal");
        dto.setPublishYear(2026);
        dto.setIndexStatus("SCI");

        when(paperMapper.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = paperService.createPaper(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Paper> captor = ArgumentCaptor.forClass(Paper.class);
            verify(paperMapper).insert(captor.capture());
            Paper saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getCreatedBy()).isEqualTo(1L);
            assertThat(saved.getTitle()).isEqualTo("Test Paper Title");
        }
    }

    @Test
    void testCreatePaper_shouldThrowWhenRequiredFieldsMissing() {
        // Arrange
        PaperDTO dto = new PaperDTO();
        dto.setTitle("Test"); // Missing authors, journal, publishYear, indexStatus

        // Act & Assert
        assertThatThrownBy(() -> paperService.createPaper(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSubmitPaper_shouldTransitionToPendingDeptReview() {
        // Arrange
        PaperDTO dto = new PaperDTO();
        dto.setTitle("Test Paper");
        dto.setAuthors("Author A");
        dto.setJournal("Journal");
        dto.setPublishYear(2026);
        dto.setIndexStatus("SCI");

        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Test Paper");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDeptId(10L);

        when(paperMapper.selectById(1L)).thenReturn(existing);
        when(paperMapper.updateById(any(Paper.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act
            paperService.submitPaper(1L);

            // Assert
            ArgumentCaptor<Paper> captor = ArgumentCaptor.forClass(Paper.class);
            verify(paperMapper, atLeastOnce()).updateById(captor.capture());
            Paper updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        }
    }

    @Test
    void testSubmitDuplicateDoi_shouldThrow() {
        // Arrange
        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Test Paper");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDoi("10.1234/test.2026.001");

        when(paperMapper.selectById(1L)).thenReturn(existing);

        // Simulate duplicate DOI exists
        when(paperMapper.selectCount(any())).thenReturn(1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> paperService.submitPaper(1L))
                    .isInstanceOf(AchievementException.class)
                    .hasMessageContaining("E4001");
        }
    }

    @Test
    void testSaveDraft_shouldSetDraftStatus() {
        // Arrange
        PaperDTO dto = new PaperDTO();
        dto.setTitle("Draft Paper");
        dto.setAuthors("Author A");
        dto.setJournal("Journal");
        dto.setPublishYear(2026);
        dto.setIndexStatus("SCI");

        when(paperMapper.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = paperService.saveDraft(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Paper> captor = ArgumentCaptor.forClass(Paper.class);
            verify(paperMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
        }
    }

    @Test
    void testPagePapers_shouldApplyFilters() {
        // Arrange
        when(paperMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = paperService.pagePapers(1, 20, "DRAFT", "keyword");

        // Assert
        assertThat(result).isNotNull();
        verify(paperMapper).selectPage(any(), any());
    }
}
