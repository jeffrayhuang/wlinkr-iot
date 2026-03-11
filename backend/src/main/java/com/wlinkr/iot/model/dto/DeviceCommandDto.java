package com.wlinkr.iot.model.dto;

import com.wlinkr.iot.model.enums.CommandStatus;

import java.time.Instant;
import java.util.Map;

public record DeviceCommandDto(
        Long id,
        Long deviceId,
        Long issuedById,
        String commandName,
        Map<String, Object> payload,
        CommandStatus status,
        Map<String, Object> response,
        Instant createdAt,
        Instant executedAt
) {}
