package com.wlinkr.iot.mqtt;

import com.wlinkr.iot.model.entity.Device;
import com.wlinkr.iot.model.entity.User;
import com.wlinkr.iot.model.enums.AuthProvider;
import com.wlinkr.iot.model.enums.DeviceStatus;
import com.wlinkr.iot.model.enums.DeviceType;
import com.wlinkr.iot.repository.DeviceRepository;
import com.wlinkr.iot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-registers unknown devices that publish via MQTT.
 *
 * When a device with an unrecognised serial number sends telemetry or
 * status, a new Device record is created with default values and
 * assigned to a "system" user.  The system user is created on first
 * use if it does not exist yet.
 *
 * Set mqtt.auto-register.enabled=false to disable (the handlers will
 * log a warning and discard the message instead).
 */
@Component
public class DeviceAutoRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DeviceAutoRegistrar.class);

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Value("${mqtt.auto-register.enabled:true}")
    private boolean autoRegisterEnabled;

    @Value("${mqtt.auto-register.system-user-email:system@wlinkr.local}")
    private String systemUserEmail;

    @Value("${mqtt.auto-register.default-device-type:SENSOR}")
    private String defaultDeviceType;

    public DeviceAutoRegistrar(DeviceRepository deviceRepository,
                                UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    public boolean isEnabled() {
        return autoRegisterEnabled;
    }

    /**
     * Returns the existing device or auto-registers a new one.
     * Returns null if auto-registration is disabled and the device
     * does not exist.
     */
    @Transactional
    public Device findOrRegister(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber)
                .orElseGet(() -> {
                    if (!autoRegisterEnabled) {
                        return null;
                    }
                    return register(serialNumber);
                });
    }

    private Device register(String serialNumber) {
        User systemUser = getOrCreateSystemUser();

        DeviceType type;
        try {
            type = DeviceType.valueOf(defaultDeviceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = DeviceType.SENSOR;
        }

        Device device = Device.builder()
                .name("Auto-" + serialNumber)
                .serialNumber(serialNumber)
                .deviceType(type)
                .status(DeviceStatus.ONLINE)
                .description("Auto-registered via MQTT")
                .owner(systemUser)
                .build();

        device = deviceRepository.save(device);
        log.info("Auto-registered new device: {} (id={}, owner={})",
                serialNumber, device.getId(), systemUser.getEmail());
        return device;
    }

    private User getOrCreateSystemUser() {
        return userRepository.findByEmail(systemUserEmail)
                .orElseGet(() -> {
                    User sys = User.builder()
                            .name("System")
                            .email(systemUserEmail)
                            .provider(AuthProvider.LOCAL)
                            .providerId("system")
                            .build();
                    sys = userRepository.save(sys);
                    log.info("Created system user: {}", systemUserEmail);
                    return sys;
                });
    }
}
