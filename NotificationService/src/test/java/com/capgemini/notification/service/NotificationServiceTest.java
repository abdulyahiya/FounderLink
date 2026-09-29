package com.capgemini.notification.service;

import com.capgemini.notification.dto.NotificationResponse;
import com.capgemini.notification.entity.Notification;
import com.capgemini.notification.enums.NotificationType;
import com.capgemini.notification.repository.NotificationRepository;
import com.capgemini.notification.mapper.NotificationMapper;
import com.capgemini.notification.service.impl.NotificationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .message("A new startup was created")
                .type(NotificationType.STARTUP_CREATED)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Default mapper stub
        when(notificationMapper.toResponse(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return NotificationResponse.builder()
                    .id(n.getId()).userId(n.getUserId()).message(n.getMessage())
                    .type(n.getType()).isRead(n.getIsRead()).createdAt(n.getCreatedAt())
                    .build();
        });
    }

    // -----------------------------------------------------------------------
    // createNotification
    // -----------------------------------------------------------------------

    @Test
    void createNotification_shouldSaveWithIsReadFalse() {
        // given
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // when
        notificationService.createNotification(10L, "A new startup was created", NotificationType.STARTUP_CREATED);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getMessage()).isEqualTo("A new startup was created");
        assertThat(saved.getType()).isEqualTo(NotificationType.STARTUP_CREATED);
        assertThat(saved.getIsRead()).isFalse();
    }

    // -----------------------------------------------------------------------
    // getNotificationsByUser
    // -----------------------------------------------------------------------

    @Test
    void getNotificationsByUser_shouldReturnList() {
        // given
        Notification second = Notification.builder()
                .id(2L)
                .userId(10L)
                .message("Investment was created")
                .type(NotificationType.INVESTMENT_CREATED)
                .isRead(true)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification, second));

        // when
        List<NotificationResponse> responses = notificationService.getNotificationsByUser(10L);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUserId()).isEqualTo(10L);
        assertThat(responses.get(0).getType()).isEqualTo(NotificationType.STARTUP_CREATED);
        assertThat(responses.get(1).getType()).isEqualTo(NotificationType.INVESTMENT_CREATED);
    }

    // -----------------------------------------------------------------------
    // getUnreadNotifications
    // -----------------------------------------------------------------------

    @Test
    void getUnreadNotifications_shouldReturnUnreadList() {
        // given — only the unread notification is returned
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification));

        // when
        List<NotificationResponse> responses = notificationService.getUnreadNotifications(10L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIsRead()).isFalse();
    }

    // -----------------------------------------------------------------------
    // markAsRead
    // -----------------------------------------------------------------------

    @Test
    void markAsRead_whenNotFound_shouldThrowEntityNotFoundException() {
        // given
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void markAsRead_whenFound_shouldSetIsReadTrue() {
        // given
        notification.setIsRead(false);
        Notification readNotification = Notification.builder()
                .id(1L)
                .userId(10L)
                .message("A new startup was created")
                .type(NotificationType.STARTUP_CREATED)
                .isRead(true)
                .createdAt(notification.getCreatedAt())
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(readNotification);

        // when
        NotificationResponse response = notificationService.markAsRead(1L);

        // then
        assertThat(response.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
        // isRead flag was set on the entity before save
        assertThat(notification.getIsRead()).isTrue();
    }
}
