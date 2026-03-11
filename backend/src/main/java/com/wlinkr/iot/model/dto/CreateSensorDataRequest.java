package com.wlinkr.iot.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSensorDataRequest(
        @NotBlank String metricName,
        @NotNull Double metricValue,
        String unit
) {}
