package com.institute.achievement.reminder.util;

import com.institute.achievement.reminder.enums.ReminderTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.Map;

/**
 * Utility for reminder template variable substitution (D-23).
 * <p>
 * Replaces {variableName} placeholders in title and body templates
 * with actual values. Supports variables:
 * <ul>
 *   <li>{@code {achievementName}} — achievement/certificate name</li>
 *   <li>{@code {deadline}} — deadline date (formatted string)</li>
 *   <li>{@code {daysRemaining}} — days remaining until deadline</li>
 *   <li>{@code {responsiblePerson}} — responsible person's name</li>
 * </ul>
 */
@UtilityClass
public class ReminderTemplateUtil {

    /**
     * Substitute {variableName} placeholders in a template string.
     * <p>
     * Iterates over the variable map and replaces each key's placeholder
     * with its value. Unmatched placeholders are left unchanged.
     *
     * @param template  the template string with {variableName} placeholders
     * @param variables map of variable names to values (without curly braces)
     * @return the substituted string
     */
    public static String substitute(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * Build a notification title from the type's default template.
     *
     * @param type            the reminder type enum
     * @param achievementName the achievement name
     * @return the rendered title string
     */
    public static String buildTitle(ReminderTypeEnum type, String achievementName) {
        if (type == null) {
            return "";
        }
        return type.getDefaultTitleTemplate()
                .replace("{achievementName}", achievementName != null ? achievementName : "");
    }

    /**
     * Build a notification body from the type's default template.
     *
     * @param type              the reminder type enum
     * @param responsiblePerson the responsible person's name
     * @param achievementName   the achievement name
     * @param deadline          the deadline date as string
     * @param daysRemaining     the days remaining as string
     * @return the rendered body string
     */
    public static String buildContent(ReminderTypeEnum type, String responsiblePerson,
                                      String achievementName, String deadline,
                                      String daysRemaining) {
        if (type == null) {
            return "";
        }
        return type.getDefaultBodyTemplate()
                .replace("{responsiblePerson}", responsiblePerson != null ? responsiblePerson : "")
                .replace("{achievementName}", achievementName != null ? achievementName : "")
                .replace("{deadline}", deadline != null ? deadline : "")
                .replace("{daysRemaining}", daysRemaining != null ? daysRemaining : "");
    }
}
