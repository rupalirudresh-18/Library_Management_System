package com.sjbit.library.controller;

import com.sjbit.library.entity.Notification;
import com.sjbit.library.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public List<Notification> forUser(@PathVariable String userId) {
        return notificationService.forUser(userId);
    }
}
