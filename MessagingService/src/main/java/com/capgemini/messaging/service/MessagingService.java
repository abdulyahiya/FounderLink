package com.capgemini.messaging.service;

import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import java.util.List;

public interface MessagingService {
    MessageResponse sendMessage(Long senderId, MessageRequest request);
    List<MessageResponse> getConversationMessages(Long conversationId);
    List<ConversationResponse> getMyConversations(Long userId);
    ConversationResponse getOrCreateConversation(Long userId, Long otherUserId);
}
