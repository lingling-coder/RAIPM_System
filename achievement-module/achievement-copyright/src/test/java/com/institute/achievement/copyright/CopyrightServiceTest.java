package com.institute.achievement.copyright;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.copyright.dto.CopyrightDTO;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.copyright.service.CopyrightService;
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
 * Unit tests for CopyrightService covering creation, submission, duplicate detection,
 * draft lifecycle, and pagination.
 */
@ExtendWith(MockitoExtension.class)
class CopyrightServiceTest {

    @Mock
    private CopyrightMapper copyrightMapper;

    @Mock
    private INotificationService notificationService;

    private CopyrightService copyrightService;

    @BeforeEach
    void setUp() {
        copyrightService = new CopyrightService(copyrightMapper, notificationService);
    }

    // ── createCopyright ───────────────────────────────────────────────────

    @Test
    void testCreateCopyright_shouldSetDraftStatusAndDeptId() {
        // Arrange
        CopyrightDTO dto = new CopyrightDTO();
        dto.setName("Test Software");
        dto.setCopyrightHolder("Institute A");
        dto.setRegistrationNo("2026SR000001");
        dto.setRegistrationDate(LocalDate.of(2026, 1, 15));
        dto.setSoftwareVersion("V1.0");
        dto.setSoftwareCategory("应用软件");

        when(copyrightMapper.insert(any(Copyright.class))).thenAnswer(invocation -> {
            Copyright c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = copyrightService.createCopyright(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Copyright> captor = ArgumentCaptor.forClass(Copyright.class);
            verify(copyrightMapper).insert(captor.capture());
            Copyright saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getCreatedBy()).isEqualTo(1L);
            assertThat(saved.getName()).isEqualTo("Test Software");
            assertThat(saved.getCopyrightHolder()).isEqualTo("Institute A");
            assertThat(saved.getRegistrationNo()).isEqualTo("2026SR000001");
            assertThat(saved.getRegistrationDate()).isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(saved.getSoftwareVersion()).isEqualTo("V1.0");
            assertThat(saved.getSoftwareCategory()).isEqualTo("应用软件");
        }
    }

    // ── submitCopyright ───────────────────────────────────────────────────

    @Test
    void testSubmitCopyright_shouldTransitionToPendingDeptReview() {
        // Arrange
        Copyright existing = new Copyright();
        existing.setId(1L);
        existing.setName("Test Software");
        existing.setCopyrightHolder("Institute A");
        existing.setRegistrationNo("2026SR000001");
        existing.setRegistrationDate(LocalDate.of(2026, 1, 15));
        existing.setSoftwareVersion("V1.0");
        existing.setSoftwareCategory("应用软件");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);
        existing.setDeptId(10L);

        when(copyrightMapper.selectById(1L)).thenReturn(existing);
        when(copyrightMapper.selectCount(any())).thenReturn(0L); // No duplicate
        when(copyrightMapper.updateById(any(Copyright.class))).thenReturn(1);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act
            copyrightService.submitCopyright(1L);

            // Assert
            ArgumentCaptor<Copyright> captor = ArgumentCaptor.forClass(Copyright.class);
            verify(copyrightMapper, atLeastOnce()).updateById(captor.capture());
            Copyright updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(AchievementStatusEnum.PENDING_DEPT_REVIEW.name());
        }
    }

    @Test
    void testSubmitDuplicateRegistrationNo_shouldThrow() {
        // Arrange
        Copyright existing = new Copyright();
        existing.setId(1L);
        existing.setName("Test Software");
        existing.setCopyrightHolder("Institute A");
        existing.setRegistrationNo("2026SR000001");
        existing.setRegistrationDate(LocalDate.of(2026, 1, 15));
        existing.setSoftwareVersion("V1.0");
        existing.setSoftwareCategory("应用软件");
        existing.setStatus(AchievementStatusEnum.DRAFT.name());
        existing.setCreatedBy(1L);

        when(copyrightMapper.selectById(1L)).thenReturn(existing);
        // Simulate duplicate registrationNo exists
        when(copyrightMapper.selectCount(any())).thenReturn(1L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> copyrightService.submitCopyright(1L))
                    .isInstanceOf(AchievementException.class)
                    .hasMessageContaining("E4001");
        }
    }

    // ── saveDraft ─────────────────────────────────────────────────────────

    @Test
    void testSaveDraft_shouldSetDraftStatus() {
        // Arrange
        CopyrightDTO dto = new CopyrightDTO();
        dto.setName("Draft Software");
        dto.setCopyrightHolder("Institute A");
        dto.setRegistrationNo("2026SR000001");
        dto.setRegistrationDate(LocalDate.of(2026, 1, 15));
        dto.setSoftwareVersion("V1.0");
        dto.setSoftwareCategory("应用软件");

        when(copyrightMapper.insert(any(Copyright.class))).thenAnswer(invocation -> {
            Copyright c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        });

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            securityUtils.when(SecurityUtils::getCurrentDeptId).thenReturn(10L);

            // Act
            Long id = copyrightService.saveDraft(dto);

            // Assert
            assertThat(id).isNotNull();

            ArgumentCaptor<Copyright> captor = ArgumentCaptor.forClass(Copyright.class);
            verify(copyrightMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AchievementStatusEnum.DRAFT.name());
        }
    }

    // ── pageCopyrights ────────────────────────────────────────────────────

    @Test
    void testPageCopyrights_shouldApplyFilters() {
        // Arrange
        when(copyrightMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = copyrightService.pageCopyrights(1, 20, "DRAFT", "keyword");

        // Assert
        assertThat(result).isNotNull();
        verify(copyrightMapper).selectPage(any(), any());
    }
}
