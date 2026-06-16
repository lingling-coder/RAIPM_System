package com.institute.achievement.module.system.dto;

import lombok.Data;

/**
 * Department update DTO.
 */
@Data
public class DepartmentUpdateDTO {

    private Long id;

    private String deptName;

    private String deptCode;

    private String leader;

    private String phone;

    /** 1=normal, 0=disabled */
    private Integer status;
}
