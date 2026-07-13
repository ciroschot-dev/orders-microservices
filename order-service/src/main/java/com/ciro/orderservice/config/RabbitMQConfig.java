package com.ciro.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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

/**
 * RabbitMQ setup for order-service. Publica order.created (producer) y, desde Phase 7.5,
 * también consume los eventos de vuelta order.confirmed / order.cancelled (consumer).
 */
@Configuration
public class RabbitMQConfig
{
    // Shared contract: inventory-service binds its queue to this exchange using this routing key.
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    // Reverse events (inventory → order): resultado de reservar stock.
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_CONFIRMED_QUEUE = "order.confirmed.order.queue";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.order.queue";

    @Bean
    public TopicExchange orderExchange()
    {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // Colas propias del consumer, bindeadas al mismo exchange por routing key.
    @Bean
    public Queue orderConfirmedQueue()
    {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Queue orderCancelledQueue()
    {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange orderExchange)
    {
        return BindingBuilder.bind(orderConfirmedQueue).to(orderExchange).with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange orderExchange)
    {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

    // INFERRED: al consumir, ignora el header __TypeId__ del productor y deserializa según el tipo
    // del parámetro del @RabbitListener, así mapea a NUESTROS records aunque el paquete difiera.
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

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv)
    {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(conv);
        return template;
    }
}
