package com.ciro.orderservice.event;

import com.ciro.orderservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges Spring's in-memory application events to RabbitMQ, so OrderService never touches messaging.
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisher
{
    private final RabbitTemplate rabbitTemplate;

    // AFTER_COMMIT: publish only once the order really hit the DB. Publishing inside the transaction
    // would leak events for orders that later roll back — the message can't be un-sent.
    // Remaining gap (accepted): if the process dies between commit and send, the event is lost.
    // The airtight fix is a transactional outbox; overkill here.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event)
    {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
