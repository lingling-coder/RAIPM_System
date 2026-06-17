package com.institute.achievement.reminder.util;

import com.institute.achievement.reminder.enums.ReminderTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ReminderTemplateUtil covering variable substitution and
 * utility methods (RMD-03).
 */
class TemplateSubstitutionTest {

    @Test
    void substituteShouldReplaceAchievementName() {
        Map<String, String> variables = new HashMap<>();
        variables.put("achievementName", "高性能芯片设计");

        String result = ReminderTemplateUtil.substitute(
                "提醒：{achievementName}申报截止日期临近", variables);

        assertThat(result).isEqualTo("提醒：高性能芯片设计申报截止日期临近");
    }

    @Test
    void substituteShouldReplaceMultipleVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("responsiblePerson", "张三");
        variables.put("achievementName", "高性能芯片设计");

        String result = ReminderTemplateUtil.substitute(
                "您好，{responsiblePerson}，{achievementName}", variables);

        assertThat(result).isEqualTo("您好，张三，高性能芯片设计");
    }

    @Test
    void substituteShouldLeaveUnmatchedPlaceholdersUnchanged() {
        Map<String, String> variables = new HashMap<>();
        variables.put("achievementName", "测试成果");

        String result = ReminderTemplateUtil.substitute(
                "提醒：{achievementName}，{unknownVariable}", variables);

        assertThat(result).isEqualTo("提醒：测试成果，{unknownVariable}");
    }

    @Test
    void substituteWithNullVariablesShouldReturnOriginal() {
        String result = ReminderTemplateUtil.substitute("原始模板", null);
        assertThat(result).isEqualTo("原始模板");
    }

    @Test
    void substituteWithNullTemplateShouldReturnNull() {
        Map<String, String> variables = new HashMap<>();
        variables.put("key", "value");

        String result = ReminderTemplateUtil.substitute(null, variables);
        assertThat(result).isNull();
    }

    @Test
    void buildTitleShouldProduceCorrectOutput() {
        String title = ReminderTemplateUtil.buildTitle(
                ReminderTypeEnum.PROJECT_APPLICATION, "高性能芯片设计");

        assertThat(title).isEqualTo("项目申报提醒 — 高性能芯片设计");
    }

    @Test
    void buildContentShouldProduceCorrectOutput() {
        String content = ReminderTemplateUtil.buildContent(
                ReminderTypeEnum.PATENT_ANNUAL_FEE,
                "张三", "高性能芯片设计",
                "2026-08-15", "15");

        assertThat(content)
                .contains("张三")
                .contains("高性能芯片设计")
                .contains("2026-08-15")
                .contains("15")
                .contains("逾期将产生滞纳金");
    }

    @Test
    void buildTitleWithNullTypeShouldReturnEmpty() {
        String title = ReminderTemplateUtil.buildTitle(null, "test");
        assertThat(title).isEmpty();
    }
}
