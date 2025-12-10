package com.uit.auditservice.controller;

import com.uit.auditservice.entity.TransactionAuditLog;
import com.uit.auditservice.repository.TransactionAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit/transaction")
@RequiredArgsConstructor
public class TransactionAuditController {

    private final TransactionAuditLogRepository repository;

    @GetMapping
    public ResponseEntity<Page<TransactionAuditLog>> getAllAudits(Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<TransactionAuditLog>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(repository.findHistoryByEntity(entityType, entityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TransactionAuditLog>> getUserAudits(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByUserId(userId, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<TransactionAuditLog>> getByAction(
            @PathVariable String action,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByAction(action, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<TransactionAuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        return ResponseEntity.ok(repository.findByTimestampBetween(start, end, pageable));
    }
}
