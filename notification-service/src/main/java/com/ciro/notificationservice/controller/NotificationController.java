package com.ciro.notificationservice.controller;

import com.ciro.notificationservice.model.Notification;
import com.ciro.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController
{
    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAll()
    {
        return notificationService.getAll();
    }
}
