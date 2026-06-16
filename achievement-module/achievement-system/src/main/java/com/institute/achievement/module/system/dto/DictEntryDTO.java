package com.institute.achievement.module.system.dto;

import lombok.Data;

/**
 * Dictionary entry DTO.
 */
@Data
public class DictEntryDTO {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String dictKey;

    private String dictValue;

    private Integer sortOrder;

    /** 1=normal, 0=disabled */
    private Integer status;

    private java.time.LocalDateTime createdAt;
}
