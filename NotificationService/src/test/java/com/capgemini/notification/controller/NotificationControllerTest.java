package com.capgemini.notification.controller;

import com.capgemini.notification.config.JwtAuthenticationFilter;
import com.capgemini.notification.config.JwtUtil;
import com.capgemini.notification.dto.NotificationResponse;
import com.capgemini.notification.enums.NotificationType;
import com.capgemini.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    private UsernamePasswordAuthenticationToken userAuth;

    private NotificationResponse notificationResponse1;
    private NotificationResponse notificationResponse2;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked JWT filter pass through so requests reach the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        userAuth = new UsernamePasswordAuthenticationToken(
                10L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));

        notificationResponse1 = NotificationResponse.builder()
                .id(1L)
                .userId(10L)
                .message("A new startup was created")
                .type(NotificationType.STARTUP_CREATED)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationResponse2 = NotificationResponse.builder()
                .id(2L)
                .userId(10L)
                .message("Investment was created")
                .type(NotificationType.INVESTMENT_CREATED)
                .isRead(true)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    // -----------------------------------------------------------------------
    // GET /notifications/{userId}
    // -----------------------------------------------------------------------

    @Test
    void getNotifications_shouldReturn200WithList() throws Exception {
        // given
        when(notificationService.getNotificationsByUser(10L))
                .thenReturn(List.of(notificationResponse1, notificationResponse2));

        // when / then
        mockMvc.perform(get("/notifications/10")
                        .with(authentication(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Notifications fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].type").value("STARTUP_CREATED"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].type").value("INVESTMENT_CREATED"));
    }

    // -----------------------------------------------------------------------
    // GET /notifications/{userId}/unread
    // -----------------------------------------------------------------------

    @Test
    void getUnreadNotifications_shouldReturn200WithList() throws Exception {
        // given — only unread notifications returned
        when(notificationService.getUnreadNotifications(10L))
                .thenReturn(List.of(notificationResponse1));

        // when / then
        mockMvc.perform(get("/notifications/10/unread")
                        .with(authentication(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Unread notifications fetched"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data").isArray());
    }

    // -----------------------------------------------------------------------
    // PUT /notifications/{notificationId}/read
    // -----------------------------------------------------------------------

    @Test
    void markAsRead_shouldReturn200() throws Exception {
        // given
        NotificationResponse readResponse = NotificationResponse.builder()
                .id(1L)
                .userId(10L)
                .message("A new startup was created")
                .type(NotificationType.STARTUP_CREATED)
                .isRead(true)
                .createdAt(notificationResponse1.getCreatedAt())
                .build();
        when(notificationService.markAsRead(1L)).thenReturn(readResponse);

        // when / then
        mockMvc.perform(put("/notifications/1/read")
                        .with(authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Notification marked as read"))
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void markAsRead_whenNotFound_shouldPropagateEntityNotFoundException() {
        // given — EntityNotFoundException has no dedicated handler in @WebMvcTest
        when(notificationService.markAsRead(99L))
                .thenThrow(new EntityNotFoundException("Notification not found"));

        // when / then — exception propagates through MockMvc since no @ControllerAdvice handles it
        assertThatThrownBy(() ->
                mockMvc.perform(put("/notifications/99/read")
                        .with(authentication(userAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())))
                .hasCauseInstanceOf(EntityNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // GET /notifications/{userId} — unauthenticated
    // -----------------------------------------------------------------------

    @Test
    void getNotifications_withoutAuth_shouldReturn401() throws Exception {
        // when / then — no authentication token provided
        mockMvc.perform(get("/notifications/10"))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).getNotificationsByUser(any());
    }
}
