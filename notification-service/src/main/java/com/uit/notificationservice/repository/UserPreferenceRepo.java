package com.uit.notificationservice.repository;

import com.uit.notificationservice.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepo extends JpaRepository<UserPreference, String> {
}
