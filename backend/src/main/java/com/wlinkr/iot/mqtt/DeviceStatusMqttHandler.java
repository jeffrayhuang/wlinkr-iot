package com.wlinkr.iot.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Handles device status updates from MQTT topic: wlinkr/devices/{serialNumber}/status
 *
 * Expected payload:
 * { "status": "ONLINE" }
 * or
 * { "status": "OFFLINE", "reason": "power_loss" }
 *
 * Unknown serial numbers are auto-registered as new devices.
 */
@Component
public class DeviceStatusMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusMqttHandler.class);

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceAutoRegistrar deviceAutoRegistrar;

    public DeviceStatusMqttHandler(ObjectMapper objectMapper,
                                    DeviceRepository deviceRepository,
                                    SimpMessagingTemplate messagingTemplate,
                                    DeviceAutoRegistrar deviceAutoRegistrar) {
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.messagingTemplate = messagingTemplate;
        this.deviceAutoRegistrar = deviceAutoRegistrar;
    }

    @Transactional
    @CacheEvict(value = {"devices", "device-detail", "dashboard"}, allEntries = true)
    public void handle(String serialNumber, String payload) {
        Device device = deviceAutoRegistrar.findOrRegister(serialNumber);
        if (device == null) {
            log.warn("Received status for unknown device (auto-register disabled): {}", serialNumber);
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});
            String statusStr = (String) data.get("status");

            if (statusStr == null) {
                log.warn("Status field missing in payload from device {}", serialNumber);
                return;
            }

            DeviceStatus newStatus;
            try {
                newStatus = DeviceStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown device status '{}' from device {}", statusStr, serialNumber);
                return;
            }

            DeviceStatus oldStatus = device.getStatus();
            device.setStatus(newStatus);
            deviceRepository.save(device);

            log.info("Device {} status changed: {} -> {}", serialNumber, oldStatus, newStatus);

            // Forward status change to WebSocket subscribers
            Map<String, Object> wsPayload = Map.of(
                    "deviceId", device.getId(),
                    "serialNumber", serialNumber,
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name(),
                    "reason", data.getOrDefault("reason", ""),
                    "timestamp", Instant.now().toString()
            );
            messagingTemplate.convertAndSend("/topic/devices/" + device.getId() + "/status", wsPayload);
            messagingTemplate.convertAndSend("/topic/devices/status", wsPayload);

        } catch (Exception e) {
            log.error("Failed to process status update from device {}: {}", serialNumber, e.getMessage(), e);
        }
    }
}
