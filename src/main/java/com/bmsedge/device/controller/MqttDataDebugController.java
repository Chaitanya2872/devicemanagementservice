package com.bmsedge.device.controller;

import com.bmsedge.device.model.Counter;
import com.bmsedge.device.model.Device;
import com.bmsedge.device.repository.CounterRepository;
import com.bmsedge.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT Data Debug Controller
 * Test MQTT data availability and connectivity
 */
@RestController
@RequestMapping("/api/counter-analytics/debug")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MqttDataDebugController {

    private final CounterRepository counterRepository;
    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    /**
     * Check MQTT data for a specific counter
     * GET /api/counter-analytics/debug/counter/{counterCode}/mqtt-data
     */
    @GetMapping("/counter/{counterCode}/mqtt-data")
    public ResponseEntity<?> checkMqttData(@PathVariable("counterCode") String counterCode) {
        try {
            counterCode = counterCode.trim();
            log.info("Checking MQTT data for counter: {}", counterCode);

            // Get counter
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElse(null);

            if (counter == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Counter not found: " + counterCode));
            }

            // Get devices
            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("counterCode", counter.getCounterCode());
            response.put("counterName", counter.getCounterName());
            response.put("deviceCount", devices.size());

            if (devices.isEmpty()) {
                response.put("error", "No devices found for this counter");
                return ResponseEntity.ok(response);
            }

            // List devices
            List<Map<String, String>> deviceList = devices.stream()
                    .map(d -> Map.of(
                            "deviceId", d.getDeviceId(),
                            "deviceName", d.getDeviceName()
                    ))
                    .toList();
            response.put("devices", deviceList);

            // Test MQTT API for each device
            List<Map<String, Object>> mqttTests = new java.util.ArrayList<>();

            for (Device device : devices) {
                Map<String, Object> test = new HashMap<>();
                test.put("deviceId", device.getDeviceId());
                test.put("deviceName", device.getDeviceName());

                try {
                    // Try to get latest data
                    String latestUrl = String.format("%s/api/mqtt-data/device/%s/latest",
                            mqttApiBaseUrl, device.getDeviceId());
                    log.debug("Testing MQTT URL: {}", latestUrl);

                    Map<String, Object> mqttData = restTemplate.getForObject(latestUrl, Map.class);
                    test.put("mqttDataAvailable", mqttData != null);
                    test.put("latestData", mqttData);

                    // Try to get range data (last 24 hours)
                    LocalDateTime endTime = LocalDateTime.now();
                    LocalDateTime startTime = endTime.minusHours(24);
                    String rangeUrl = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                            mqttApiBaseUrl, device.getDeviceId(), startTime, endTime);

                    log.debug("Testing MQTT range URL: {}", rangeUrl);
                    List<Map<String, Object>> rangeData = restTemplate.getForObject(rangeUrl, List.class);
                    test.put("rangeDataCount", rangeData != null ? rangeData.size() : 0);

                    if (rangeData != null && !rangeData.isEmpty()) {
                        test.put("sampleData", rangeData.get(0));
                    }

                } catch (Exception e) {
                    test.put("error", e.getMessage());
                    test.put("mqttDataAvailable", false);
                    log.error("Error fetching MQTT data for device {}: {}", device.getDeviceId(), e.getMessage());
                }

                mqttTests.add(test);
            }

            response.put("mqttTests", mqttTests);
            response.put("mqttApiBaseUrl", mqttApiBaseUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking MQTT data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test MQTT API connectivity
     * GET /api/counter-analytics/debug/mqtt-api/health
     */
    @GetMapping("/mqtt-api/health")
    public ResponseEntity<?> testMqttApiHealth() {
        try {
            log.info("Testing MQTT API health at: {}", mqttApiBaseUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("mqttApiBaseUrl", mqttApiBaseUrl);

            try {
                // Try to call a health endpoint or any endpoint
                String healthUrl = mqttApiBaseUrl + "/actuator/health";
                log.debug("Testing health URL: {}", healthUrl);

                Map<String, Object> healthData = restTemplate.getForObject(healthUrl, Map.class);
                response.put("mqttApiAvailable", true);
                response.put("healthCheck", healthData);

            } catch (Exception e) {
                response.put("mqttApiAvailable", false);
                response.put("error", e.getMessage());

                // Try alternative endpoint
                try {
                    String alternativeUrl = mqttApiBaseUrl + "/api/mqtt-data/recent?limit=1";
                    log.debug("Testing alternative URL: {}", alternativeUrl);

                    Map<String, Object> testData = restTemplate.getForObject(alternativeUrl, Map.class);
                    response.put("mqttApiAvailable", true);
                    response.put("alternativeEndpointWorking", true);
                    response.put("testData", testData);

                } catch (Exception e2) {
                    response.put("alternativeEndpointError", e2.getMessage());
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing MQTT API: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all available MQTT data (recent)
     * GET /api/counter-analytics/debug/mqtt-api/recent
     */
    @GetMapping("/mqtt-api/recent")
    public ResponseEntity<?> getRecentMqttData(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Fetching recent MQTT data (limit: {})", limit);

            String url = String.format("%s/api/mqtt-data/recent?limit=%d", mqttApiBaseUrl, limit);
            log.debug("MQTT URL: {}", url);

            Map<String, Object> mqttData = restTemplate.getForObject(url, Map.class);

            Map<String, Object> response = new HashMap<>();
            response.put("mqttApiBaseUrl", mqttApiBaseUrl);
            response.put("requestedLimit", limit);
            response.put("data", mqttData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching recent MQTT data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "mqttApiBaseUrl", mqttApiBaseUrl
                    ));
        }
    }
}