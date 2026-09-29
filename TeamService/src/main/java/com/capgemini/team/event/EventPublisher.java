package com.capgemini.team.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.team-invite-sent}")
    private String teamInviteSentRoutingKey;

    public void publishTeamInviteSent(TeamInviteSentEvent event) {
        log.info("Publishing TEAM_INVITE_SENT event for invitation: {}", event.getInvitationId());
        rabbitTemplate.convertAndSend(exchange, teamInviteSentRoutingKey, event);
    }
}
