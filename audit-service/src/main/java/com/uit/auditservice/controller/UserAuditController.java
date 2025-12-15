package com.uit.auditservice.controller;

import com.uit.auditservice.entity.UserAuditLog;
import com.uit.auditservice.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit/user")
@RequiredArgsConstructor
public class UserAuditController {

    private final UserAuditLogRepository repository;

    @GetMapping
    public ResponseEntity<Page<UserAuditLog>> getAllAudits(Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<UserAuditLog>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(repository.findHistoryByEntity(entityType, entityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UserAuditLog>> getUserAudits(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByUserId(userId, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<UserAuditLog>> getByAction(
            @PathVariable String action,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByAction(action, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<UserAuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByTimestampBetween(start, end, pageable));
    }
}
