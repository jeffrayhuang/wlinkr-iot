package com.wlinkr.iot.model.dto;

import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeviceRequest(
        @NotBlank String name,
        @NotNull DeviceType deviceType,
        @NotBlank String serialNumber,
        String firmwareVersion,
        String location,
        String description
) {}
