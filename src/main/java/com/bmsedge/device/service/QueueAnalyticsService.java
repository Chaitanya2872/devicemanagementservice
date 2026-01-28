package com.bmsedge.device.service;

import com.bmsedge.device.dto.*;
import com.bmsedge.device.model.Device;
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
 * Queue Analytics Service
 * Handles queue trend analysis, comparisons, and averages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueAnalyticsService {

    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    /**
     * Get queue trends for a device with time filtering
     */
    public List<QueueTrendDTO> getQueueTrends(
            String deviceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval) {

        log.info("Fetching queue trends: device={}, start={}, end={}, interval={}",
                deviceId, startTime, endTime, interval);

        try {
            // Fetch MQTT data for the time range
            String url = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                    mqttApiBaseUrl, deviceId, startTime, endTime);

            List<Map<String, Object>> mqttData = restTemplate.getForObject(url, List.class);

            if (mqttData == null || mqttData.isEmpty()) {
                log.warn("No MQTT data found for device: {}", deviceId);
                return Collections.emptyList();
            }

            // Group data by interval
            return groupByInterval(mqttData, interval, deviceId);

        } catch (Exception e) {
            log.error("Error fetching queue trends for device {}: {}", deviceId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get queue comparison between multiple devices
     */
    public List<QueueComparisonDTO> getQueueComparison(
            String[] deviceIds,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("Fetching queue comparison for {} devices", deviceIds.length);

        List<QueueComparisonDTO> comparisons = new ArrayList<>();

        for (String deviceId : deviceIds) {
            try {
                // Get device info
                Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);

                // Get MQTT data
                String url = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                        mqttApiBaseUrl, deviceId, startTime, endTime);

                List<Map<String, Object>> mqttData = restTemplate.getForObject(url, List.class);

                if (mqttData != null && !mqttData.isEmpty()) {
                    // Calculate statistics
                    DoubleSummaryStatistics stats = mqttData.stream()
                            .filter(data -> data.containsKey("queueLength"))
                            .mapToDouble(data -> ((Number) data.get("queueLength")).doubleValue())
                            .summaryStatistics();

                    comparisons.add(QueueComparisonDTO.builder()
                            .deviceId(deviceId)
                            .deviceName(device != null ? device.getDeviceName() : deviceId)
                            .location(device != null && device.getLocation() != null ?
                                    device.getLocation().getLocationName() : null)
                            .averageQueueLength(stats.getAverage())
                            .maxQueueLength(stats.getMax())
                            .minQueueLength(stats.getMin())
                            .totalReadings((int) stats.getCount())
                            .firstReading(startTime)
                            .lastReading(endTime)
                            .build());
                }

            } catch (Exception e) {
                log.error("Error processing device {} in comparison: {}", deviceId, e.getMessage());
            }
        }

        // Sort by average queue length
        comparisons.sort(Comparator.comparingDouble(QueueComparisonDTO::getAverageQueueLength));

        return comparisons;
    }

    /**
     * Get average queue data for location/segment
     */
    public List<AverageQueueDTO> getAverageQueue(
            String location,
            String segment,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String groupBy) {

        log.info("Fetching average queue: location={}, segment={}, groupBy={}",
                location, segment, groupBy);

        try {
            // Find devices by location or segment using codes
            List<Device> devices;
            if (location != null && !location.isEmpty()) {
                // Assuming location is passed as locationCode
                devices = deviceRepository.findByLocationCode(location);
            } else {
                // Assuming segment is passed as segmentCode
                devices = deviceRepository.findBySegmentCode(segment);
            }

            if (devices.isEmpty()) {
                log.warn("No devices found for location={}, segment={}", location, segment);
                return Collections.emptyList();
            }

            // Get MQTT data for all devices
            List<Map<String, Object>> allMqttData = new ArrayList<>();
            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                            mqttApiBaseUrl, device.getDeviceId(), startTime, endTime);

                    List<Map<String, Object>> deviceData = restTemplate.getForObject(url, List.class);
                    if (deviceData != null) {
                        // Add device info to each data point
                        deviceData.forEach(data -> {
                            data.put("deviceId", device.getDeviceId());
                            data.put("location", device.getLocation() != null ?
                                    device.getLocation().getLocationName() : null);
                            data.put("segment", device.getSegment() != null ?
                                    device.getSegment().getSegmentName() : null);
                        });
                        allMqttData.addAll(deviceData);
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            // Group and average by time period
            return groupAndAverageByPeriod(allMqttData, groupBy, location, segment, devices.size());

        } catch (Exception e) {
            log.error("Error fetching average queue: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get hourly pattern for a device
     */
    public List<Map<String, Object>> getHourlyPattern(
            String deviceId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("Fetching hourly pattern for device: {}", deviceId);

        try {
            // Get MQTT data
            String url = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                    mqttApiBaseUrl, deviceId, startTime, endTime);

            List<Map<String, Object>> mqttData = restTemplate.getForObject(url, List.class);

            if (mqttData == null || mqttData.isEmpty()) {
                return Collections.emptyList();
            }

            // Group by hour of day (0-23)
            Map<Integer, List<Double>> hourlyData = new HashMap<>();

            for (Map<String, Object> data : mqttData) {
                if (data.containsKey("timestamp") && data.containsKey("queueLength")) {
                    LocalDateTime timestamp = LocalDateTime.parse(
                            data.get("timestamp").toString(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    int hour = timestamp.getHour();
                    double queueLength = ((Number) data.get("queueLength")).doubleValue();

                    hourlyData.computeIfAbsent(hour, k -> new ArrayList<>()).add(queueLength);
                }
            }

            // Calculate averages for each hour
            List<Map<String, Object>> pattern = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) {
                List<Double> values = hourlyData.getOrDefault(hour, Collections.emptyList());

                if (!values.isEmpty()) {
                    DoubleSummaryStatistics stats = values.stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    Map<String, Object> hourData = new HashMap<>();
                    hourData.put("hour", hour);
                    hourData.put("averageQueueLength", stats.getAverage());
                    hourData.put("maxQueueLength", stats.getMax());
                    hourData.put("minQueueLength", stats.getMin());
                    hourData.put("dataPoints", stats.getCount());

                    pattern.add(hourData);
                }
            }

            return pattern;

        } catch (Exception e) {
            log.error("Error fetching hourly pattern for device {}: {}", deviceId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get queue statistics summary
     */
    public Map<String, Object> getQueueStatistics(
            String deviceId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("Fetching queue statistics for device: {}", deviceId);

        try {
            // Get MQTT data
            String url = String.format("%s/api/mqtt-data/device/%s/range?startTime=%s&endTime=%s",
                    mqttApiBaseUrl, deviceId, startTime, endTime);

            List<Map<String, Object>> mqttData = restTemplate.getForObject(url, List.class);

            if (mqttData == null || mqttData.isEmpty()) {
                return Collections.emptyMap();
            }

            // Extract queue lengths
            List<Double> queueLengths = mqttData.stream()
                    .filter(data -> data.containsKey("queueLength"))
                    .map(data -> ((Number) data.get("queueLength")).doubleValue())
                    .collect(Collectors.toList());

            if (queueLengths.isEmpty()) {
                return Collections.emptyMap();
            }

            // Calculate statistics
            DoubleSummaryStatistics stats = queueLengths.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("averageQueueLength", stats.getAverage());
            statistics.put("maxQueueLength", stats.getMax());
            statistics.put("minQueueLength", stats.getMin());
            statistics.put("totalReadings", stats.getCount());

            // Calculate median
            List<Double> sorted = queueLengths.stream().sorted().collect(Collectors.toList());
            double median = sorted.size() % 2 == 0
                    ? (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2)) / 2.0
                    : sorted.get(sorted.size() / 2);
            statistics.put("medianQueueLength", median);

            // Calculate standard deviation
            double variance = queueLengths.stream()
                    .mapToDouble(v -> Math.pow(v - stats.getAverage(), 2))
                    .average()
                    .orElse(0.0);
            statistics.put("standardDeviation", Math.sqrt(variance));

            // Determine trend
            String trend = calculateTrendFromData(queueLengths);
            statistics.put("trend", trend);

            return statistics;

        } catch (Exception e) {
            log.error("Error fetching queue statistics for device {}: {}", deviceId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Find peak hours from hourly pattern
     */
    public List<Integer> findPeakHours(List<Map<String, Object>> hourlyPattern) {
        return hourlyPattern.stream()
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("averageQueueLength")).doubleValue(),
                        ((Number) a.get("averageQueueLength")).doubleValue()))
                .limit(3)
                .map(h -> (Integer) h.get("hour"))
                .collect(Collectors.toList());
    }

    /**
     * Find low hours from hourly pattern
     */
    public List<Integer> findLowHours(List<Map<String, Object>> hourlyPattern) {
        return hourlyPattern.stream()
                .sorted((a, b) -> Double.compare(
                        ((Number) a.get("averageQueueLength")).doubleValue(),
                        ((Number) b.get("averageQueueLength")).doubleValue()))
                .limit(3)
                .map(h -> (Integer) h.get("hour"))
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private List<QueueTrendDTO> groupByInterval(
            List<Map<String, Object>> mqttData,
            String interval,
            String deviceId) {

        // Parse interval
        int minutes = parseIntervalToMinutes(interval);

        // Group data by time buckets
        Map<LocalDateTime, List<Double>> buckets = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp") && data.containsKey("queueLength")) {
                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);

                // Round to interval
                LocalDateTime bucket = roundToInterval(timestamp, minutes);
                double queueLength = ((Number) data.get("queueLength")).doubleValue();

                buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(queueLength);
            }
        }

        // Calculate statistics for each bucket
        return buckets.entrySet().stream()
                .map(entry -> {
                    DoubleSummaryStatistics stats = entry.getValue().stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    return QueueTrendDTO.builder()
                            .timestamp(entry.getKey())
                            .deviceId(deviceId)
                            .averageQueueLength(stats.getAverage())
                            .maxQueueLength(stats.getMax())
                            .minQueueLength(stats.getMin())
                            .dataPoints((int) stats.getCount())
                            .interval(interval)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<AverageQueueDTO> groupAndAverageByPeriod(
            List<Map<String, Object>> mqttData,
            String groupBy,
            String location,
            String segment,
            int deviceCount) {

        // Parse group-by period
        ChronoUnit unit = parseGroupByToChronoUnit(groupBy);

        // Group data by time period
        Map<LocalDateTime, List<Double>> periods = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp") && data.containsKey("queueLength")) {
                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);

                // Truncate to period
                LocalDateTime period = timestamp.truncatedTo(unit);
                double queueLength = ((Number) data.get("queueLength")).doubleValue();

                periods.computeIfAbsent(period, k -> new ArrayList<>()).add(queueLength);
            }
        }

        // Calculate averages for each period
        return periods.entrySet().stream()
                .map(entry -> {
                    DoubleSummaryStatistics stats = entry.getValue().stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    return AverageQueueDTO.builder()
                            .timestamp(entry.getKey())
                            .location(location)
                            .segment(segment)
                            .averageQueueLength(stats.getAverage())
                            .maxQueueLength(stats.getMax())
                            .minQueueLength(stats.getMin())
                            .deviceCount(deviceCount)
                            .totalReadings((int) stats.getCount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private int parseIntervalToMinutes(String interval) {
        switch (interval.toLowerCase()) {
            case "15min": return 15;
            case "30min": return 30;
            case "1hour": return 60;
            case "4hour": return 240;
            case "1day": return 1440;
            default: return 60; // Default to 1 hour
        }
    }

    private ChronoUnit parseGroupByToChronoUnit(String groupBy) {
        switch (groupBy.toLowerCase()) {
            case "hour": return ChronoUnit.HOURS;
            case "day": return ChronoUnit.DAYS;
            case "week": return ChronoUnit.WEEKS;
            case "month": return ChronoUnit.MONTHS;
            default: return ChronoUnit.HOURS;
        }
    }

    private LocalDateTime roundToInterval(LocalDateTime timestamp, int intervalMinutes) {
        int minute = timestamp.getMinute();
        int roundedMinute = (minute / intervalMinutes) * intervalMinutes;
        return timestamp.truncatedTo(ChronoUnit.HOURS).plusMinutes(roundedMinute);
    }

    private String calculateTrendFromData(List<Double> data) {
        if (data.size() < 10) {
            return "insufficient_data";
        }

        int splitPoint = data.size() / 2;
        double firstHalf = data.subList(0, splitPoint).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double secondHalf = data.subList(splitPoint, data.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double change = ((secondHalf - firstHalf) / firstHalf) * 100;

        if (Math.abs(change) < 5) {
            return "stable";
        } else if (change > 0) {
            return "increasing";
        } else {
            return "decreasing";
        }
    }
}