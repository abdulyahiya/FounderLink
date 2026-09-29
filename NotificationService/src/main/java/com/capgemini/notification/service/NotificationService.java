package com.capgemini.notification.service;

import com.capgemini.notification.dto.NotificationResponse;
import com.capgemini.notification.enums.NotificationType;
import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, String message, NotificationType type);
    List<NotificationResponse> getNotificationsByUser(Long userId);
    List<NotificationResponse> getUnreadNotifications(Long userId);
    NotificationResponse markAsRead(Long notificationId);
}
