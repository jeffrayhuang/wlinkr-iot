package com.wlinkr.iot.model.dto;

import java.time.Instant;

public record SensorDataDto(
        Long id,
        Long deviceId,
        String metricName,
        Double metricValue,
        String unit,
        Instant recordedAt
) {}
