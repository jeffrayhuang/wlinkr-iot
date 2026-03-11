package com.wlinkr.iot.service;

import com.wlinkr.iot.exception.ResourceNotFoundException;
import com.wlinkr.iot.model.dto.CreateCommandRequest;
import com.wlinkr.iot.model.dto.DeviceCommandDto;
import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.entity.DeviceCommand;
import com.wlinkr.iot.model.entity.User;
import com.wlinkr.iot.model.enums.CommandStatus;
import com.wlinkr.iot.mqtt.MqttCommandPublisher;
import com.wlinkr.iot.repository.DeviceCommandRepository;
import com.wlinkr.iot.repository.DeviceRepository;
import com.wlinkr.iot.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class DeviceCommandService {

    private final DeviceCommandRepository commandRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final MqttCommandPublisher mqttCommandPublisher;

    public DeviceCommandService(DeviceCommandRepository commandRepository,
                                DeviceRepository deviceRepository,
                                UserRepository userRepository,
                                MqttCommandPublisher mqttCommandPublisher) {
        this.commandRepository = commandRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.mqttCommandPublisher = mqttCommandPublisher;
    }

    @Transactional(readOnly = true)
    public Page<DeviceCommandDto> getCommands(Long deviceId, Long ownerId, Pageable pageable) {
        findDeviceChecked(deviceId, ownerId);
        return commandRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId, pageable).map(this::toDto);
    }

    @Transactional
    public DeviceCommandDto sendCommand(Long deviceId, Long userId, CreateCommandRequest request) {
        Device device = findDeviceChecked(deviceId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DeviceCommand command = DeviceCommand.builder()
                .device(device)
                .issuedBy(user)
                .commandName(request.commandName())
                .payload(request.payload())
                .status(CommandStatus.PENDING)
                .build();

        DeviceCommand saved = commandRepository.save(command);

        // Publish the command to the device via MQTT
        mqttCommandPublisher.publishCommand(saved);

        return toDto(saved);
    }

    @Transactional
    public DeviceCommandDto acknowledgeCommand(Long commandId, Map<String, Object> response) {
        DeviceCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Command", "id", commandId));
        command.setStatus(CommandStatus.ACKNOWLEDGED);
        command.setResponse(response);
        command.setExecutedAt(Instant.now());
        return toDto(commandRepository.save(command));
    }

    private Device findDeviceChecked(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        if (!device.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Device", "id", deviceId);
        }
        return device;
    }

    private DeviceCommandDto toDto(DeviceCommand c) {
        return new DeviceCommandDto(
                c.getId(), c.getDevice().getId(), c.getIssuedBy().getId(),
                c.getCommandName(), c.getPayload(), c.getStatus(),
                c.getResponse(), c.getCreatedAt(), c.getExecutedAt()
        );
    }
}
