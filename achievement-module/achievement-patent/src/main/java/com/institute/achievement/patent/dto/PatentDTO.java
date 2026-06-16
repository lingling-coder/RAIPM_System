package com.institute.achievement.patent.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Patent create/update DTO — input for PatentService.
 * <p>
 * Validation rules per UI-SPEC §5.1.2.
 */
@Data
public class PatentDTO {

    @NotBlank(message = "专利名称不能为空")
    @Size(max = 500, message = "专利名称不超过500字")
    private String patentName;

    @NotBlank(message = "发明人不能为空")
    private String inventors;

    @NotBlank(message = "申请号不能为空")
    @Size(max = 100)
    private String applicationNo;

    private String authorizationNo;

    @NotNull(message = "申请日不能为空")
    private LocalDate applicationDate;

    private LocalDate authorizationDate;

    @NotBlank(message = "专利类型不能为空")
    private String patentType;

    @NotBlank(message = "国别不能为空")
    private String country;

    private LocalDate nextFeeDate;

    @NotBlank(message = "法律状态不能为空")
    private String legalStatus;

    // ── Classification (D-06) ───────────────────────────────────────

    private Integer isClassified;
    private String classifiedLevel;

    // ── Project Linkage (D-08) ──────────────────────────────────────

    private String projectRef;
}
