package com.bmsedge.device.service;

import com.bmsedge.device.model.Counter;
import com.bmsedge.device.model.Device;
import com.bmsedge.device.repository.CounterRepository;
import com.bmsedge.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Live Counter Status Service
 * Only shows available MQTT fields: occupancy, inCount (queue length), waitTime
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveCounterStatusService {

    private final DeviceRepository deviceRepository;
    private final CounterRepository counterRepository;
    private final RestTemplate restTemplate;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    public Map<String, Object> getLiveCounterStatus(String[] counterCodes) {
        log.info("Fetching live status");

        List<Counter> counters;
        if (counterCodes != null && counterCodes.length > 0) {
            counters = Arrays.stream(counterCodes)
                    .map(code -> counterRepository.findByCounterCode(code).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            counters = counterRepository.findByActive(true);
        }

        List<Map<String, Object>> counterStatuses = new ArrayList<>();
        for (Counter counter : counters) {
            try {
                Map<String, Object> status = getCounterLiveStatus(counter);
                if (status != null) counterStatuses.add(status);
            } catch (Exception e) {
                log.error("Error for counter {}: {}", counter.getCounterCode(), e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("counterCount", counterStatuses.size());
        response.put("counters", counterStatuses);
        return response;
    }

    private Map<String, Object> getCounterLiveStatus(Counter counter) {
        List<Device> devices = deviceRepository.findByCounterId(counter.getId());
        if (devices.isEmpty()) return null;

        List<Map<String, Object>> deviceStatuses = new ArrayList<>();
        double totalOccupancy = 0;
        long totalQueueLength = 0;
        double totalWaitTime = 0;
        double maxWaitTime = 0;
        int validWaitTimeCount = 0;
        LocalDateTime latestUpdate = null;
        int activeDeviceCount = 0;

        for (Device device : devices) {
            try {
                String url = String.format("%s/api/mqtt-data/device/%s/latest",
                        mqttApiBaseUrl, device.getDeviceId());
                Map<String, Object> data = restTemplate.getForObject(url, Map.class);

                if (data != null && !data.containsKey("status")) {
                    Double occupancy = getNumber(data, "occupancy");
                    Long inCount = getLong(data, "inCount");
                    Double waitTime = getNumber(data, "waitTime");
                    LocalDateTime timestamp = parseTimestamp(data.get("timestamp"));

                    if (occupancy != null) {
                        totalOccupancy += occupancy;
                        activeDeviceCount++;
                    }
                    if (inCount != null) totalQueueLength += inCount;

                    // Aggregate wait time from devices
                    if (waitTime != null && waitTime > 0) {
                        totalWaitTime += waitTime;
                        maxWaitTime = Math.max(maxWaitTime, waitTime);
                        validWaitTimeCount++;
                    }

                    if (timestamp != null && (latestUpdate == null || timestamp.isAfter(latestUpdate))) {
                        latestUpdate = timestamp;
                    }

                    Map<String, Object> deviceStatus = new HashMap<>();
                    deviceStatus.put("deviceId", device.getDeviceId());
                    deviceStatus.put("deviceName", device.getDeviceName());
                    deviceStatus.put("occupancy", occupancy != null ? occupancy : 0);
                    deviceStatus.put("queueLength", inCount != null ? inCount : 0);
                    deviceStatus.put("waitTime", waitTime != null ? waitTime : 0);
                    deviceStatus.put("status", "active");
                    deviceStatus.put("lastUpdated", timestamp);
                    deviceStatuses.add(deviceStatus);
                } else {
                    addInactive(deviceStatuses, device);
                }
            } catch (Exception e) {
                addInactive(deviceStatuses, device);
            }
        }

        // Calculate average wait time from MQTT data
        double avgWaitTime = validWaitTimeCount > 0
                ? Math.round((totalWaitTime / validWaitTimeCount) * 10) / 10.0
                : 0;

        // Fallback estimation based on occupancy
        double estimatedWaitTime = Math.round(totalOccupancy * 2.5 * 10) / 10.0;

        Map<String, Object> counterStatus = new HashMap<>();
        counterStatus.put("counterCode", counter.getCounterCode());
        counterStatus.put("counterName", counter.getCounterName());
        counterStatus.put("counterType", counter.getCounterType());
        counterStatus.put("occupancy", totalOccupancy);
        counterStatus.put("queueLength", totalQueueLength);
        counterStatus.put("waitTime", avgWaitTime > 0 ? avgWaitTime : estimatedWaitTime);
        counterStatus.put("estimatedWaitTime", estimatedWaitTime);
        counterStatus.put("maxWaitTime", maxWaitTime); // Optional: show worst-case wait time
        counterStatus.put("deviceCount", devices.size());
        counterStatus.put("activeDeviceCount", activeDeviceCount);
        counterStatus.put("devices", deviceStatuses);
        counterStatus.put("status", activeDeviceCount > 0 ? "active" : "inactive");
        counterStatus.put("lastUpdated", latestUpdate);
        return counterStatus;
    }

    public Map<String, Object> getOccupancyTrends(String counterCode, LocalDateTime startTime,
                                                  LocalDateTime endTime, String interval) {
        log.info("Fetching occupancy trends: counter={}, start={}, end={}, interval={}",
                counterCode, startTime, endTime, interval);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));
            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            List<Map<String, Object>> allMqttData = new ArrayList<>();
            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s", mqttApiBaseUrl, device.getDeviceId());
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> deviceData = (List<Map<String, Object>>) response.get("data");

                        List<Map<String, Object>> filteredData = deviceData.stream()
                                .filter(data -> data.containsKey("timestamp") &&
                                        isWithinTimeRange(data.get("timestamp"), startTime, endTime))
                                .collect(Collectors.toList());

                        filteredData.forEach(data -> data.put("deviceId", device.getDeviceId()));
                        allMqttData.addAll(filteredData);
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            if (allMqttData.isEmpty()) {
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            List<Map<String, Object>> trends = calculateOccupancyTrends(allMqttData, interval, devices.size());
            Map<String, Object> statistics = calculateOccupancyStatistics(trends);

            Map<String, Object> result = new HashMap<>();
            result.put("counterCode", counterCode);
            result.put("counterName", counter.getCounterName());
            result.put("counterType", counter.getCounterType());
            result.put("deviceCount", devices.size());
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("interval", interval);
            result.put("dataPointCount", trends.size());
            result.put("trends", trends);
            result.put("statistics", statistics);
            return result;
        } catch (Exception e) {
            log.error("Error fetching occupancy trends: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching occupancy trends", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private void addInactive(List<Map<String, Object>> deviceStatuses, Device device) {
        Map<String, Object> status = new HashMap<>();
        status.put("deviceId", device.getDeviceId());
        status.put("deviceName", device.getDeviceName());
        status.put("occupancy", 0);
        status.put("queueLength", 0);
        status.put("waitTime", 0);
        status.put("status", "inactive");
        status.put("lastUpdated", null);
        deviceStatuses.add(status);
    }

    private Double getNumber(Map<String, Object> data, String key) {
        return data.containsKey(key) && data.get(key) instanceof Number ?
                ((Number) data.get(key)).doubleValue() : null;
    }

    private Long getLong(Map<String, Object> data, String key) {
        return data.containsKey(key) && data.get(key) instanceof Number ?
                ((Number) data.get(key)).longValue() : null;
    }

    private LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) return null;
        try {
            return LocalDateTime.parse(timestamp.toString(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isWithinTimeRange(Object timestamp, LocalDateTime start, LocalDateTime end) {
        try {
            LocalDateTime dataTime = LocalDateTime.parse(timestamp.toString(), DateTimeFormatter.ISO_DATE_TIME);
            return !dataTime.isBefore(start) && !dataTime.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> calculateOccupancyTrends(List<Map<String, Object>> mqttData,
                                                               String interval, int deviceCount) {
        int minutes = parseIntervalToMinutes(interval);
        Map<LocalDateTime, Map<String, Double>> bucketDeviceMap = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp") && data.containsKey("deviceId")) {
                Double occupancy = extractOccupancy(data);
                if (occupancy == null) continue;

                LocalDateTime timestamp = LocalDateTime.parse(data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);
                LocalDateTime bucket = roundToInterval(timestamp, minutes);
                bucketDeviceMap.computeIfAbsent(bucket, k -> new HashMap<>())
                        .put((String) data.get("deviceId"), occupancy);
            }
        }

        return bucketDeviceMap.entrySet().stream().map(entry -> {
            Map<String, Double> deviceOccupancies = entry.getValue();
            double totalOccupancy = deviceOccupancies.values().stream().mapToDouble(Double::doubleValue).sum();
            DoubleSummaryStatistics stats = deviceOccupancies.values().stream()
                    .mapToDouble(Double::doubleValue).summaryStatistics();

            Map<String, Object> trend = new HashMap<>();
            trend.put("timestamp", entry.getKey());
            trend.put("totalOccupancy", totalOccupancy);
            trend.put("averageOccupancy", stats.getAverage());
            trend.put("maxOccupancy", stats.getMax());
            trend.put("minOccupancy", stats.getMin());
            trend.put("activeDeviceCount", deviceOccupancies.size());
            return trend;
        }).collect(Collectors.toList());
    }

    private Double extractOccupancy(Map<String, Object> data) {
        return data.containsKey("occupancy") && data.get("occupancy") instanceof Number ?
                ((Number) data.get("occupancy")).doubleValue() : null;
    }

    private Map<String, Object> calculateOccupancyStatistics(List<Map<String, Object>> trends) {
        Map<String, Object> statistics = new HashMap<>();
        if (!trends.isEmpty()) {
            DoubleSummaryStatistics totalStats = trends.stream()
                    .mapToDouble(t -> ((Number) t.get("totalOccupancy")).doubleValue()).summaryStatistics();
            statistics.put("averageTotalOccupancy", totalStats.getAverage());
            statistics.put("maxTotalOccupancy", totalStats.getMax());
            statistics.put("minTotalOccupancy", totalStats.getMin());

            DoubleSummaryStatistics avgStats = trends.stream()
                    .mapToDouble(t -> ((Number) t.get("averageOccupancy")).doubleValue()).summaryStatistics();
            statistics.put("averageOccupancyPerDevice", avgStats.getAverage());
        }
        return statistics;
    }

    private int parseIntervalToMinutes(String interval) {
        return switch (interval.toLowerCase()) {
            case "15min" -> 15;
            case "30min" -> 30;
            case "1hour" -> 60;
            case "4hour" -> 240;
            case "1day" -> 1440;
            default -> 60;
        };
    }

    private LocalDateTime roundToInterval(LocalDateTime timestamp, int intervalMinutes) {
        int minute = timestamp.getMinute();
        int roundedMinute = (minute / intervalMinutes) * intervalMinutes;
        return timestamp.truncatedTo(ChronoUnit.HOURS).plusMinutes(roundedMinute);
    }

    private Map<String, Object> createEmptyResponse(String counterCode, String counterName) {
        Map<String, Object> response = new HashMap<>();
        response.put("counterCode", counterCode);
        response.put("counterName", counterName);
        response.put("dataPointCount", 0);
        response.put("trends", Collections.emptyList());
        response.put("statistics", Collections.emptyMap());
        return response;
    }
}