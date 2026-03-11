package com.wlinkr.iot.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Core MQTT service – connects to the broker, subscribes to topics,
 * and provides publish capabilities.
 *
 * When the embedded broker is enabled, @DependsOn ensures the broker
 * bean is fully initialised before this service attempts to connect.
 */
@Service
public class MqttService implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final ObjectMapper objectMapper;
    private final MqttMessageRouter messageRouter;

    /**
     * Optional reference to the embedded broker – present only when
     * mqtt.embedded.enabled=true.  Used solely for bean-ordering.
     */
    @Autowired(required = false)
    private io.moquette.broker.Server embeddedMqttBroker;

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.broker.client-id:wlinkr-server}")
    private String clientId;

    @Value("${mqtt.broker.username:}")
    private String username;

    @Value("${mqtt.broker.password:}")
    private String password;

    @Value("${mqtt.topics.sensor-data:wlinkr/devices/+/telemetry}")
    private String sensorDataTopic;

    @Value("${mqtt.topics.status:wlinkr/devices/+/status}")
    private String statusTopic;

    @Value("${mqtt.topics.command-response:wlinkr/devices/+/commands/response}")
    private String commandResponseTopic;

    private MqttClient client;

    public MqttService(ObjectMapper objectMapper, MqttMessageRouter messageRouter) {
        this.objectMapper = objectMapper;
        this.messageRouter = messageRouter;
    }

    @PostConstruct
    public void connect() {
        if (embeddedMqttBroker != null) {
            log.info("Embedded MQTT broker detected – connecting to it");
        }
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            // Set Last Will and Testament for the server
            options.setWill("wlinkr/server/status", "offline".getBytes(), 1, true);

            client.connect(options);
            log.info("Connected to MQTT broker at {}", brokerUrl);
        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker: {}", e.getMessage(), e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
        subscribe();
    }

    private void subscribe() {
        try {
            client.subscribe(sensorDataTopic, 1);
            client.subscribe(statusTopic, 1);
            client.subscribe(commandResponseTopic, 1);
            log.info("Subscribed to topics: {}, {}, {}", sensorDataTopic, statusTopic, commandResponseTopic);
        } catch (MqttException e) {
            log.error("Failed to subscribe to MQTT topics: {}", e.getMessage(), e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.debug("MQTT message received on [{}]: {}", topic, payload);
        messageRouter.route(topic, payload);
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }

    /**
     * Publish a message to a topic.
     */
    public void publish(String topic, Object payload) {
        publish(topic, payload, 1, false);
    }

    public void publish(String topic, Object payload, int qos, boolean retained) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(qos);
            message.setRetained(retained);
            client.publish(topic, message);
            log.debug("Published to [{}]: {}", topic, json);
        } catch (Exception e) {
            log.error("Failed to publish MQTT message to [{}]: {}", topic, e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                log.info("Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            log.error("Error disconnecting from MQTT broker: {}", e.getMessage());
        }
    }
}
