package com.institute.achievement.paper;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.paper.dto.PaperDTO;
import com.institute.achievement.paper.dto.PaperVO;
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

/**
 * Integration-like tests for paper service covering the full lifecycle
 * flow: create -> submit -> verify status, duplicate DOI blocking,
 * draft lifecycle, and pagination.
 * <p>
 * Uses mocked persistence layer with Mockito, verifying service orchestration
 * logic (status transitions, authorization checks, duplicate validation).
 */
@ExtendWith(MockitoExtension.class)
class PaperServiceIntegrationTest {

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
    void testCreateAndSubmitPaper() {
        // Arrange
        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Integration Test Paper");
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
    void testDuplicateDoiBlock() {
        // Arrange
        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Duplicate DOI Test");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDoi("10.1234/duplicate-test");

        when(paperMapper.selectById(1L)).thenReturn(existing);
        when(paperMapper.selectCount(any())).thenReturn(1L); // Duplicate exists

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> paperService.submitPaper(1L))
                    .isInstanceOf(AchievementException.class);
        }
    }

    @Test
    void testPaperPagination() {
        // Arrange
        when(paperMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = paperService.pagePapers(1, 20, null, null);

        // Assert
        assertThat(result).isNotNull();
        verify(paperMapper).selectPage(any(), any());
    }
}
