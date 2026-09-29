package com.capgemini.investment.config;

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

    @Value("${rabbitmq.routing-key.investment-created}")
    private String investmentCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.investment-approved}")
    private String investmentApprovedRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue investmentCreatedQueue() {
        return new Queue(investmentCreatedQueue, true);
    }

    @Bean
    public Queue investmentApprovedQueue() {
        return new Queue(investmentApprovedQueue, true);
    }

    @Bean
    public Binding investmentCreatedBinding(Queue investmentCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(investmentCreatedQueue).to(exchange).with(investmentCreatedRoutingKey);
    }

    @Bean
    public Binding investmentApprovedBinding(Queue investmentApprovedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(investmentApprovedQueue).to(exchange).with(investmentApprovedRoutingKey);
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
