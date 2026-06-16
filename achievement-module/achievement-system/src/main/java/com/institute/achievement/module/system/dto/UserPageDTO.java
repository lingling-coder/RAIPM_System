package com.institute.achievement.module.system.dto;

import com.institute.achievement.common.util.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User pagination query DTO.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageDTO extends PageQuery {

    /** Search keyword for username/realName */
    private String keyword;

    /** Filter by department */
    private Long deptId;

    /** Filter by role */
    private Long roleId;

    /** Filter by status: 1=normal, 0=disabled */
    private Integer status;
}
