package com.capgemini.startup.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "founderlink.exchange";
    public static final String STARTUP_CREATED_QUEUE = "startup.created.queue";
    public static final String STARTUP_CREATED_ROUTING_KEY = "startup.created";
    public static final String STARTUP_REJECTED_QUEUE = "startup.rejected.queue";
    public static final String STARTUP_REJECTED_ROUTING_KEY = "startup.rejected";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue startupCreatedQueue() {
        return QueueBuilder.durable(STARTUP_CREATED_QUEUE).build();
    }

    @Bean
    public Queue startupRejectedQueue() {
        return QueueBuilder.durable(STARTUP_REJECTED_QUEUE).build();
    }

    @Bean
    public Binding startupCreatedBinding(Queue startupCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(startupCreatedQueue).to(exchange).with(STARTUP_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding startupRejectedBinding(Queue startupRejectedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(startupRejectedQueue).to(exchange).with(STARTUP_REJECTED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
