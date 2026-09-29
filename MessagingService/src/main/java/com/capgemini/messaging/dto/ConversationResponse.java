package com.capgemini.messaging.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationResponse {
    private Long id;
    private Long participant1Id;
    private Long participant2Id;
    private LocalDateTime createdAt;
    private List<MessageResponse> messages;
}
