package com.wlinkr.iot.controller;

import com.wlinkr.iot.model.dto.*;
import com.wlinkr.iot.security.CurrentUser;
import com.wlinkr.iot.security.UserPrincipal;
import com.wlinkr.iot.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device CRUD and management")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @Operation(summary = "List all devices for the current user")
    public Page<DeviceDto> listDevices(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) DeviceType type,
            @PageableDefault(size = 20) Pageable pageable) {

        if (status != null) {
            return deviceService.getDevicesByStatus(principal.getId(), status, pageable);
        }
        if (type != null) {
            return deviceService.getDevicesByType(principal.getId(), type, pageable);
        }
        return deviceService.getDevices(principal.getId(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single device by ID")
    public DeviceDto getDevice(@PathVariable Long id,
                               @CurrentUser UserPrincipal principal) {
        return deviceService.getDevice(id, principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new device")
    public DeviceDto createDevice(@Valid @RequestBody CreateDeviceRequest request,
                                  @CurrentUser UserPrincipal principal) {
        return deviceService.createDevice(request, principal.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a device")
    public DeviceDto updateDevice(@PathVariable Long id,
                                  @Valid @RequestBody UpdateDeviceRequest request,
                                  @CurrentUser UserPrincipal principal) {
        return deviceService.updateDevice(id, request, principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a device")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id,
                                             @CurrentUser UserPrincipal principal) {
        deviceService.deleteDevice(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
