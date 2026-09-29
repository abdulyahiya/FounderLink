package com.capgemini.investment.event;


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

    @Value("${rabbitmq.routing-key.investment-created}")
    private String investmentCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.investment-approved}")
    private String investmentApprovedRoutingKey;

    public void publishInvestmentCreated(InvestmentCreatedEvent event) {
        log.info("Publishing INVESTMENT_CREATED event for investment: {}", event.getInvestmentId());
        rabbitTemplate.convertAndSend(exchange, investmentCreatedRoutingKey, event);
    }

    public void publishInvestmentApproved(InvestmentApprovedEvent event) {
        log.info("Publishing INVESTMENT_APPROVED event for investment: {}", event.getInvestmentId());
        rabbitTemplate.convertAndSend(exchange, investmentApprovedRoutingKey, event);
    }
}
