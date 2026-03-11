package com.wlinkr.iot.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Routes incoming MQTT messages to the appropriate handler
 * based on the topic pattern.
 */
@Component
public class MqttMessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageRouter.class);

    // Topic patterns: wlinkr/devices/{serialNumber}/telemetry|commands|status
    private static final Pattern TELEMETRY_PATTERN =
            Pattern.compile("^wlinkr/devices/([^/]+)/telemetry$");
    private static final Pattern STATUS_PATTERN =
            Pattern.compile("^wlinkr/devices/([^/]+)/status$");
    private static final Pattern COMMAND_RESPONSE_PATTERN =
            Pattern.compile("^wlinkr/devices/([^/]+)/commands/response$");

    private final SensorDataMqttHandler sensorDataHandler;
    private final DeviceStatusMqttHandler deviceStatusHandler;
    private final CommandResponseMqttHandler commandResponseHandler;

    public MqttMessageRouter(SensorDataMqttHandler sensorDataHandler,
                             DeviceStatusMqttHandler deviceStatusHandler,
                             CommandResponseMqttHandler commandResponseHandler) {
        this.sensorDataHandler = sensorDataHandler;
        this.deviceStatusHandler = deviceStatusHandler;
        this.commandResponseHandler = commandResponseHandler;
    }

    public void route(String topic, String payload) {
        try {
            Matcher telemetryMatcher = TELEMETRY_PATTERN.matcher(topic);
            if (telemetryMatcher.matches()) {
                String serialNumber = telemetryMatcher.group(1);
                sensorDataHandler.handle(serialNumber, payload);
                return;
            }

            Matcher statusMatcher = STATUS_PATTERN.matcher(topic);
            if (statusMatcher.matches()) {
                String serialNumber = statusMatcher.group(1);
                deviceStatusHandler.handle(serialNumber, payload);
                return;
            }

            Matcher commandResponseMatcher = COMMAND_RESPONSE_PATTERN.matcher(topic);
            if (commandResponseMatcher.matches()) {
                String serialNumber = commandResponseMatcher.group(1);
                commandResponseHandler.handle(serialNumber, payload);
                return;
            }

            log.warn("No handler found for MQTT topic: {}", topic);
        } catch (Exception e) {
            log.error("Error routing MQTT message on topic [{}]: {}", topic, e.getMessage(), e);
        }
    }
}
