package com.institute.achievement.module.system.dto;

import lombok.Data;

/**
 * Role update DTO.
 */
@Data
public class RoleUpdateDTO {

    private Long id;

    private String roleName;

    private String description;

    /** 1=normal, 0=disabled */
    private Integer status;
}
