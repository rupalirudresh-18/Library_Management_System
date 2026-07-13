package com.sjbit.library.repository;

import com.sjbit.library.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByWhenDesc(String userId);
}
