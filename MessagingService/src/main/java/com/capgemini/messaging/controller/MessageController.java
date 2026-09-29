package com.capgemini.messaging.controller;

import com.capgemini.messaging.dto.ApiResponse;
import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @RequestHeader("X-User-Id") Long senderId,
            @Valid @RequestBody MessageRequest request) {
        MessageResponse response = messagingService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", response));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId) {
        List<MessageResponse> messages = messagingService.getConversationMessages(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully", messages));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getMyConversations(
            @RequestHeader("X-User-Id") Long userId) {
        List<ConversationResponse> conversations = messagingService.getMyConversations(userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long otherUserId) {
        ConversationResponse response = messagingService.getOrCreateConversation(userId, otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation ready", response));
    }
}
