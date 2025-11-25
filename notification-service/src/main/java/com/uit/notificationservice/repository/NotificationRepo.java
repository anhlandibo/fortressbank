package com.uit.notificationservice.repository;

import com.uit.notificationservice.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepo extends JpaRepository<NotificationMessage, String> {

}
