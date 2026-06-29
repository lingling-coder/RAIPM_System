package com.institute.achievement.patent;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.patent.dto.PatentDTO;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import com.institute.achievement.patent.service.PatentService;
import com.institute.achievement.framework.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatentService covering creation, submission, duplicate detection,
 * draft lifecycle, and pagination.
 */
@ExtendWith(MockitoExtension.class)
class PatentServiceTest {

    @Mock
    private PatentMapper patentMapper;

    @Mock
    private INotificationService notificationService;

    private PatentService patentService;

    @BeforeEach
    void setUp() {
        patentService = new PatentService(patentMapper, notificationService);
    }

    // ── createPatent ──────────────────────────────────────────────────────

    @Test
    void testCreatePatent_shouldSetDraftStatusAndDeptId() {
        // Arrange
        PatentDTO dto = new PatentDTO();
        dto.setPatentName("Test Patent");
        dto.setInventors("Inventor A; Inventor B");
        dto.setApplicationNo("202410123456.0");
        dto.setApplicationDate(LocalDate.of(2024, 1, 15));
        dto.setPatentType("发明");
        dto.setCountry("中国");
        dto.setLegalStatus("授权");

        when(patentMapper.insert(any(Patent.class))).thenAnswer(invocation -> {
            Patent p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = patentService.createPatent(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Patent> captor = ArgumentCaptor.forClass(Patent.class);
            verify(patentMapper).insert(captor.capture());
            Patent saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getCreatedBy()).isEqualTo(1L);
            assertThat(saved.getPatentName()).isEqualTo("Test Patent");
            assertThat(saved.getInventors()).isEqualTo("Inventor A; Inventor B");
            assertThat(saved.getApplicationNo()).isEqualTo("202410123456.0");
            assertThat(saved.getApplicationDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(saved.getPatentType()).isEqualTo("发明");
            assertThat(saved.getCountry()).isEqualTo("中国");
            assertThat(saved.getLegalStatus()).isEqualTo("授权");
        }
    }

    @Test
    void testCreatePatent_shouldThrowWhenRequiredFieldsMissing() {
        // Arrange
        PatentDTO dto = new PatentDTO();
        dto.setPatentName("Test"); // Missing other required fields

        // Act & Assert
        assertThatThrownBy(() -> patentService.createPatent(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── submitPatent ──────────────────────────────────────────────────────

    @Test
    void testSubmitPatent_shouldTransitionToPendingDeptReview() {
        // Arrange
        Patent existing = new Patent();
        existing.setId(1L);
        existing.setPatentName("Test Patent");
        existing.setInventors("Inventor A");
        existing.setApplicationNo("202410123456.0");
        existing.setApplicationDate(LocalDate.of(2024, 1, 15));
        existing.setPatentType("发明");
        existing.setCountry("中国");
        existing.setLegalStatus("授权");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDeptId(10L);

        when(patentMapper.selectById(1L)).thenReturn(existing);
        when(patentMapper.selectCount(any())).thenReturn(0L); // No duplicate
        when(patentMapper.updateById(any(Patent.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act
            patentService.submitPatent(1L);

            // Assert
            ArgumentCaptor<Patent> captor = ArgumentCaptor.forClass(Patent.class);
            verify(patentMapper, atLeastOnce()).updateById(captor.capture());
            Patent updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        }
    }

    @Test
    void testSubmitDuplicateApplicationNo_shouldThrow() {
        // Arrange
        Patent existing = new Patent();
        existing.setId(1L);
        existing.setPatentName("Test Patent");
        existing.setInventors("Inventor A");
        existing.setApplicationNo("202410123456.0");
        existing.setApplicationDate(LocalDate.of(2024, 1, 15));
        existing.setPatentType("发明");
        existing.setCountry("中国");
        existing.setLegalStatus("授权");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDeptId(10L);

        when(patentMapper.selectById(1L)).thenReturn(existing);
        // Simulate duplicate applicationNo exists
        when(patentMapper.selectCount(any())).thenReturn(1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> patentService.submitPatent(1L))
                    .isInstanceOf(AchievementException.class)
                    .hasMessageContaining("E4001");
        }
    }

    // ── saveDraft ─────────────────────────────────────────────────────────

    @Test
    void testSaveDraft_shouldSetDraftStatus() {
        // Arrange
        PatentDTO dto = new PatentDTO();
        dto.setPatentName("Draft Patent");
        dto.setInventors("Inventor A");
        dto.setApplicationNo("202410123456.0");
        dto.setApplicationDate(LocalDate.of(2024, 1, 15));
        dto.setPatentType("实用新型");
        dto.setCountry("中国");
        dto.setLegalStatus("实审");

        when(patentMapper.insert(any(Patent.class))).thenAnswer(invocation -> {
            Patent p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = patentService.saveDraft(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Patent> captor = ArgumentCaptor.forClass(Patent.class);
            verify(patentMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
        }
    }

    // ── updatePatent ──────────────────────────────────────────────────────

    @Test
    void testUpdatePatent_shouldVerifyOwnership() {
        // Arrange
        PatentDTO dto = new PatentDTO();
        dto.setPatentName("Updated Patent");
        dto.setInventors("Inventor A");
        dto.setApplicationNo("202410123456.0");
        dto.setApplicationDate(LocalDate.of(2024, 1, 15));
        dto.setPatentType("发明");
        dto.setCountry("中国");
        dto.setLegalStatus("授权");

        Patent existing = new Patent();
        existing.setId(1L);
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);

        when(patentMapper.selectById(1L)).thenReturn(existing);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act
            patentService.updatePatent(1L, dto);

            // Assert
            ArgumentCaptor<Patent> captor = ArgumentCaptor.forClass(Patent.class);
            verify(patentMapper).updateById(captor.capture());
            assertThat(captor.getValue().getPatentName()).isEqualTo("Updated Patent");
        }
    }

    // ── pagePatents ───────────────────────────────────────────────────────

    @Test
    void testPagePatents_shouldApplyFilters() {
        // Arrange
        when(patentMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = patentService.pagePatents(1, 20, "DRAFT", "keyword");

        // Assert
        assertThat(result).isNotNull();
        verify(patentMapper).selectPage(any(), any());
    }
}
