package com.wlinkr.iot.model.dto;

import com.wlinkr.iot.model.enums.AuthProvider;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String name,
        String avatarUrl,
        AuthProvider provider,
        Instant createdAt
) {}
