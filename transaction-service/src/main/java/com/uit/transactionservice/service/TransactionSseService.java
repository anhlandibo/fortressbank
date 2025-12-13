package com.uit.transactionservice.service;

import com.uit.transactionservice.dto.sse.TransactionStatusUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing SSE connections and pushing transaction updates.
 * Uses in-memory Map - suitable for single instance deployment.
 * 
 * Flow:
 * 1. Client verifies OTP successfully
 * 2. Client opens SSE connection: GET /api/transactions/sse/{transactionId}
 * 3. Server stores SseEmitter in Map
 * 4. Stripe webhook arrives → Server pushes update via SseEmitter
 * 5. Client receives update and closes connection
 */
@Service
@Slf4j
public class TransactionSseService {

    // In-memory map: transactionId -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE timeout: 5 minutes (enough for Stripe to process)
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * Create and register SSE emitter for a transaction.
     * Called when client opens SSE connection.
     * 
     * @param transactionId Transaction ID to subscribe to
     * @return SseEmitter for the connection
     */
    public SseEmitter subscribe(String transactionId) {
        // Remove existing emitter if any
        SseEmitter existingEmitter = emitters.remove(transactionId);
        if (existingEmitter != null) {
            existingEmitter.complete();
        }

        // Create new emitter with timeout
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Setup cleanup callbacks
        emitter.onCompletion(() -> {
            emitters.remove(transactionId);
            log.info("SSE completed for transaction: {}", transactionId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(transactionId);
            log.warn("SSE timeout for transaction: {}", transactionId);
        });
        
        emitter.onError(e -> {
            emitters.remove(transactionId);
            log.error("SSE error for transaction: {} - {}", transactionId, e.getMessage());
        });

        // Store emitter
        emitters.put(transactionId, emitter);
        log.info("SSE connection opened for transaction: {}", transactionId);

        // Send initial "connected" event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established for transaction: " + transactionId));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Push transaction status update to client.
     * Called when webhook is received from Stripe.
     * 
     * @param transactionId Transaction ID
     * @param update Status update to send
     */
    public void pushUpdate(String transactionId, TransactionStatusUpdate update) {
        SseEmitter emitter = emitters.get(transactionId);
        
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("transaction-update")
                        .data(update));
                
                log.info("SSE pushed update for transaction: {} - Status: {}", 
                        transactionId, update.getStatus());
                
                // If terminal state, complete the emitter
                if (update.getStatus() == com.uit.transactionservice.entity.TransactionStatus.COMPLETED ||
                    update.getStatus() == com.uit.transactionservice.entity.TransactionStatus.FAILED) {
                    
                    emitter.complete();
                    emitters.remove(transactionId);
                    log.info("📡 SSE connection closed after terminal state for: {}", transactionId);
                }
                
            } catch (IOException e) {
                log.error("SSE failed to push update for transaction: {} - {}", 
                        transactionId, e.getMessage());
                emitters.remove(transactionId);
            }
        } else {
            log.warn("No SSE subscriber for transaction: {} - Client may not be listening", 
                    transactionId);
        }
    }

    /**
     * Check if there's an active SSE connection for a transaction
     */
    public boolean hasSubscriber(String transactionId) {
        return emitters.containsKey(transactionId);
    }

    /**
     * Get count of active SSE connections (for monitoring)
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
