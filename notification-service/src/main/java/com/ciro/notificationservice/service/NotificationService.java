package com.ciro.notificationservice.service;

import com.ciro.notificationservice.event.OrderCreatedEvent;
import com.ciro.notificationservice.model.Notification;
import com.ciro.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService
{
    private final NotificationRepository notificationRepository;

    // Reacts to OrderCreated: builds and stores a notification (a real system would also send an email/SMS).
    public Notification notifyOrderCreated(OrderCreatedEvent event)
    {
        String message = "Order %d received with %d item(s). We'll let you know when it's confirmed."
                .formatted(event.orderId(), event.items().size());

        Notification notification = new Notification(null, event.orderId(), message, Instant.now());
        Notification saved = notificationRepository.save(notification);

        log.info("Notification stored for order {}", event.orderId());
        return saved;
    }

    // Reacts to OrderConfirmed: the "we'll let you know" promise from the created-notification, fulfilled.
    public Notification notifyOrderConfirmed(Long orderId)
    {
        return store(orderId, "Order %d confirmed — stock reserved. It's being prepared.".formatted(orderId));
    }

    // Reacts to OrderCancelled: stock couldn't be reserved (e.g. a race lost the last units).
    public Notification notifyOrderCancelled(Long orderId)
    {
        return store(orderId, "Order %d cancelled — out of stock. Nothing was charged.".formatted(orderId));
    }

    private Notification store(Long orderId, String message)
    {
        Notification saved = notificationRepository.save(new Notification(null, orderId, message, Instant.now()));
        log.info("Notification stored for order {}", orderId);
        return saved;
    }

    public List<Notification> getAll()
    {
        return notificationRepository.findAll();
    }
}
