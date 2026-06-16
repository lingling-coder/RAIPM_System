package com.institute.achievement.copyright.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Software copyright create/update DTO — input for CopyrightService.
 * <p>
 * Validation rules per UI-SPEC §5.1.3.
 */
@Data
public class CopyrightDTO {

    @NotBlank(message = "软著名称不能为空")
    @Size(max = 500, message = "软著名称不超过500字")
    private String name;

    @NotBlank(message = "著作权人不能为空")
    private String copyrightHolder;

    @NotBlank(message = "登记号不能为空")
    @Size(max = 100)
    private String registrationNo;

    @NotNull(message = "登记日期不能为空")
    private LocalDate registrationDate;

    @NotBlank(message = "版本号不能为空")
    @Size(max = 100)
    private String softwareVersion;

    @NotBlank(message = "软件类别不能为空")
    private String softwareCategory;

    // ── Classification (D-06) ───────────────────────────────────────

    private Integer isClassified;
    private String classifiedLevel;

    // ── Project Linkage (D-08) ──────────────────────────────────────

    private String projectRef;
}
