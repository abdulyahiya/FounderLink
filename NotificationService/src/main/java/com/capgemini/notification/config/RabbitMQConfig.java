package com.capgemini.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.investment-created}")
    private String investmentCreatedQueue;

    @Value("${rabbitmq.queue.investment-approved}")
    private String investmentApprovedQueue;

    @Value("${rabbitmq.queue.team-invite-sent}")
    private String teamInviteSentQueue;

    @Value("${rabbitmq.queue.startup-created}")
    private String startupCreatedQueue;

    @Value("${rabbitmq.queue.startup-rejected}")
    private String startupRejectedQueue;

    @Value("${rabbitmq.queue.payment-success}")
    private String paymentSuccessQueue;

    @Value("${rabbitmq.queue.payment-failed}")
    private String paymentFailedQueue;

    @Value("${rabbitmq.queue.user-registered}")
    private String userRegisteredQueue;

    @Bean
    public TopicExchange founderLinkExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue investmentCreatedQueue() {
        return QueueBuilder.durable(investmentCreatedQueue).build();
    }

    @Bean
    public Queue investmentApprovedQueue() {
        return QueueBuilder.durable(investmentApprovedQueue).build();
    }

    @Bean
    public Queue teamInviteSentQueue() {
        return QueueBuilder.durable(teamInviteSentQueue).build();
    }

    @Bean
    public Queue startupCreatedQueue() {
        return QueueBuilder.durable(startupCreatedQueue).build();
    }

    @Bean
    public Queue startupRejectedQueue() {
        return QueueBuilder.durable(startupRejectedQueue).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(paymentSuccessQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue).build();
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(userRegisteredQueue).build();
    }

    @Bean
    public Binding investmentCreatedBinding() {
        return BindingBuilder.bind(investmentCreatedQueue()).to(founderLinkExchange()).with("investment.created");
    }

    @Bean
    public Binding investmentApprovedBinding() {
        return BindingBuilder.bind(investmentApprovedQueue()).to(founderLinkExchange()).with("investment.approved");
    }

    @Bean
    public Binding teamInviteSentBinding() {
        return BindingBuilder.bind(teamInviteSentQueue()).to(founderLinkExchange()).with("team.invite.sent");
    }

    @Bean
    public Binding startupCreatedBinding() {
        return BindingBuilder.bind(startupCreatedQueue()).to(founderLinkExchange()).with("startup.created");
    }

    @Bean
    public Binding startupRejectedBinding() {
        return BindingBuilder.bind(startupRejectedQueue()).to(founderLinkExchange()).with("startup.rejected");
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(founderLinkExchange()).with("payment.success");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(founderLinkExchange()).with("payment.failed");
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder.bind(userRegisteredQueue()).to(founderLinkExchange()).with("user.registered");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
