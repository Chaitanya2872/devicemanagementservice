package com.bmsedge.device.controller;

import com.bmsedge.device.dto.DeviceDTO;
import com.bmsedge.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<?> createDevice(@Valid @RequestBody DeviceDTO deviceDTO) {
        try {
            log.info("REST request to create device: {}", deviceDTO.getDeviceId());
            DeviceDTO result = deviceService.createDevice(deviceDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating device: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating device: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDevice(@PathVariable("id") Long id) {
        try {
            log.info("REST request to get device: {}", id);
            DeviceDTO device = deviceService.getDeviceById(id);
            return ResponseEntity.ok(device);
        } catch (IllegalArgumentException e) {
            log.error("Device not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting device with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting device: " + e.getMessage()));
        }
    }

    @GetMapping("/device-id/{deviceId}")
    public ResponseEntity<?> getDeviceByDeviceId(@PathVariable("deviceId") String deviceId) {
        try {
            log.info("REST request to get device by device ID: {}", deviceId);

            if (deviceId == null || deviceId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device ID cannot be null or empty"));
            }

            DeviceDTO device = deviceService.getDeviceByDeviceId(deviceId);

            if (device == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Device not found with device ID: " + deviceId));
            }

            return ResponseEntity.ok(device);
        } catch (Exception e) {
            log.error("Error getting device by device ID: {}", deviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting device: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllDevices() {
        try {
            log.info("REST request to get all devices");
            List<DeviceDTO> devices = deviceService.getAllDevices();
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error getting all devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting devices: " + e.getMessage()));
        }
    }

    @GetMapping("/location/{locationCode}")
    public ResponseEntity<?> getDevicesByLocation(@PathVariable("locationCode") String locationCode) {
        try {
            log.info("REST request to get devices by location: {}", locationCode);
            List<DeviceDTO> devices = deviceService.getDevicesByLocation(locationCode);
            return ResponseEntity.ok(devices);
        } catch (IllegalArgumentException e) {
            log.error("Invalid location code: {}", locationCode);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting devices by location: {}", locationCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting devices: " + e.getMessage()));
        }
    }

    @GetMapping("/segment/{segmentCode}")
    public ResponseEntity<?> getDevicesBySegment(@PathVariable("segmentCode") String segmentCode) {
        try {
            log.info("REST request to get devices by segment: {}", segmentCode);
            List<DeviceDTO> devices = deviceService.getDevicesBySegment(segmentCode);
            return ResponseEntity.ok(devices);
        } catch (IllegalArgumentException e) {
            log.error("Invalid segment code: {}", segmentCode);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting devices by segment: {}", segmentCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting devices: " + e.getMessage()));
        }
    }

    @GetMapping("/counter/{counterCode}")
    public ResponseEntity<?> getDevicesByCounter(@PathVariable("counterCode") String counterCode) {
        try {
            log.info("REST request to get devices by counter: {}", counterCode);
            List<DeviceDTO> devices = deviceService.getDevicesByCounter(counterCode);
            return ResponseEntity.ok(devices);
        } catch (IllegalArgumentException e) {
            log.error("Invalid counter code: {}", counterCode);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting devices by counter: {}", counterCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting devices: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDevices(@RequestParam("term") String term) {
        try {
            log.info("REST request to search devices with term: {}", term);
            List<DeviceDTO> devices = deviceService.searchDevices(term);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error searching devices with term: {}", term, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error searching devices: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDevice(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeviceDTO deviceDTO
    ) {
        try {
            log.info("REST request to update device: {}", id);
            DeviceDTO result = deviceService.updateDevice(id, deviceDTO);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating device: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating device with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating device: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable("id") Long id) {
        try {
            log.info("REST request to delete device: {}", id);
            deviceService.deleteDevice(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Device not found for deletion: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting device with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deleting device: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}