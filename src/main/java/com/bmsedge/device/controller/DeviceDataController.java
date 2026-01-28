package com.bmsedge.device.controller;

import com.bmsedge.device.dto.DeviceDTO;
import com.bmsedge.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Device Data Controller - Integrates with MQTT Data API
 */
@RestController
@RequestMapping("/api/device-data")
@RequiredArgsConstructor
@Slf4j
public class DeviceDataController {

    private final DeviceService deviceService;
    private final RestTemplate restTemplate;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    /**
     * Get device with latest MQTT data
     * GET /api/device-data/{deviceId}/with-mqtt-data
     */
    @GetMapping("/{deviceId}/with-mqtt-data")
    public ResponseEntity<?> getDeviceWithMqttData(@PathVariable("deviceId") String deviceId) {
        try {
            log.info("Fetching device {} with MQTT data", deviceId);

            // Validate deviceId
            if (deviceId == null || deviceId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device ID cannot be null or empty"));
            }

            // Get device details
            DeviceDTO device = deviceService.getDeviceByDeviceId(deviceId);

            if (device == null) {
                log.warn("Device not found: {}", deviceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Device not found with ID: " + deviceId));
            }

            // Get latest MQTT data for this device
            String mqttDataUrl = mqttApiBaseUrl + "/api/mqtt-data/device/" + deviceId + "/latest";

            Map<String, Object> response = new HashMap<>();
            response.put("device", device);
            response.put("deviceId", deviceId);

            try {
                log.debug("Fetching MQTT data from: {}", mqttDataUrl);
                Object mqttData = restTemplate.getForObject(mqttDataUrl, Object.class);
                response.put("latestMqttData", mqttData);
                response.put("mqttDataAvailable", true);
                log.info("Successfully fetched MQTT data for device: {}", deviceId);
            } catch (Exception e) {
                log.warn("No MQTT data found for device {}: {}", deviceId, e.getMessage());
                response.put("latestMqttData", null);
                response.put("mqttDataAvailable", false);
                response.put("mqttDataError", e.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching device with MQTT data for device ID {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching device data: " + e.getMessage()));
        }
    }

    /**
     * Get device by counter with latest MQTT data
     * GET /api/device-data/counter/{counterName}/with-mqtt-data
     */
    @GetMapping("/counter/{counterName}/with-mqtt-data")
    public ResponseEntity<?> getDeviceByCounterWithMqttData(@PathVariable("counterName") String counterName) {
        try {
            log.info("Fetching devices for counter {} with MQTT data", counterName);

            // Validate counterName
            if (counterName == null || counterName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter name cannot be null or empty"));
            }

            // Get devices by counter
            List<DeviceDTO> devices = deviceService.getDevicesByCounter(counterName);

            if (devices == null || devices.isEmpty()) {
                log.warn("No devices found for counter: {}", counterName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("No devices found for counter: " + counterName));
            }

            // Get latest MQTT data for this counter
            String mqttDataUrl = mqttApiBaseUrl + "/api/mqtt-data/counter/" + counterName + "/latest";

            Map<String, Object> response = new HashMap<>();
            response.put("devices", devices);
            response.put("counterName", counterName);
            response.put("deviceCount", devices.size());

            try {
                log.debug("Fetching MQTT data from: {}", mqttDataUrl);
                Object mqttData = restTemplate.getForObject(mqttDataUrl, Object.class);
                response.put("latestMqttData", mqttData);
                response.put("mqttDataAvailable", true);
                log.info("Successfully fetched MQTT data for counter: {}", counterName);
            } catch (Exception e) {
                log.warn("No MQTT data found for counter {}: {}", counterName, e.getMessage());
                response.put("latestMqttData", null);
                response.put("mqttDataAvailable", false);
                response.put("mqttDataError", e.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching devices by counter with MQTT data for counter {}: {}", counterName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching counter data: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", "error");
        return errorResponse;
    }
}