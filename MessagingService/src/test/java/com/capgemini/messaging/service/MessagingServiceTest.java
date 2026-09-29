package com.capgemini.messaging.service;

import com.capgemini.messaging.dto.ConversationResponse;
import com.capgemini.messaging.dto.MessageRequest;
import com.capgemini.messaging.dto.MessageResponse;
import com.capgemini.messaging.entity.Conversation;
import com.capgemini.messaging.entity.Message;
import com.capgemini.messaging.repository.ConversationRepository;
import com.capgemini.messaging.repository.MessageRepository;
import com.capgemini.messaging.mapper.MessageMapper;
import com.capgemini.messaging.service.impl.MessagingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessagingServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessagingServiceImpl messagingService;

    private static final Long SENDER_ID   = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long CONV_ID     = 100L;

    private Conversation existingConversation;
    private Message savedMessage;
    private MessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        existingConversation = Conversation.builder()
                .id(CONV_ID)
                .participant1Id(SENDER_ID)
                .participant2Id(RECEIVER_ID)
                .createdAt(LocalDateTime.now())
                .build();

        messageRequest = new MessageRequest(RECEIVER_ID, "Hello!");

        savedMessage = Message.builder()
                .id(200L)
                .conversationId(CONV_ID)
                .senderId(SENDER_ID)
                .content("Hello!")
                .createdAt(LocalDateTime.now())
                .build();

        // Default mapper stubs
        when(messageMapper.toMessageResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return MessageResponse.builder()
                    .id(m.getId()).conversationId(m.getConversationId())
                    .senderId(m.getSenderId()).content(m.getContent())
                    .createdAt(m.getCreatedAt()).build();
        });
        when(messageMapper.toConversationResponse(any(Conversation.class), anyList())).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            List<MessageResponse> msgs = inv.getArgument(1);
            return ConversationResponse.builder()
                    .id(c.getId()).participant1Id(c.getParticipant1Id())
                    .participant2Id(c.getParticipant2Id()).createdAt(c.getCreatedAt())
                    .messages(msgs).build();
        });
    }

    // ── sendMessage ────────────────────────────────────────────────────────────

    @Test
    void sendMessage_whenConversationExists_shouldSendMessage() {
        // given
        given(conversationRepository.findByParticipants(SENDER_ID, RECEIVER_ID))
                .willReturn(Optional.of(existingConversation));
        given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

        // when
        MessageResponse response = messagingService.sendMessage(SENDER_ID, messageRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getConversationId()).isEqualTo(CONV_ID);
        assertThat(response.getSenderId()).isEqualTo(SENDER_ID);
        assertThat(response.getContent()).isEqualTo("Hello!");
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_whenConversationDoesNotExist_shouldCreateConversationAndSend() {
        // given
        Conversation newConversation = Conversation.builder()
                .id(CONV_ID)
                .participant1Id(SENDER_ID)
                .participant2Id(RECEIVER_ID)
                .createdAt(LocalDateTime.now())
                .build();

        given(conversationRepository.findByParticipants(SENDER_ID, RECEIVER_ID))
                .willReturn(Optional.empty());
        given(conversationRepository.save(any(Conversation.class))).willReturn(newConversation);
        given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

        // when
        MessageResponse response = messagingService.sendMessage(SENDER_ID, messageRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getConversationId()).isEqualTo(CONV_ID);
        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository).save(any(Message.class));
    }

    // ── getConversationMessages ────────────────────────────────────────────────

    @Test
    void getConversationMessages_shouldReturnMessagesInOrder() {
        // given
        Message msg1 = Message.builder().id(1L).conversationId(CONV_ID).senderId(SENDER_ID)
                .content("Hi").createdAt(LocalDateTime.now().minusMinutes(2)).build();
        Message msg2 = Message.builder().id(2L).conversationId(CONV_ID).senderId(RECEIVER_ID)
                .content("Hey").createdAt(LocalDateTime.now().minusMinutes(1)).build();

        given(messageRepository.findByConversationIdOrderByCreatedAtAsc(CONV_ID))
                .willReturn(List.of(msg1, msg2));

        // when
        List<MessageResponse> responses = messagingService.getConversationMessages(CONV_ID);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getContent()).isEqualTo("Hi");
        assertThat(responses.get(1).getContent()).isEqualTo("Hey");
        verify(messageRepository).findByConversationIdOrderByCreatedAtAsc(CONV_ID);
    }

    // ── getMyConversations ─────────────────────────────────────────────────────

    @Test
    void getMyConversations_shouldReturnAllConversationsForUser() {
        // given
        Conversation conv2 = Conversation.builder()
                .id(101L).participant1Id(SENDER_ID).participant2Id(3L)
                .createdAt(LocalDateTime.now()).build();

        given(conversationRepository.findAllByUserId(SENDER_ID))
                .willReturn(List.of(existingConversation, conv2));

        // when
        List<ConversationResponse> responses = messagingService.getMyConversations(SENDER_ID);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(CONV_ID);
        assertThat(responses.get(1).getId()).isEqualTo(101L);
        // getMyConversations does NOT load messages (includeMessages = false)
        assertThat(responses.get(0).getMessages()).isEmpty();
        verify(conversationRepository).findAllByUserId(SENDER_ID);
    }

    // ── getOrCreateConversation ────────────────────────────────────────────────

    @Test
    void getOrCreateConversation_whenExists_shouldReturnExisting() {
        // given
        given(conversationRepository.findByParticipants(SENDER_ID, RECEIVER_ID))
                .willReturn(Optional.of(existingConversation));
        given(messageRepository.findByConversationIdOrderByCreatedAtAsc(CONV_ID))
                .willReturn(List.of(savedMessage));

        // when
        ConversationResponse response = messagingService.getOrCreateConversation(SENDER_ID, RECEIVER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(CONV_ID);
        assertThat(response.getParticipant1Id()).isEqualTo(SENDER_ID);
        assertThat(response.getParticipant2Id()).isEqualTo(RECEIVER_ID);
        // getOrCreateConversation includes messages (includeMessages = true)
        assertThat(response.getMessages()).hasSize(1);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversation_whenNotExists_shouldCreateAndReturn() {
        // given
        Conversation newConversation = Conversation.builder()
                .id(CONV_ID)
                .participant1Id(SENDER_ID)
                .participant2Id(RECEIVER_ID)
                .createdAt(LocalDateTime.now())
                .build();

        given(conversationRepository.findByParticipants(SENDER_ID, RECEIVER_ID))
                .willReturn(Optional.empty());
        given(conversationRepository.save(any(Conversation.class))).willReturn(newConversation);
        given(messageRepository.findByConversationIdOrderByCreatedAtAsc(CONV_ID))
                .willReturn(List.of());

        // when
        ConversationResponse response = messagingService.getOrCreateConversation(SENDER_ID, RECEIVER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(CONV_ID);
        assertThat(response.getMessages()).isEmpty();
        verify(conversationRepository).save(any(Conversation.class));
    }
}
