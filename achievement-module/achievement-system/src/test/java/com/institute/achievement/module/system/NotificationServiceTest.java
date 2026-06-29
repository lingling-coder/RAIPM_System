package com.institute.achievement.module.system;

import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.module.system.entity.Notification;
import com.institute.achievement.module.system.mapper.NotificationMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.module.system.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService covering create, mark-as-read,
 * unread count, and cleanup operations.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationMapper, redisTemplate, sysUserMapper);
    }

    @Test
    void testSendNotification_shouldCreateRecordAndIncrementRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return 0;
        }).when(notificationMapper).insert(any(Notification.class));

        Long id = notificationService.send(1L, "APPROVAL", "审批通知",
                "您的成果已提交", "paper", 100L);

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo("APPROVAL");
        assertThat(saved.getTitle()).isEqualTo("审批通知");
        assertThat(saved.getReadFlag()).isZero();

        verify(valueOperations).increment("notify:unread:1");
    }

    @Test
    void testMarkAsRead_shouldUpdateFlagAndDecrementRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setReadFlag(0);
        notification.setType("APPROVAL");
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        notificationService.markAsRead(1L, 1L);

        verify(notificationMapper).markAsRead(1L);
        verify(valueOperations).decrement("notify:unread:1");
    }

    @Test
    void testMarkAsRead_alreadyRead_shouldNotDecrement() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(1L);
        notification.setReadFlag(1);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        notificationService.markAsRead(1L, 1L);

        verify(notificationMapper, never()).markAsRead(anyLong());
    }

    @Test
    void testMarkAsRead_wrongUser_shouldThrow() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(2L);
        notification.setReadFlag(0);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
                .isInstanceOf(AchievementException.class);
    }

    @Test
    void testMarkAsRead_notFound_shouldThrow() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(AchievementException.class);
    }

    @Test
    void testGetUnreadCount_fromRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notify:unread:1")).thenReturn("5");

        Long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
        verify(notificationMapper, never()).countUnread(anyLong());
    }

    @Test
    void testGetUnreadCount_fromDbFallback() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notify:unread:1")).thenReturn(null);
        when(notificationMapper.countUnread(1L)).thenReturn(3L);

        Long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(3L);
        verify(valueOperations).set(eq("notify:unread:1"), eq("3"), anyLong(), any());
    }

    @Test
    void testGetUnreadCount_zeroFromDb() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notify:unread:1")).thenReturn(null);
        when(notificationMapper.countUnread(1L)).thenReturn(0L);

        Long count = notificationService.getUnreadCount(1L);

        assertThat(count).isZero();
    }

    @Test
    void testCleanupOlderThan_shouldDeleteInBatches() {
        when(notificationMapper.deleteOlderThan(any())).thenReturn(1500, 1000, 500);

        notificationService.cleanupOldNotifications();

        verify(notificationMapper, times(3)).deleteOlderThan(any());
    }

    @Test
    void testMarkAllAsRead_shouldResetRedisCount() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(notificationMapper.markAllAsRead(1L)).thenReturn(5);

        int count = notificationService.markAllAsRead(1L);

        assertThat(count).isEqualTo(5);
        verify(valueOperations).set("notify:unread:1", "0");
    }
}
