package com.ciro.notificationservice.repository;

import com.ciro.notificationservice.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String>
{
    List<Notification> findByOrderId(Long orderId);
}
