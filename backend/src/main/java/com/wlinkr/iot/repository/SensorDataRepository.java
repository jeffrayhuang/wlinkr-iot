package com.wlinkr.iot.repository;

import com.wlinkr.iot.model.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Page<SensorData> findByDeviceIdOrderByRecordedAtDesc(Long deviceId, Pageable pageable);

    List<SensorData> findByDeviceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long deviceId, Instant from, Instant to);

    @Query("SELECT sd FROM SensorData sd WHERE sd.device.id = :deviceId " +
           "AND sd.metricName = :metric ORDER BY sd.recordedAt DESC")
    List<SensorData> findLatestByMetric(@Param("deviceId") Long deviceId,
                                        @Param("metric") String metric,
                                        Pageable pageable);

    @Query("SELECT COUNT(sd) FROM SensorData sd WHERE sd.device.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);
}
