package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.RegisterDeviceRequest;
import com.uit.accountservice.entity.UserDevice;
import com.uit.accountservice.service.SmartOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final SmartOtpService smartOtpService;

    /**
     * Register a new device for Smart OTP
     * POST /api/devices/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody RegisterDeviceRequest request
    ) {
        try {
            UserDevice device = smartOtpService.registerDevice(userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deviceId", device.getId(),
                    "deviceName", device.getDeviceName(),
                    "trusted", device.getTrusted(),
                    "message", "Device registered successfully. Awaiting trust approval."
            ));
        } catch (IllegalArgumentException e) {
            log.error("Device registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * List all active devices for current user
     * GET /api/devices
     */
    @GetMapping
    public ResponseEntity<?> listDevices(@RequestHeader("X-User-Id") String userId) {
        List<UserDevice> devices = smartOtpService.listUserDevices(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "devices", devices,
                "count", devices.size()
        ));
    }

    /**
     * Revoke a device (user lost phone, security breach)
     * DELETE /api/devices/{deviceId}
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<?> revokeDevice(
            @PathVariable String deviceId,
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            smartOtpService.revokeDevice(deviceId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device revoked successfully"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Device revocation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update device trust status (admin/security endpoint)
     * PUT /api/devices/{deviceId}/trust
     */
    @PutMapping("/{deviceId}/trust")
    public ResponseEntity<?> updateTrustStatus(
            @PathVariable String deviceId,
            @RequestParam boolean trusted,
            @RequestHeader("X-User-Id") String userId
    ) {
        // TODO: Add admin authorization check
        log.info("Updating trust status for deviceId={}: trusted={}", deviceId, trusted);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Trust status updated (not yet implemented)"
        ));
    }
}
