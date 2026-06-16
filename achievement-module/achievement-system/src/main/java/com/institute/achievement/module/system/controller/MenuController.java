package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.MenuTreeNode;
import com.institute.achievement.module.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Menu REST controller.
 * Provides menu tree for permission assignment UI.
 */
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * Get full menu tree.
     */
    @GetMapping("/tree")
    public Result<List<MenuTreeNode>> getTree() {
        return Result.success(menuService.buildTree());
    }
}
