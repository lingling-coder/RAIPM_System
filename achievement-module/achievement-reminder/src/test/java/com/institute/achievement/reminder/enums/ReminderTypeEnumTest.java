package com.institute.achievement.reminder.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ReminderTypeEnum and UrgencyLevelEnum.
 * <p>
 * Covers RMD-01: enum values, field validity, and lookup methods.
 */
class ReminderTypeEnumTest {

    // ── ReminderTypeEnum Tests ──────────────────────────────────────────

    @Test
    void shouldHaveExactlySixValues() {
        assertThat(ReminderTypeEnum.values()).hasSize(6);
    }

    @Test
    void eachEnumValueShouldHaveNonNullFields() {
        for (ReminderTypeEnum value : ReminderTypeEnum.values()) {
            assertThat(value.getCode()).as("Code for %s", value.name()).isNotNull();
            assertThat(value.getLabel()).as("Label for %s", value.name()).isNotNull();
            assertThat(value.getDefaultAdvanceDays()).as("defaultAdvanceDays for %s", value.name()).isPositive();
            assertThat(value.getDefaultUrgency()).as("defaultUrgency for %s", value.name()).isNotNull();
            assertThat(value.getDefaultTitleTemplate()).as("defaultTitleTemplate for %s", value.name()).isNotNull();
            assertThat(value.getDefaultBodyTemplate()).as("defaultBodyTemplate for %s", value.name()).isNotNull();
        }
    }

    @Test
    void fromCodeShouldReturnCorrectEnumForProjectApplication() {
        ReminderTypeEnum result = ReminderTypeEnum.fromCode("PROJECT_APPLICATION");
        assertThat(result).isEqualTo(ReminderTypeEnum.PROJECT_APPLICATION);
    }

    @Test
    void fromCodeShouldReturnNullForInvalidCode() {
        ReminderTypeEnum result = ReminderTypeEnum.fromCode("INVALID");
        assertThat(result).isNull();
    }

    // ── UrgencyLevelEnum Tests ──────────────────────────────────────────

    @Test
    void urgencyLevelShouldHaveExactlyThreeValues() {
        assertThat(UrgencyLevelEnum.values()).hasSize(3);
    }

    @Test
    void fromCodeShouldReturnHighForHighCode() {
        UrgencyLevelEnum result = UrgencyLevelEnum.fromCode("HIGH");
        assertThat(result).isEqualTo(UrgencyLevelEnum.HIGH);
    }

    @Test
    void fromCodeShouldReturnNullForInvalidUrgencyCode() {
        UrgencyLevelEnum result = UrgencyLevelEnum.fromCode("INVALID");
        assertThat(result).isNull();
    }

    @Test
    void highUrgencyShouldEscalateSevenDaysBeforeDeadline() {
        assertThat(UrgencyLevelEnum.HIGH.getEscalationDaysBeforeDeadline()).isEqualTo(7);
    }

    @Test
    void lowUrgencyShouldNotEscalate() {
        assertThat(UrgencyLevelEnum.LOW.getEscalationDaysBeforeDeadline()).isNegative();
    }
}
