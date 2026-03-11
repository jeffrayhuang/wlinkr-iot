package com.wlinkr.iot.controller;

import com.wlinkr.iot.model.dto.CreateCommandRequest;
import com.wlinkr.iot.model.dto.DeviceCommandDto;
import com.wlinkr.iot.security.CurrentUser;
import com.wlinkr.iot.security.UserPrincipal;
import com.wlinkr.iot.service.DeviceCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/commands")
@Tag(name = "Device Commands", description = "Send commands to devices and view history")
public class DeviceCommandController {

    private final DeviceCommandService commandService;

    public DeviceCommandController(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping
    @Operation(summary = "List commands sent to a device")
    public Page<DeviceCommandDto> listCommands(
            @PathVariable Long deviceId,
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return commandService.getCommands(deviceId, principal.getId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a new command to a device")
    public DeviceCommandDto sendCommand(
            @PathVariable Long deviceId,
            @Valid @RequestBody CreateCommandRequest request,
            @CurrentUser UserPrincipal principal) {
        return commandService.sendCommand(deviceId, principal.getId(), request);
    }
}
