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

    public List<Notification> getAll()
    {
        return notificationRepository.findAll();
    }
}
