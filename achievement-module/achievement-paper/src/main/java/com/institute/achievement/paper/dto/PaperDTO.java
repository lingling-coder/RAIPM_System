package com.institute.achievement.paper.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Paper create/update DTO — input for PaperService.
 * <p>
 * Validation rules per UI-SPEC §5.1.1 and RESEARCH.md lines 855-870.
 */
@Data
public class PaperDTO {

    @NotBlank(message = "论文标题不能为空")
    @Size(max = 500, message = "标题不超过500字")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String authors;

    @NotBlank(message = "期刊/会议名称不能为空")
    private String journal;

    @Pattern(regexp = "^(10\\.\\d{4,}\\/.*)?$", message = "DOI格式不正确")
    private String doi;

    private String issn;

    @Min(1)
    private Integer volume;

    @Min(1)
    private Integer issue;

    private String pages;

    @NotNull(message = "发表年份不能为空")
    @Min(1900)
    @Max(2100)
    private Integer publishYear;

    @NotBlank(message = "收录情况不能为空")
    private String indexStatus;

    @Digits(integer = 7, fraction = 3)
    private BigDecimal impactFactor;

    private String zone;

    @Size(max = 2000, message = "摘要不超过2000字")
    private String abstractText;

    // ── Classification (D-06) ───────────────────────────────────────

    private Integer isClassified;
    private String classifiedLevel;

    // ── Project Linkage (D-08) ──────────────────────────────────────

    private String projectRef;
}
