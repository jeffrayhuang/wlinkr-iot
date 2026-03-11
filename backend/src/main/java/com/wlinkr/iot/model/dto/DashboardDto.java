package com.wlinkr.iot.model.dto;

import java.util.Map;

/**
 * Aggregated dashboard data returned to the frontend.
 */
public record DashboardDto(
        long totalDevices,
        long onlineDevices,
        long offlineDevices,
        long errorDevices,
        long totalSensorReadings,
        long pendingCommands,
        Map<String, Long> devicesByType
) {}
