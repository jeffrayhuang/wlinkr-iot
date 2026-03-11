package com.wlinkr.iot.config;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * Starts an embedded Moquette MQTT broker when mqtt.embedded.enabled=true.
 * For production, set mqtt.embedded.enabled=false and point mqtt.broker.url to an external broker.
 */
@Configuration
@ConditionalOnProperty(name = "mqtt.embedded.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedMqttBrokerConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedMqttBrokerConfig.class);

    @Value("${mqtt.embedded.host:0.0.0.0}")
    private String host;

    @Value("${mqtt.embedded.port:1883}")
    private int port;

    @Value("${mqtt.embedded.websocket-port:8883}")
    private int websocketPort;

    private Server mqttBroker;

    @Bean
    public Server embeddedMqttBroker() throws IOException {
        Properties props = new Properties();
        props.setProperty("host", host);
        props.setProperty("port", String.valueOf(port));
        props.setProperty("websocket_port", String.valueOf(websocketPort));
        props.setProperty("allow_anonymous", "true");
        props.setProperty("allow_zero_byte_client_id", "true");

        mqttBroker = new Server();
        mqttBroker.startServer(new MemoryConfig(props));
        log.info("Embedded MQTT broker started on tcp://{}:{} and ws://{}:{}",
                host, port, host, websocketPort);
        return mqttBroker;
    }

    @PreDestroy
    public void stopBroker() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            log.info("Embedded MQTT broker stopped");
        }
    }
}
