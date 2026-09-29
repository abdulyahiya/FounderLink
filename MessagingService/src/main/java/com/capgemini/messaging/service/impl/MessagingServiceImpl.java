package com.capgemini.messaging.service.impl;

import com.capgemini.messaging.dto.ChatMessageDto;
import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.entity.Conversation;
import com.capgemini.messaging.entity.Message;
import com.capgemini.messaging.repository.ConversationRepository;
import com.capgemini.messaging.repository.MessageRepository;
import com.capgemini.messaging.mapper.MessageMapper;
import com.capgemini.messaging.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        Conversation conversation = conversationRepository
                .findByParticipants(senderId, request.getReceiverId())
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .participant1Id(senderId)
                                .participant2Id(request.getReceiverId())
                                .build()
                ));

        Message message = messageRepository.save(
                Message.builder()
                        .conversationId(conversation.getId())
                        .senderId(senderId)
                        .content(request.getContent())
                        .build()
        );

        MessageResponse response = messageMapper.toMessageResponse(message);

        // Broadcast to both participants subscribed on /topic/conversation/{id}
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversation.getId(),
                response
        );

        return response;
    }

    @Override
    public List<MessageResponse> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(messageMapper::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationResponse> getMyConversations(Long userId) {
        return conversationRepository.findAllByUserId(userId)
                .stream()
                .map(c -> messageMapper.toConversationResponse(c, List.of()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(Long userId, Long otherUserId) {
        Conversation conversation = conversationRepository
                .findByParticipants(userId, otherUserId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .participant1Id(userId)
                                .participant2Id(otherUserId)
                                .build()
                ));
        List<MessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream().map(messageMapper::toMessageResponse).collect(Collectors.toList());
        return messageMapper.toConversationResponse(conversation, messages);
    }

}
