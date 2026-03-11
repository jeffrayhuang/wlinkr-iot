package com.wlinkr.iot.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.entity.SensorData;
import com.wlinkr.iot.repository.SensorDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Handles incoming telemetry data from MQTT topic: wlinkr/devices/{serialNumber}/telemetry
 *
 * Expected payload format:
 * {
 *   "metrics": [
 *     { "name": "temperature", "value": 23.5, "unit": "°C" },
 *     { "name": "humidity", "value": 60.2, "unit": "%" }
 *   ],
 *   "timestamp": "2024-01-15T10:30:00Z"  (optional)
 * }
 *
 * Or single metric:
 * { "name": "temperature", "value": 23.5, "unit": "°C" }
 *
 * Unknown serial numbers are auto-registered as new devices.
 */
@Component
public class SensorDataMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(SensorDataMqttHandler.class);

    private final ObjectMapper objectMapper;
    private final SensorDataRepository sensorDataRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceAutoRegistrar deviceAutoRegistrar;

    public SensorDataMqttHandler(ObjectMapper objectMapper,
                                  SensorDataRepository sensorDataRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  DeviceAutoRegistrar deviceAutoRegistrar) {
        this.objectMapper = objectMapper;
        this.sensorDataRepository = sensorDataRepository;
        this.messagingTemplate = messagingTemplate;
        this.deviceAutoRegistrar = deviceAutoRegistrar;
    }

    @Transactional
    @CacheEvict(value = {"sensor-data", "dashboard"}, allEntries = true)
    public void handle(String serialNumber, String payload) {
        Device device = deviceAutoRegistrar.findOrRegister(serialNumber);
        if (device == null) {
            log.warn("Received telemetry for unknown device (auto-register disabled): {}", serialNumber);
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});

            Instant timestamp = data.containsKey("timestamp")
                    ? Instant.parse((String) data.get("timestamp"))
                    : Instant.now();

            // Support array of metrics or single metric
            if (data.containsKey("metrics")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> metrics = (List<Map<String, Object>>) data.get("metrics");
                for (Map<String, Object> metric : metrics) {
                    saveSensorData(device, metric, timestamp);
                }
            } else if (data.containsKey("name") && data.containsKey("value")) {
                saveSensorData(device, data, timestamp);
            } else {
                log.warn("Invalid telemetry payload format from device {}: {}", serialNumber, payload);
                return;
            }

            // Forward to WebSocket subscribers
            Map<String, Object> wsPayload = Map.of(
                    "deviceId", device.getId(),
                    "serialNumber", serialNumber,
                    "data", data,
                    "receivedAt", Instant.now().toString()
            );
            messagingTemplate.convertAndSend("/topic/devices/" + device.getId() + "/telemetry", wsPayload);

            log.debug("Processed telemetry from device {}", serialNumber);

        } catch (Exception e) {
            log.error("Failed to process telemetry from device {}: {}", serialNumber, e.getMessage(), e);
        }
    }

    private void saveSensorData(Device device, Map<String, Object> metric, Instant timestamp) {
        String name = (String) metric.get("name");
        Double value = ((Number) metric.get("value")).doubleValue();
        String unit = (String) metric.getOrDefault("unit", null);

        SensorData sensorData = SensorData.builder()
                .device(device)
                .metricName(name)
                .metricValue(value)
                .unit(unit)
                .recordedAt(timestamp)
                .build();

        sensorDataRepository.save(sensorData);
    }
}
