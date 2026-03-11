package com.wlinkr.iot.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sensor_data")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @NotBlank
    @Column(nullable = false)
    private String metricName;

    @NotNull
    @Column(nullable = false)
    private Double metricValue;

    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();
}
