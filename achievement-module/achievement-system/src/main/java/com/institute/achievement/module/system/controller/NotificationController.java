package com.institute.achievement.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.entity.Notification;
import com.institute.achievement.module.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for notification operations.
 * Provides endpoints for listing, marking as read, and getting unread counts.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * List notifications by type with pagination.
     * GET /api/notifications?type=APPROVAL&page=1&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "APPROVAL") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        IPage<Notification> result = notificationService.listByUser(userId, type, page, size);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of("records", result.getRecords(), "total", result.getTotal(),
                        "size", result.getSize(), "current", result.getCurrent())
        ));
    }

    /**
     * Get unread notification count.
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(Map.of("code", 200, "data", 0));
        }
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", count));
    }

    /**
     * Mark a single notification as read.
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已标记为已读"));
    }

    /**
     * Mark all notifications as read for current user.
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "全部标记为已读"));
    }
}
