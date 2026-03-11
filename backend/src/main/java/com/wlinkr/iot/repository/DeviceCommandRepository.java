package com.wlinkr.iot.repository;

import com.wlinkr.iot.model.entity.DeviceCommand;
import com.wlinkr.iot.model.enums.CommandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    Page<DeviceCommand> findByDeviceIdOrderByCreatedAtDesc(Long deviceId, Pageable pageable);

    Page<DeviceCommand> findByDeviceIdAndStatusOrderByCreatedAtDesc(
            Long deviceId, CommandStatus status, Pageable pageable);

    @Query("SELECT COUNT(dc) FROM DeviceCommand dc WHERE dc.device.owner.id = :ownerId AND dc.status = :status")
    long countByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") CommandStatus status);
}
