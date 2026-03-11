package com.wlinkr.iot.mqtt;

import com.wlinkr.iot.model.entity.DeviceCommand;
import com.wlinkr.iot.model.enums.CommandStatus;
import com.wlinkr.iot.repository.DeviceCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes device commands over MQTT to topic:
 * wlinkr/devices/{serialNumber}/commands
 *
 * Sent payload format:
 * {
 *   "commandId": 123,
 *   "commandName": "setLed",
 *   "payload": { "color": "red", "brightness": 100 }
 * }
 */
@Component
public class MqttCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttCommandPublisher.class);

    private final MqttService mqttService;
    private final DeviceCommandRepository commandRepository;

    public MqttCommandPublisher(MqttService mqttService,
                                 DeviceCommandRepository commandRepository) {
        this.mqttService = mqttService;
        this.commandRepository = commandRepository;
    }

    /**
     * Publishes a command to the device's MQTT command topic and updates status to SENT.
     */
    public void publishCommand(DeviceCommand command) {
        String serialNumber = command.getDevice().getSerialNumber();
        String topic = "wlinkr/devices/" + serialNumber + "/commands";

        Map<String, Object> mqttPayload = Map.of(
                "commandId", command.getId(),
                "commandName", command.getCommandName(),
                "payload", command.getPayload() != null ? command.getPayload() : Map.of()
        );

        if (!mqttService.isConnected()) {
            log.warn("MQTT not connected, cannot publish command {} to device {}",
                    command.getId(), serialNumber);
            return;
        }

        mqttService.publish(topic, mqttPayload);

        // Update command status to SENT
        command.setStatus(CommandStatus.SENT);
        commandRepository.save(command);

        log.info("Published command {} ({}) to device {}",
                command.getId(), command.getCommandName(), serialNumber);
    }
}
