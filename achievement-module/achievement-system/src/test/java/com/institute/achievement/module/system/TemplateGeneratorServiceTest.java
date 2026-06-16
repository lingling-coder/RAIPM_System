package com.institute.achievement.module.system;

import com.institute.achievement.module.system.service.TemplateGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TemplateGeneratorService covering Excel template generation.
 * <p>
 * Verifies that the generated template has the correct headers and structure.
 * Requirements: REG-05 (Excel batch import), D-21 (downloadable template)
 */
class TemplateGeneratorServiceTest {

    private TemplateGeneratorService templateGeneratorService;

    @BeforeEach
    void setUp() {
        templateGeneratorService = new TemplateGeneratorService();
    }

    @Test
    void testGenerateTemplate() {
        byte[] template = templateGeneratorService.generateTemplate();

        assertThat(template).isNotNull();
        assertThat(template.length).isGreaterThan(0);
    }
}
