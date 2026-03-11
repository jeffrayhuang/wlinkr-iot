package com.wlinkr.iot.controller;

import com.wlinkr.iot.model.dto.CreateSensorDataRequest;
import com.wlinkr.iot.model.dto.SensorDataDto;
import com.wlinkr.iot.security.CurrentUser;
import com.wlinkr.iot.security.UserPrincipal;
import com.wlinkr.iot.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/data")
@Tag(name = "Sensor Data", description = "Ingest and query sensor telemetry")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    public SensorDataController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping
    @Operation(summary = "Get paginated sensor data for a device")
    public Page<SensorDataDto> getData(
            @PathVariable Long deviceId,
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 50) Pageable pageable) {
        return sensorDataService.getSensorData(deviceId, principal.getId(), pageable);
    }

    @GetMapping("/range")
    @Operation(summary = "Get sensor data within a time range")
    public List<SensorDataDto> getDataRange(
            @PathVariable Long deviceId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @CurrentUser UserPrincipal principal) {
        return sensorDataService.getSensorDataRange(deviceId, principal.getId(), from, to);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest N readings for a given metric")
    public List<SensorDataDto> getLatestMetric(
            @PathVariable Long deviceId,
            @RequestParam String metric,
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser UserPrincipal principal) {
        return sensorDataService.getLatestMetric(deviceId, principal.getId(), metric, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a new sensor reading")
    public SensorDataDto ingestData(
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateSensorDataRequest request,
            @CurrentUser UserPrincipal principal) {
        return sensorDataService.ingestData(deviceId, principal.getId(), request);
    }
}
