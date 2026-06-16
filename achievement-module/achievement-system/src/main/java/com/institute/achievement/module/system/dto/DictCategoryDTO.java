package com.institute.achievement.module.system.dto;

import lombok.Data;

/**
 * Dictionary category DTO.
 */
@Data
public class DictCategoryDTO {

    private Long id;

    private String categoryName;

    private String categoryCode;

    private String description;

    private Integer sortOrder;

    /** 1=normal, 0=disabled */
    private Integer status;
}
