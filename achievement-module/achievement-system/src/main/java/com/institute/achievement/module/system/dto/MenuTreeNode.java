package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * Menu tree node for permission assignment UI (P-04).
 * Used to build a recursive tree structure with checked state for role permission assignment.
 */
@Data
public class MenuTreeNode {

    private Long id;

    private Long parentId;

    /** Node display label (menu name) */
    private String label;

    /** Permission identifier */
    private String permission;

    /** 0=directory, 1=menu, 2=button */
    private Integer type;

    /** Element Plus icon name */
    private String icon;

    private Integer sortOrder;

    /** Child nodes (recursive) */
    private List<MenuTreeNode> children;

    /** Whether this node is checked for the current role */
    private Boolean checked;
}
