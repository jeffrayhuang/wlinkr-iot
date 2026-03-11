package com.wlinkr.iot.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wlinkr.iot.model.entity.DeviceCommand;
import com.wlinkr.iot.model.enums.CommandStatus;
import com.wlinkr.iot.repository.DeviceCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Handles command responses from devices via MQTT topic:
 * wlinkr/devices/{serialNumber}/commands/response
 *
 * Expected payload:
 * {
 *   "commandId": 123,
 *   "status": "ACKNOWLEDGED",
 *   "response": { "result": "ok", "details": "LED turned on" }
 * }
 */
@Component
public class CommandResponseMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandResponseMqttHandler.class);

    private final ObjectMapper objectMapper;
    private final DeviceCommandRepository commandRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CommandResponseMqttHandler(ObjectMapper objectMapper,
                                       DeviceCommandRepository commandRepository,
                                       SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.commandRepository = commandRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void handle(String serialNumber, String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});
            Number commandIdNum = (Number) data.get("commandId");
            String statusStr = (String) data.get("status");

            if (commandIdNum == null || statusStr == null) {
                log.warn("Invalid command response from device {}: missing commandId or status", serialNumber);
                return;
            }

            Long commandId = commandIdNum.longValue();
            Optional<DeviceCommand> commandOpt = commandRepository.findById(commandId);
            if (commandOpt.isEmpty()) {
                log.warn("Command response for unknown command ID {} from device {}", commandId, serialNumber);
                return;
            }

            DeviceCommand command = commandOpt.get();
            CommandStatus newStatus;
            try {
                newStatus = CommandStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown command status '{}' from device {}", statusStr, serialNumber);
                return;
            }

            command.setStatus(newStatus);
            command.setExecutedAt(Instant.now());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) data.get("response");
            if (response != null) {
                command.setResponse(response);
            }

            commandRepository.save(command);

            log.info("Command {} ({}) on device {} status: {}",
                    commandId, command.getCommandName(), serialNumber, newStatus);

            // Forward to WebSocket subscribers
            Map<String, Object> wsPayload = Map.of(
                    "commandId", commandId,
                    "deviceId", command.getDevice().getId(),
                    "serialNumber", serialNumber,
                    "commandName", command.getCommandName(),
                    "status", newStatus.name(),
                    "response", response != null ? response : Map.of(),
                    "executedAt", Instant.now().toString()
            );
            messagingTemplate.convertAndSend(
                    "/topic/devices/" + command.getDevice().getId() + "/commands", wsPayload);

        } catch (Exception e) {
            log.error("Failed to process command response from device {}: {}", serialNumber, e.getMessage(), e);
        }
    }
}
