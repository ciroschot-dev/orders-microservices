package com.ciro.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Consumer-side RabbitMQ setup. Declares its OWN queue bound to the SHARED order.exchange:
 * that's the fan-out — inventory and notification each have a queue on the same exchange, so one
 * OrderCreated event reaches both. Adding this consumer required zero changes in order-service.
 */
@Configuration
public class RabbitMQConfig
{
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "order.created.notification.queue";

    // Dead-letter target: after the listener's retries are exhausted (technical failure), the message
    // lands here instead of looping forever.
    public static final String DLX = "order.dlx";
    public static final String ORDER_CREATED_DLQ = "order.created.notification.queue.dlq";

    @Bean
    public Queue orderCreatedNotificationQueue()
    {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public TopicExchange orderExchange()
    {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderCreatedNotificationBinding(Queue orderCreatedNotificationQueue, TopicExchange orderExchange)
    {
        return BindingBuilder.bind(orderCreatedNotificationQueue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange()
    {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue orderCreatedNotificationDeadLetterQueue()
    {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Binding notificationDeadLetterBinding(Queue orderCreatedNotificationDeadLetterQueue, DirectExchange deadLetterExchange)
    {
        return BindingBuilder.bind(orderCreatedNotificationDeadLetterQueue).to(deadLetterExchange).with(ORDER_CREATED_DLQ);
    }

    // INFERRED: convert to this service's own event type, ignoring the producer's __TypeId__ header
    // (which names an order-service class that doesn't exist here).
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter()
    {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
