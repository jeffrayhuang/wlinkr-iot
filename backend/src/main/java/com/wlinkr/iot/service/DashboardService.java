package com.wlinkr.iot.service;

import com.wlinkr.iot.model.dto.DashboardDto;
import com.wlinkr.iot.model.enums.CommandStatus;
import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.repository.DeviceCommandRepository;
import com.wlinkr.iot.repository.DeviceRepository;
import com.wlinkr.iot.repository.SensorDataRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final DeviceCommandRepository commandRepository;

    public DashboardService(DeviceRepository deviceRepository,
                            SensorDataRepository sensorDataRepository,
                            DeviceCommandRepository commandRepository) {
        this.deviceRepository = deviceRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.commandRepository = commandRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#ownerId")
    public DashboardDto getDashboard(Long ownerId) {
        long total = deviceRepository.countByOwnerId(ownerId);
        long online = deviceRepository.countByOwnerIdAndStatus(ownerId, DeviceStatus.ONLINE);
        long offline = deviceRepository.countByOwnerIdAndStatus(ownerId, DeviceStatus.OFFLINE);
        long error = deviceRepository.countByOwnerIdAndStatus(ownerId, DeviceStatus.ERROR);
        long readings = sensorDataRepository.countByOwnerId(ownerId);
        long pending = commandRepository.countByOwnerIdAndStatus(ownerId, CommandStatus.PENDING);

        Map<String, Long> byType = deviceRepository.countByOwnerGroupedByType(ownerId).stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        return new DashboardDto(total, online, offline, error, readings, pending, byType);
    }
}
