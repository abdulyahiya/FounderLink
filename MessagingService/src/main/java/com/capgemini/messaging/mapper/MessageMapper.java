package com.capgemini.messaging.mapper;

import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.entity.Conversation;
import com.capgemini.messaging.entity.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageMapper {

    public MessageResponse toMessageResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public ConversationResponse toConversationResponse(Conversation c, List<MessageResponse> messages) {
        return ConversationResponse.builder()
                .id(c.getId())
                .participant1Id(c.getParticipant1Id())
                .participant2Id(c.getParticipant2Id())
                .createdAt(c.getCreatedAt())
                .messages(messages)
                .build();
    }
}
