package com.wlinkr.iot.model.dto;

import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;

import java.time.Instant;

public record DeviceDto(
        Long id,
        String name,
        DeviceType deviceType,
        DeviceStatus status,
        String serialNumber,
        String firmwareVersion,
        String location,
        String description,
        Long ownerId,
        Instant createdAt,
        Instant updatedAt
) {}
