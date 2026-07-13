package com.sjbit.library.service;

import com.sjbit.library.entity.Notification;
import com.sjbit.library.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification notify(String userId, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        return notificationRepository.save(n);
    }

    public List<Notification> forUser(String userId) {
        return notificationRepository.findByUserIdOrderByWhenDesc(userId);
    }
}
