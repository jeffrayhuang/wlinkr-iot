package com.wlinkr.iot.model.entity;

import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_type")
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_status")
    private DeviceStatus status;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String serialNumber;

    private String firmwareVersion;

    private String location;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
