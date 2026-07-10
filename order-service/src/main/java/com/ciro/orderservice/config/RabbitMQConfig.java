package com.ciro.orderservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ setup for the producer side: the exchange we publish to and how messages are serialized.
 */
@Configuration
public class RabbitMQConfig
{
    // Shared contract: inventory-service binds its queue to this exchange using this routing key.
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    // We declare only the exchange — queues belong to whoever consumes, not to the publisher.
    @Bean
    public TopicExchange orderExchange()
    {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // Without this, Spring falls back to Java serialization (opaque bytes, Java-only consumers).
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter()
    {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv)
    {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(conv);
        return template;
    }
}
