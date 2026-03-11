package com.wlinkr.iot.controller;

import com.wlinkr.iot.mqtt.MqttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mqtt")
@Tag(name = "MQTT", description = "MQTT broker status and operations")
public class MqttController {

    private final MqttService mqttService;

    public MqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get MQTT connection status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "connected", mqttService.isConnected(),
                "status", mqttService.isConnected() ? "CONNECTED" : "DISCONNECTED"
        ));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish a message to an MQTT topic (admin/debug)")
    public ResponseEntity<Map<String, Object>> publish(
            @RequestParam String topic,
            @RequestBody Map<String, Object> payload) {

        if (!mqttService.isConnected()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "MQTT broker not connected"
            ));
        }

        mqttService.publish(topic, payload);
        return ResponseEntity.ok(Map.of(
                "published", true,
                "topic", topic
        ));
    }
}
