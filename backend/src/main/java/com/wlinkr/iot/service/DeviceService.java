package com.wlinkr.iot.service;

import com.wlinkr.iot.exception.ResourceNotFoundException;
import com.wlinkr.iot.model.dto.*;
import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.entity.User;
import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;
import com.wlinkr.iot.repository.DeviceRepository;
import com.wlinkr.iot.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "devices", key = "#ownerId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<DeviceDto> getDevices(Long ownerId, Pageable pageable) {
        return deviceRepository.findByOwnerId(ownerId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "devices", key = "#ownerId + '-' + #status + '-' + #pageable.pageNumber")
    public Page<DeviceDto> getDevicesByStatus(Long ownerId, DeviceStatus status, Pageable pageable) {
        return deviceRepository.findByOwnerIdAndStatus(ownerId, status, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "devices", key = "#ownerId + '-type-' + #type + '-' + #pageable.pageNumber")
    public Page<DeviceDto> getDevicesByType(Long ownerId, DeviceType type, Pageable pageable) {
        return deviceRepository.findByOwnerIdAndDeviceType(ownerId, type, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "device-detail", key = "#deviceId")
    public DeviceDto getDevice(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        validateOwnership(device, ownerId);
        return toDto(device);
    }

    @Transactional
    @CacheEvict(value = {"devices", "device-detail", "dashboard"}, allEntries = true)
    public DeviceDto createDevice(CreateDeviceRequest request, Long ownerId) {
        if (deviceRepository.existsBySerialNumber(request.serialNumber())) {
            throw new IllegalArgumentException("Serial number already exists: " + request.serialNumber());
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Device device = Device.builder()
                .name(request.name())
                .deviceType(request.deviceType())
                .status(DeviceStatus.OFFLINE)
                .serialNumber(request.serialNumber())
                .firmwareVersion(request.firmwareVersion())
                .location(request.location())
                .description(request.description())
                .owner(owner)
                .build();

        return toDto(deviceRepository.save(device));
    }

    @Transactional
    @CacheEvict(value = {"devices", "device-detail", "dashboard"}, allEntries = true)
    public DeviceDto updateDevice(Long deviceId, UpdateDeviceRequest request, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        validateOwnership(device, ownerId);

        if (request.name() != null) device.setName(request.name());
        if (request.status() != null) device.setStatus(request.status());
        if (request.firmwareVersion() != null) device.setFirmwareVersion(request.firmwareVersion());
        if (request.location() != null) device.setLocation(request.location());
        if (request.description() != null) device.setDescription(request.description());

        return toDto(deviceRepository.save(device));
    }

    @Transactional
    @CacheEvict(value = {"devices", "device-detail", "dashboard"}, allEntries = true)
    public void deleteDevice(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        validateOwnership(device, ownerId);
        deviceRepository.delete(device);
    }

    private void validateOwnership(Device device, Long ownerId) {
        if (!device.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Device", "id", device.getId());
        }
    }

    private DeviceDto toDto(Device d) {
        return new DeviceDto(
                d.getId(), d.getName(), d.getDeviceType(), d.getStatus(),
                d.getSerialNumber(), d.getFirmwareVersion(), d.getLocation(),
                d.getDescription(), d.getOwner().getId(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
