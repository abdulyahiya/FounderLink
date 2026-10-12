package com.capgemini.messaging.controller;

import com.capgemini.messaging.config.JwtAuthenticationFilter;
import com.capgemini.messaging.config.JwtUtil;
import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.service.MessagingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import com.capgemini.messaging.exception.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessagingService messagingService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MessageRequest messageRequest;
    private MessageResponse messageResponse;
    private ConversationResponse conversationResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked JWT filter pass through so requests reach the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        messageRequest = new MessageRequest(2L, "Hello!");

        messageResponse = MessageResponse.builder()
                .id(200L)
                .conversationId(100L)
                .senderId(1L)
                .content("Hello!")
                .createdAt(LocalDateTime.now())
                .build();

        conversationResponse = ConversationResponse.builder()
                .id(100L)
                .participant1Id(1L)
                .participant2Id(2L)
                .createdAt(LocalDateTime.now())
                .messages(List.of())
                .build();
    }

    // ── POST /messages ────────────────────────────────────────────────────────

    @Test
    void sendMessage_withValidHeader_shouldReturn200() throws Exception {
        // given
        given(messagingService.sendMessage(eq(1L), any(MessageRequest.class)))
                .willReturn(messageResponse);

        // when / then
        mockMvc.perform(post("/messages")
                        .with(user("test"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Message sent successfully"))
                .andExpect(jsonPath("$.data.senderId").value(1))
                .andExpect(jsonPath("$.data.content").value("Hello!"));
    }

    // ── GET /messages/conversation/{conversationId} ────────────────────────────

    @Test
    void getConversationMessages_asParticipant_shouldReturn200() throws Exception {
        // given
        given(messagingService.getConversationMessages(eq(100L), eq(1L)))
                .willReturn(List.of(messageResponse));

        // when / then
        mockMvc.perform(get("/messages/conversation/100")
                        .with(user("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Messages fetched successfully"))
                .andExpect(jsonPath("$.data[0].conversationId").value(100))
                .andExpect(jsonPath("$.data[0].content").value("Hello!"));
    }

    @Test
    void getConversationMessages_asNonParticipant_shouldReturn403() throws Exception {
        // given
        given(messagingService.getConversationMessages(eq(100L), eq(99L)))
                .willThrow(new AccessDeniedException("Access denied: You are not a participant in this conversation"));

        // when / then
        mockMvc.perform(get("/messages/conversation/100")
                        .with(user("99")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You are not a participant in this conversation"));
    }

    @Test
    void getConversationMessages_unauthenticated_shouldReturn401() throws Exception {
        // when / then - no user credentials provided, Spring Security rejects with 401
        mockMvc.perform(get("/messages/conversation/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getConversationMessages_changingConversationId_nonParticipant_shouldReturn403() throws Exception {
        // given - user 1 attempts to access conversation 200 where they are not a participant
        given(messagingService.getConversationMessages(eq(200L), eq(1L)))
                .willThrow(new AccessDeniedException("Access denied: You are not a participant in this conversation"));

        // when / then
        mockMvc.perform(get("/messages/conversation/200")
                        .with(user("1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You are not a participant in this conversation"));
    }

    @Test
    void getConversationMessages_whenNotFound_shouldReturn404() throws Exception {
        // given
        given(messagingService.getConversationMessages(eq(999L), eq(1L)))
                .willThrow(new ResourceNotFoundException("Conversation not found"));

        // when / then
        mockMvc.perform(get("/messages/conversation/999")
                        .with(user("1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Conversation not found"));
    }

    // ── GET /messages/conversations ────────────────────────────────────────────

    @Test
    void getMyConversations_withHeader_shouldReturn200() throws Exception {
        // given
        given(messagingService.getMyConversations(1L))
                .willReturn(List.of(conversationResponse));

        // when / then
        mockMvc.perform(get("/messages/conversations")
                        .with(user("test"))
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Conversations fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].participant1Id").value(1));
    }

    // ── POST /messages/conversations ───────────────────────────────────────────

    @Test
    void startConversation_shouldReturn200() throws Exception {
        // given
        given(messagingService.getOrCreateConversation(1L, 2L))
                .willReturn(conversationResponse);

        // when / then
        mockMvc.perform(post("/messages/conversations")
                        .with(user("test"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("X-User-Id", "1")
                        .param("otherUserId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Conversation ready"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.participant2Id").value(2));
    }
}
