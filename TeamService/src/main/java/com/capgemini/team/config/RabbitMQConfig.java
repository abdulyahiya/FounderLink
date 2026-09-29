package com.capgemini.team.config;

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

    @Value("${rabbitmq.queue.team-invite-sent}")
    private String teamInviteSentQueue;

    @Value("${rabbitmq.routing-key.team-invite-sent}")
    private String teamInviteSentRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue teamInviteSentQueue() {
        return new Queue(teamInviteSentQueue, true);
    }

    @Bean
    public Binding teamInviteSentBinding(Queue teamInviteSentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(teamInviteSentQueue).to(exchange).with(teamInviteSentRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
