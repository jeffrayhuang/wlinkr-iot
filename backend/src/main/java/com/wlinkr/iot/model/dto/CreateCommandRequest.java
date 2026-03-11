package com.wlinkr.iot.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateCommandRequest(
        @NotBlank String commandName,
        Map<String, Object> payload
) {}
