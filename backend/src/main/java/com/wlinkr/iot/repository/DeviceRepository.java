package com.wlinkr.iot.repository;

import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Page<Device> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Device> findByOwnerIdAndStatus(Long ownerId, DeviceStatus status, Pageable pageable);

    Page<Device> findByOwnerIdAndDeviceType(Long ownerId, DeviceType type, Pageable pageable);

    long countByOwnerIdAndStatus(Long ownerId, DeviceStatus status);

    long countByOwnerId(Long ownerId);

    @Query("SELECT d.deviceType, COUNT(d) FROM Device d WHERE d.owner.id = :ownerId GROUP BY d.deviceType")
    java.util.List<Object[]> countByOwnerGroupedByType(@Param("ownerId") Long ownerId);

    boolean existsBySerialNumber(String serialNumber);

    Optional<Device> findBySerialNumber(String serialNumber);
}
