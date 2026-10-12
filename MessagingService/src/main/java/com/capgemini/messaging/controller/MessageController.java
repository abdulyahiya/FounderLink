package com.capgemini.messaging.controller;

import com.capgemini.messaging.dto.ApiResponse;
import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.exception.ResourceNotFoundException;
import com.capgemini.messaging.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication,
            @Valid @RequestBody MessageRequest request) {
        Long senderId = resolveUserId(authentication, headerUserId);
        MessageResponse response = messagingService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", response));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(authentication, headerUserId);
        List<MessageResponse> messages = messagingService.getConversationMessages(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully", messages));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getMyConversations(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication) {
        Long userId = resolveUserId(authentication, headerUserId);
        List<ConversationResponse> conversations = messagingService.getMyConversations(userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            Authentication authentication,
            @RequestParam Long otherUserId) {
        Long userId = resolveUserId(authentication, headerUserId);
        ConversationResponse response = messagingService.getOrCreateConversation(userId, otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation ready", response));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = "Authentication required".equals(ex.getMessage()) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    private Long resolveUserId(Authentication authentication, Long headerUserId) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long l) {
                return l;
            }
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {
                if (headerUserId != null) {
                    return headerUserId;
                }
            }
        }
        throw new AccessDeniedException("Authentication required");
    }
}

