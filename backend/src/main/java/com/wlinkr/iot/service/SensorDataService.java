package com.wlinkr.iot.service;

import com.wlinkr.iot.exception.ResourceNotFoundException;
import com.wlinkr.iot.model.dto.CreateSensorDataRequest;
import com.wlinkr.iot.model.dto.SensorDataDto;
import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.entity.SensorData;
import com.wlinkr.iot.repository.DeviceRepository;
import com.wlinkr.iot.repository.SensorDataRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;
    private final DeviceRepository deviceRepository;

    public SensorDataService(SensorDataRepository sensorDataRepository,
                             DeviceRepository deviceRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sensor-data", key = "#deviceId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<SensorDataDto> getSensorData(Long deviceId, Long ownerId, Pageable pageable) {
        Device device = findDeviceChecked(deviceId, ownerId);
        return sensorDataRepository.findByDeviceIdOrderByRecordedAtDesc(device.getId(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<SensorDataDto> getSensorDataRange(Long deviceId, Long ownerId,
                                                   Instant from, Instant to) {
        Device device = findDeviceChecked(deviceId, ownerId);
        return sensorDataRepository.findByDeviceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                device.getId(), from, to
        ).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SensorDataDto> getLatestMetric(Long deviceId, Long ownerId,
                                                String metricName, int limit) {
        Device device = findDeviceChecked(deviceId, ownerId);
        return sensorDataRepository.findLatestByMetric(device.getId(), metricName, PageRequest.of(0, limit))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @CacheEvict(value = {"sensor-data", "dashboard"}, allEntries = true)
    public SensorDataDto ingestData(Long deviceId, Long ownerId, CreateSensorDataRequest request) {
        Device device = findDeviceChecked(deviceId, ownerId);

        SensorData data = SensorData.builder()
                .device(device)
                .metricName(request.metricName())
                .metricValue(request.metricValue())
                .unit(request.unit())
                .recordedAt(Instant.now())
                .build();

        return toDto(sensorDataRepository.save(data));
    }

    private Device findDeviceChecked(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        if (!device.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Device", "id", deviceId);
        }
        return device;
    }

    private SensorDataDto toDto(SensorData sd) {
        return new SensorDataDto(
                sd.getId(), sd.getDevice().getId(),
                sd.getMetricName(), sd.getMetricValue(),
                sd.getUnit(), sd.getRecordedAt()
        );
    }
}
