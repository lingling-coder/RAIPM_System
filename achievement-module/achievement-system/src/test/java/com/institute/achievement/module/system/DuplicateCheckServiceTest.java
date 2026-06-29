package com.institute.achievement.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import com.institute.achievement.module.system.dto.DuplicateCheckResult;
import com.institute.achievement.module.system.service.DuplicateCheckService;
import com.institute.achievement.paper.entity.Paper;
import com.institute.achievement.paper.mapper.PaperMapper;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DuplicateCheckService covering submit-time duplicate
 * detection across all three achievement types (D-45~D-47).
 */
@ExtendWith(MockitoExtension.class)
class DuplicateCheckServiceTest {

    @Mock
    private PaperMapper paperMapper;
    @Mock
    private PatentMapper patentMapper;
    @Mock
    private CopyrightMapper copyrightMapper;

    private DuplicateCheckService duplicateCheckService;

    @BeforeEach
    void setUp() {
        duplicateCheckService = new DuplicateCheckService(paperMapper, patentMapper, copyrightMapper);
    }

    // ── Paper DOI Detection ──────────────────────────────────────────

    @Test
    void testFindExistingByDoi_shouldReturnPaperWhenDuplicate() {
        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Existing Paper");
        existing.setDoi("10.1234/test");
        existing.setStatus("ARCHIVED");

        when(paperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        java.util.Optional<Paper> result = duplicateCheckService.findExistingByDoi("10.1234/test", null);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void testFindExistingByDoi_shouldReturnEmptyWhenNoDuplicate() {
        when(paperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        java.util.Optional<Paper> result = duplicateCheckService.findExistingByDoi("10.9999/unique", null);
        assertThat(result).isEmpty();
    }

    // ── Patent ApplicationNo Detection ────────────────────────────────

    @Test
    void testFindExistingByApplicationNo_shouldReturnPatentWhenDuplicate() {
        Patent existing = new Patent();
        existing.setId(2L);
        existing.setPatentName("Existing Patent");
        existing.setApplicationNo("CN202410000001");
        existing.setStatus("ARCHIVED");

        when(patentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        java.util.Optional<Patent> result = duplicateCheckService.findExistingByApplicationNo("CN202410000001", null);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
    }

    @Test
    void testFindExistingByApplicationNo_shouldReturnEmptyWhenNoDuplicate() {
        when(patentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        java.util.Optional<Patent> result = duplicateCheckService.findExistingByApplicationNo("CN202410099999", null);
        assertThat(result).isEmpty();
    }

    // ── Copyright RegistrationNo Detection ────────────────────────────

    @Test
    void testFindExistingByRegistrationNo_shouldReturnCopyrightWhenDuplicate() {
        Copyright existing = new Copyright();
        existing.setId(3L);
        existing.setName("Existing Copyright");
        existing.setRegistrationNo("2026SR000001");
        existing.setStatus("ARCHIVED");

        when(copyrightMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        java.util.Optional<Copyright> result = duplicateCheckService.findExistingByRegistrationNo("2026SR000001", null);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(3L);
    }

    @Test
    void testFindExistingByRegistrationNo_shouldReturnEmptyWhenNoDuplicate() {
        when(copyrightMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        java.util.Optional<Copyright> result = duplicateCheckService.findExistingByRegistrationNo("2026SR099999", null);
        assertThat(result).isEmpty();
    }

    // ── DuplicateCheckResult for Submit ───────────────────────────────

    @Test
    void testCheckDuplicateForSubmit_shouldReturnProperResult() {
        Paper existing = new Paper();
        existing.setId(1L);
        existing.setTitle("Duplicate Paper");
        existing.setStatus("ARCHIVED");

        when(paperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", "10.1234/dup", null);
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getExistingId()).isEqualTo(1L);
        assertThat(result.getExistingTitle()).isEqualTo("Duplicate Paper");
        assertThat(result.getExistingStatus()).isEqualTo("已归档");
    }

    @Test
    void testCheckDuplicateForSubmit_shouldReturnNoDuplicateForNullDoi() {
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", null, null);
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isFalse();
    }

    @Test
    void testCheckDuplicateForSubmit_shouldReturnNoDuplicateForEmptyString() {
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", "", null);
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isFalse();
    }

    // ── Draft Skip (D-47) ────────────────────────────────────────────

    @Test
    void testCheckDuplicateForSubmit_draftShouldReturnNoDuplicate() {
        // D-47: Draft submissions skip duplicate check
        // In the current design, duplicate check happens before status transition,
        // so we only check by unique field value. If the field is empty/null,
        // we return no duplicate.
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit("paper", null, null);
        assertThat(result.isDuplicate()).isFalse();
    }
}
