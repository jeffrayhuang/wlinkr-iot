package com.wlinkr.iot.controller;

import com.wlinkr.iot.model.dto.DashboardDto;
import com.wlinkr.iot.security.CurrentUser;
import com.wlinkr.iot.security.UserPrincipal;
import com.wlinkr.iot.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Aggregated IoT dashboard metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard overview for the current user")
    public DashboardDto getDashboard(@CurrentUser UserPrincipal principal) {
        return dashboardService.getDashboard(principal.getId());
    }
}
