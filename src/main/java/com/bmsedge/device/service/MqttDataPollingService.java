package com.bmsedge.device.service;

import com.bmsedge.device.controller.DeviceWebSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service to poll MQTT API and push updates via WebSocket
 *
 * This service bridges the MQTT data from the MQTT module to WebSocket clients
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MqttDataPollingService {

    private final DeviceWebSocketController webSocketController;
    private final RestTemplate restTemplate;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    @Value("${mqtt.polling.enabled:true}")
    private boolean pollingEnabled;

    @Value("${mqtt.polling.limit:10}")
    private int pollingLimit;

    /**
     * Poll MQTT API for recent data and broadcast via WebSocket
     * Runs every 2 seconds
     */
    @Scheduled(fixedDelay = 2000)
    public void pollAndBroadcastRecentData() {
        if (!pollingEnabled) {
            log.trace("MQTT polling is disabled");
            return;
        }

        try {
            String url = mqttApiBaseUrl + "/api/mqtt-data/recent?limit=" + pollingLimit;
            log.trace("Polling MQTT data from: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.debug("No response received from MQTT API");
                return;
            }

            Object dataObj = response.get("data");

            if (!(dataObj instanceof Iterable<?> dataList)) {
                log.warn("Unexpected response format from MQTT API - 'data' field is not iterable");
                return;
            }

            int processedCount = 0;
            for (Object item : dataList) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    log.debug("Skipping non-map item in data list");
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) rawMap;

                    String deviceId = (String) data.get("deviceId");
                    String counterName = (String) data.get("counterName");

                    // Broadcast to device-specific subscribers
                    if (deviceId != null && !deviceId.trim().isEmpty()) {
                        webSocketController.broadcastDeviceData(deviceId, data);
                        log.trace("Broadcast data for device: {}", deviceId);
                    }

                    // Broadcast to counter-specific subscribers
                    if (counterName != null && !counterName.trim().isEmpty()) {
                        webSocketController.broadcastCounterData(counterName, data);
                        log.trace("Broadcast data for counter: {}", counterName);
                    }

                    // Broadcast to all subscribers
                    webSocketController.broadcastToAll(data);
                    processedCount++;

                } catch (Exception itemError) {
                    log.warn("Error processing individual MQTT data item: {}", itemError.getMessage());
                    // Continue processing other items
                }
            }

            if (processedCount > 0) {
                log.debug("Successfully processed and broadcast {} MQTT data items", processedCount);
            }

        } catch (RestClientException e) {
            log.error("REST client error polling MQTT data from {}: {}", mqttApiBaseUrl, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error polling MQTT data: {}", e.getMessage(), e);
        }
    }
}