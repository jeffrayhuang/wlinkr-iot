package com.wlinkr.iot.model.dto;

import com.wlinkr.iot.model.enums.DeviceStatus;

public record UpdateDeviceRequest(
        String name,
        DeviceStatus status,
        String firmwareVersion,
        String location,
        String description
) {}
