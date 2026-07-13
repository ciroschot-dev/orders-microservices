package com.ciro.inventoryservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig
{
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "order.created.inventory.queue";

    // Reverse events we publish back to order-service once stock is (or isn't) reserved.
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    // Dead-letter target: after the listener's retries are exhausted (technical failure), the message is
    // routed here instead of being retried forever. Business failures (no stock) are logged, never thrown,
    // so they don't reach the DLQ.
    public static final String DLX = "order.dlx";
    public static final String ORDER_CREATED_DLQ = "order.created.inventory.queue.dlq";

    @Bean
    public Queue orderCreatedQueue()
    {
        // Durable, and its dead-letter target is the DLX + the DLQ routing key.
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
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange)
    {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange()
    {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue orderCreatedDeadLetterQueue()
    {
        return QueueBuilder.durable(ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue orderCreatedDeadLetterQueue, DirectExchange deadLetterExchange)
    {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue).to(deadLetterExchange).with(ORDER_CREATED_DLQ);
    }

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

    // Hasta ahora inventory solo consumía; para publicar los eventos de vuelta
    // (confirmed/cancelled) necesita un RabbitTemplate con el mismo converter JSON.
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv)
    {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(conv);
        return template;
    }
}
