package com.bmsedge.device.service;

import com.bmsedge.device.client.MqttAggregationClient;
import com.bmsedge.device.dto.CounterTrendDto;
import com.bmsedge.device.dto.CounterTrendResponse;
import com.bmsedge.device.dto.MqttAggregationDTO;
import com.bmsedge.device.model.Counter;
import com.bmsedge.device.model.Device;
import com.bmsedge.device.repository.CounterRepository;
import com.bmsedge.device.repository.CounterSessionAnalyticsRepository;
import com.bmsedge.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Counter-Specific Queue Analytics Service
 * Provides counter performance analysis with cumulative queue aggregation
 *
 * Uses inCount from MQTT data as the primary queue/people counter metric
 * MQTT API Endpoint: GET /api/mqtt-data/device/{deviceId} - Returns all data for a device
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CounterAnalyticsService {

    private final DeviceRepository deviceRepository;
    private final CounterRepository counterRepository;
    private final RestTemplate restTemplate;
    private final CounterSessionAnalyticsRepository repository;
    private final MqttAggregationClient mqttAggregationClient;

    @Value("${mqtt.api.base-url:http://localhost:8090}")
    private String mqttApiBaseUrl;

    /**
     * Get queue trends for a specific counter with CUMULATIVE aggregation
     * Returns aggregated queue data across all devices in the counter at each timestamp
     */
    public Map<String, Object> getCounterQueueTrends(
            String counterCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval) {

        log.info("Fetching counter queue trends: counter={}, start={}, end={}, interval={}",
                counterCode, startTime, endTime, interval);

        try {
            // Get counter
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            // Get all devices for this counter
            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                log.warn("No devices found for counter: {}", counterCode);
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            log.info("Found {} devices for counter {}", devices.size(), counterCode);

            // Collect MQTT data for all devices using /device/{deviceId} endpoint
            List<Map<String, Object>> allMqttData = new ArrayList<>();
            Map<String, List<Map<String, Object>>> deviceDataMap = new HashMap<>();

            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s",
                            mqttApiBaseUrl, device.getDeviceId());

                    log.debug("Fetching MQTT data from: {}", url);

                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                        Object dataObj = response.get("data");

                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                            if (!deviceData.isEmpty()) {
                                // Filter by time range
                                List<Map<String, Object>> filteredData = deviceData.stream()
                                        .filter(data -> {
                                            if (data.containsKey("timestamp")) {
                                                try {
                                                    LocalDateTime dataTime = LocalDateTime.parse(
                                                            data.get("timestamp").toString(),
                                                            DateTimeFormatter.ISO_DATE_TIME);
                                                    return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                            }
                                            return false;
                                        })
                                        .collect(Collectors.toList());

                                // Tag each data point with device info
                                filteredData.forEach(data -> {
                                    data.put("deviceId", device.getDeviceId());
                                    data.put("deviceName", device.getDeviceName());
                                });

                                allMqttData.addAll(filteredData);
                                deviceDataMap.put(device.getDeviceId(), filteredData);
                                log.debug("Fetched {} data points for device {} (after time filter)",
                                        filteredData.size(), device.getDeviceId());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            if (allMqttData.isEmpty()) {
                log.warn("No MQTT data found for counter: {}", counterCode);
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            log.info("Total MQTT data points collected: {}", allMqttData.size());

            // Calculate CUMULATIVE trends (sum across all devices at each timestamp)
            List<Map<String, Object>> cumulativeTrends = calculateCumulativeTrends(
                    allMqttData, interval, devices.size());

            // Calculate overall statistics
            Map<String, Object> statistics = calculateCounterStatistics(cumulativeTrends, allMqttData);

            // Create device breakdown
            List<Map<String, Object>> deviceBreakdown = createDeviceBreakdown(deviceDataMap, allMqttData);

            Map<String, Object> response = new HashMap<>();
            response.put("counterCode", counterCode);
            response.put("counterName", counter.getCounterName());
            response.put("counterType", counter.getCounterType());
            response.put("totalDeviceCount", devices.size());
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("interval", interval);
            response.put("dataPointCount", cumulativeTrends.size());

            // Main trends with cumulative (total) queue
            response.put("trends", cumulativeTrends);
            response.put("statistics", statistics);
            response.put("deviceBreakdown", deviceBreakdown);

            response.put("devices", devices.stream()
                    .map(d -> Map.of(
                            "deviceId", d.getDeviceId(),
                            "deviceName", d.getDeviceName()
                    ))
                    .collect(Collectors.toList()));

            log.info("Successfully calculated {} trend points for counter {}",
                    cumulativeTrends.size(), counterCode);

            return response;

        } catch (Exception e) {
            log.error("Error fetching counter queue trends: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching counter queue trends", e);
        }
    }

    /**
     * Compare performance across multiple counters
     */
    // MODIFICATION TO EXISTING METHOD in CounterAnalyticsService.java
// Find the compareCounterPerformance method and update it

    /**
     * Compare performance across multiple counters
     * UPDATED: Now includes congestionRate calculation
     */
    public Map<String, Object> compareCounterPerformance(
            String[] counterCodes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String filterType) {

        log.info("Comparing {} counters with filter: {}", counterCodes.length, filterType);

        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (String counterCode : counterCodes) {
            try {
                Counter counter = counterRepository.findByCounterCode(counterCode).orElse(null);
                if (counter == null) {
                    log.warn("Counter not found: {}", counterCode);
                    continue;
                }

                List<Device> devices = deviceRepository.findByCounterId(counter.getId());
                if (devices.isEmpty()) {
                    continue;
                }

                // Collect MQTT data for all devices
                List<Map<String, Object>> allData = new ArrayList<>();
                for (Device device : devices) {
                    try {
                        String url = String.format("%s/api/mqtt-data/device/%s",
                                mqttApiBaseUrl, device.getDeviceId());

                        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                        if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                            Object dataObj = response.get("data");

                            if (dataObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                                // Filter by time range
                                List<Map<String, Object>> filteredData = deviceData.stream()
                                        .filter(data -> {
                                            if (data.containsKey("timestamp")) {
                                                try {
                                                    LocalDateTime dataTime = LocalDateTime.parse(
                                                            data.get("timestamp").toString(),
                                                            DateTimeFormatter.ISO_DATE_TIME);
                                                    return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                            }
                                            return false;
                                        })
                                        .collect(Collectors.toList());

                                filteredData.forEach(d -> d.put("deviceId", device.getDeviceId()));
                                allData.addAll(filteredData);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                    }
                }

                if (!allData.isEmpty()) {
                    // Calculate cumulative stats
                    Map<String, Object> stats = calculateAggregatedStats(allData, devices.size());

                    // ========== NEW: Calculate Congestion Rate ==========
                    double averageTotalQueue = ((Number) stats.get("averageTotalQueue")).doubleValue();
                    double maxTotalQueue = ((Number) stats.get("maxTotalQueue")).doubleValue();
                    double congestionRate = calculateCongestionRate(averageTotalQueue, maxTotalQueue);
                    // ====================================================

                    double filterValue = switch (filterType.toLowerCase()) {
                        case "max" -> ((Number) stats.get("maxTotalQueue")).doubleValue();
                        case "min" -> ((Number) stats.get("minTotalQueue")).doubleValue();
                        default -> ((Number) stats.get("averageTotalQueue")).doubleValue();
                    };

                    Map<String, Object> comparison = new HashMap<>();
                    comparison.put("counterCode", counterCode);
                    comparison.put("counterName", counter.getCounterName());
                    comparison.put("counterType", counter.getCounterType());
                    comparison.put("deviceCount", devices.size());
                    comparison.put("averageTotalQueue", stats.get("averageTotalQueue"));
                    comparison.put("averageQueuePerDevice", stats.get("averageQueuePerDevice"));
                    comparison.put("maxTotalQueue", stats.get("maxTotalQueue"));
                    comparison.put("minTotalQueue", stats.get("minTotalQueue"));
                    comparison.put("filterValue", filterValue);
                    comparison.put("totalReadings", stats.get("totalReadings"));
                    comparison.put("efficiency", stats.get("efficiency"));

                    // ========== NEW: Add Congestion Rate to response ==========
                    comparison.put("congestionRate", congestionRate);
                    // ==========================================================

                    comparisons.add(comparison);
                }

            } catch (Exception e) {
                log.error("Error processing counter {}: {}", counterCode, e.getMessage());
            }
        }

        // Sort by filter value
        comparisons.sort((a, b) -> Double.compare(
                ((Number) a.get("filterValue")).doubleValue(),
                ((Number) b.get("filterValue")).doubleValue()
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("counterCodes", counterCodes);
        response.put("counterCount", counterCodes.length);
        response.put("filterType", filterType);
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("comparisons", comparisons);

        // Add insights
        if (!comparisons.isEmpty()) {
            Map<String, Object> best = comparisons.get(0);
            Map<String, Object> worst = comparisons.get(comparisons.size() - 1);

            Map<String, Object> insights = new HashMap<>();
            insights.put("bestPerforming", Map.of(
                    "counterCode", best.get("counterCode"),
                    "counterName", best.get("counterName"),
                    "averageQueue", best.get("averageTotalQueue"),
                    "efficiency", best.get("efficiency"),
                    "congestionRate", best.get("congestionRate")  // NEW
            ));
            insights.put("worstPerforming", Map.of(
                    "counterCode", worst.get("counterCode"),
                    "counterName", worst.get("counterName"),
                    "averageQueue", worst.get("averageTotalQueue"),
                    "efficiency", worst.get("efficiency"),
                    "congestionRate", worst.get("congestionRate")  // NEW
            ));

            response.put("insights", insights);
        }

        return response;
    }

    /**
     * Get counter performance analysis with hourly patterns
     */
    public Map<String, Object> getCounterPerformanceAnalysis(
            String counterCode,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("Analyzing counter performance: {}", counterCode);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            List<Device> devices = deviceRepository.findByCounterId(counter.getId());
            if (devices.isEmpty()) {
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            // Collect all MQTT data
            List<Map<String, Object>> allData = new ArrayList<>();
            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s",
                            mqttApiBaseUrl, device.getDeviceId());

                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                        Object dataObj = response.get("data");

                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                            // Filter by time range
                            List<Map<String, Object>> filteredData = deviceData.stream()
                                    .filter(data -> {
                                        if (data.containsKey("timestamp")) {
                                            try {
                                                LocalDateTime dataTime = LocalDateTime.parse(
                                                        data.get("timestamp").toString(),
                                                        DateTimeFormatter.ISO_DATE_TIME);
                                                return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                            } catch (Exception e) {
                                                return false;
                                            }
                                        }
                                        return false;
                                    })
                                    .collect(Collectors.toList());

                            filteredData.forEach(d -> d.put("deviceId", device.getDeviceId()));
                            allData.addAll(filteredData);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            if (allData.isEmpty()) {
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            // Calculate hourly pattern with cumulative data
            List<Map<String, Object>> hourlyPattern = calculateHourlyPattern(allData, devices.size());

            // Overall statistics
            Map<String, Object> statistics = calculateAggregatedStats(allData, devices.size());

            // Peak and low hours
            List<Integer> peakHours = findPeakHours(hourlyPattern);
            List<Integer> lowHours = findLowHours(hourlyPattern);

            Map<String, Object> response = new HashMap<>();
            response.put("counterCode", counterCode);
            response.put("counterName", counter.getCounterName());
            response.put("counterType", counter.getCounterType());
            response.put("deviceCount", devices.size());
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("hourlyPattern", hourlyPattern);
            response.put("statistics", statistics);
            response.put("peakHours", peakHours);
            response.put("lowHours", lowHours);

            return response;

        } catch (Exception e) {
            log.error("Error analyzing counter performance: {}", e.getMessage(), e);
            throw new RuntimeException("Error analyzing counter performance", e);
        }
    }

    /**
     * Get all active counters summary
     */
    public List<Map<String, Object>> getAllCountersSummary(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("Fetching summary for all counters");

        List<Counter> counters = counterRepository.findByActive(true);
        List<Map<String, Object>> summaries = new ArrayList<>();

        for (Counter counter : counters) {
            try {
                List<Device> devices = deviceRepository.findByCounterId(counter.getId());
                if (devices.isEmpty()) {
                    continue;
                }

                List<Map<String, Object>> allData = new ArrayList<>();
                for (Device device : devices) {
                    try {
                        String url = String.format("%s/api/mqtt-data/device/%s",
                                mqttApiBaseUrl, device.getDeviceId());

                        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                        if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                            Object dataObj = response.get("data");

                            if (dataObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                                // Filter by time range
                                List<Map<String, Object>> filteredData = deviceData.stream()
                                        .filter(data -> {
                                            if (data.containsKey("timestamp")) {
                                                try {
                                                    LocalDateTime dataTime = LocalDateTime.parse(
                                                            data.get("timestamp").toString(),
                                                            DateTimeFormatter.ISO_DATE_TIME);
                                                    return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                                } catch (Exception e) {
                                                    return false;
                                                }
                                            }
                                            return false;
                                        })
                                        .collect(Collectors.toList());

                                filteredData.forEach(d -> d.put("deviceId", device.getDeviceId()));
                                allData.addAll(filteredData);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                    }
                }

                if (!allData.isEmpty()) {
                    Map<String, Object> stats = calculateAggregatedStats(allData, devices.size());

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("counterCode", counter.getCounterCode());
                    summary.put("counterName", counter.getCounterName());
                    summary.put("counterType", counter.getCounterType());
                    summary.put("deviceCount", devices.size());
                    summary.put("averageTotalQueue", stats.get("averageTotalQueue"));
                    summary.put("averageQueuePerDevice", stats.get("averageQueuePerDevice"));
                    summary.put("maxTotalQueue", stats.get("maxTotalQueue"));
                    summary.put("minTotalQueue", stats.get("minTotalQueue"));
                    summary.put("totalReadings", stats.get("totalReadings"));
                    summary.put("efficiency", stats.get("efficiency"));

                    summaries.add(summary);
                }

            } catch (Exception e) {
                log.error("Error processing counter {}: {}", counter.getCounterCode(), e.getMessage());
            }
        }

        // Sort by average total queue
        summaries.sort((a, b) -> Double.compare(
                ((Number) a.get("averageTotalQueue")).doubleValue(),
                ((Number) b.get("averageTotalQueue")).doubleValue()
        ));

        return summaries;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract queue value from MQTT data
     * Priority: inCount (primary) > queueLength (fallback) > occupancy (legacy)
     */
    private Double extractQueueValue(Map<String, Object> data) {
        // Priority 1: inCount (the actual people/queue counter)
        if (data.containsKey("inCount")) {
            Object value = data.get("inCount");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }

        // Priority 2: queueLength (standard field name)
        if (data.containsKey("queueLength")) {
            Object value = data.get("queueLength");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }

        // Priority 3: occupancy (alternative field)
        if (data.containsKey("occupancy")) {
            Object value = data.get("occupancy");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }

        return null;
    }

    /**
     * Calculate cumulative trends by aggregating all devices at each timestamp interval
     * KEY METHOD: This sums queue lengths across all devices for each timestamp
     */
    private List<Map<String, Object>> calculateCumulativeTrends(
            List<Map<String, Object>> mqttData,
            String interval,
            int totalDeviceCount) {

        int minutes = parseIntervalToMinutes(interval);

        // Group by timestamp bucket AND device
        Map<LocalDateTime, Map<String, Double>> bucketDeviceMap = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp") && data.containsKey("deviceId")) {
                Double queueLength = extractQueueValue(data);

                if (queueLength == null) {
                    continue; // Skip if no queue field exists
                }

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);

                LocalDateTime bucket = roundToInterval(timestamp, minutes);
                String deviceId = (String) data.get("deviceId");

                bucketDeviceMap.computeIfAbsent(bucket, k -> new HashMap<>())
                        .put(deviceId, queueLength);
            }
        }

        // Calculate aggregates for each time bucket
        return bucketDeviceMap.entrySet().stream()
                .map(entry -> {
                    LocalDateTime timestamp = entry.getKey();
                    Map<String, Double> deviceQueues = entry.getValue();

                    // CUMULATIVE = Sum of all device queues at this timestamp
                    double totalQueue = deviceQueues.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    // Statistics across devices
                    DoubleSummaryStatistics stats = deviceQueues.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    Map<String, Object> trend = new HashMap<>();
                    trend.put("timestamp", timestamp);
                    trend.put("totalQueueLength", totalQueue);        // SUM of all devices
                    trend.put("averageQueueLength", stats.getAverage());  // Average per device
                    trend.put("maxQueueLength", stats.getMax());
                    trend.put("minQueueLength", stats.getMin());
                    trend.put("activeDeviceCount", deviceQueues.size());
                    trend.put("totalDeviceCount", totalDeviceCount);
                    trend.put("dataPointCount", deviceQueues.size());

                    return trend;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate aggregated statistics across all timestamps
     */
    private Map<String, Object> calculateCounterStatistics(
            List<Map<String, Object>> trends,
            List<Map<String, Object>> allMqttData) {

        Map<String, Object> statistics = new HashMap<>();

        if (!trends.isEmpty()) {
            // Statistics from cumulative trends
            DoubleSummaryStatistics totalQueueStats = trends.stream()
                    .mapToDouble(t -> ((Number) t.get("totalQueueLength")).doubleValue())
                    .summaryStatistics();

            statistics.put("averageTotalQueue", totalQueueStats.getAverage());
            statistics.put("maxTotalQueue", totalQueueStats.getMax());
            statistics.put("minTotalQueue", totalQueueStats.getMin());

            // Per-device statistics
            long count = allMqttData.stream()
                    .map(this::extractQueueValue)
                    .filter(Objects::nonNull)
                    .count();

            double average = allMqttData.stream()
                    .map(this::extractQueueValue)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            statistics.put("averageQueuePerDevice", average);
            statistics.put("totalReadings", count);

            // Efficiency
            double avg = totalQueueStats.getAverage();
            double max = totalQueueStats.getMax();
            statistics.put("efficiency", calculateEfficiency(avg, max));

            // Trend
            List<Double> totalQueues = trends.stream()
                    .map(t -> ((Number) t.get("totalQueueLength")).doubleValue())
                    .collect(Collectors.toList());
            statistics.put("trend", calculateTrend(totalQueues));
        }

        return statistics;
    }

    /**
     * Calculate aggregated stats from raw MQTT data
     */
    private Map<String, Object> calculateAggregatedStats(
            List<Map<String, Object>> allData,
            int deviceCount) {

        Map<String, Object> stats = new HashMap<>();

        // Group by timestamp to calculate cumulative queues
        Map<LocalDateTime, List<Double>> timeQueues = new HashMap<>();

        for (Map<String, Object> data : allData) {
            if (data.containsKey("timestamp")) {
                Double queueValue = extractQueueValue(data);

                if (queueValue == null) {
                    continue;
                }

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);
                LocalDateTime rounded = timestamp.truncatedTo(ChronoUnit.MINUTES);

                timeQueues.computeIfAbsent(rounded, k -> new ArrayList<>()).add(queueValue);
            }
        }

        // Calculate cumulative sums per timestamp
        List<Double> cumulativeSums = timeQueues.values().stream()
                .map(list -> list.stream().mapToDouble(Double::doubleValue).sum())
                .collect(Collectors.toList());

        if (!cumulativeSums.isEmpty()) {
            DoubleSummaryStatistics cumulativeStats = cumulativeSums.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            stats.put("averageTotalQueue", cumulativeStats.getAverage());
            stats.put("maxTotalQueue", cumulativeStats.getMax());
            stats.put("minTotalQueue", cumulativeStats.getMin());
        }

        // Per-device stats
        long count = allData.stream()
                .map(this::extractQueueValue)
                .filter(Objects::nonNull)
                .count();

        double average = allData.stream()
                .map(this::extractQueueValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        stats.put("averageQueuePerDevice", average);
        stats.put("totalReadings", count);

        double avg = (Double) stats.getOrDefault("averageTotalQueue", 0.0);
        double max = (Double) stats.getOrDefault("maxTotalQueue", 1.0);
        stats.put("efficiency", calculateEfficiency(avg, max));

        return stats;
    }

    /**
     * Calculate hourly pattern with cumulative data
     */
    private List<Map<String, Object>> calculateHourlyPattern(
            List<Map<String, Object>> allData,
            int deviceCount) {

        Map<Integer, Map<LocalDateTime, Map<String, Double>>> hourlyDeviceData = new HashMap<>();

        for (Map<String, Object> data : allData) {
            if (data.containsKey("timestamp") && data.containsKey("deviceId")) {
                Double queueValue = extractQueueValue(data);

                if (queueValue == null) {
                    continue;
                }

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);
                int hour = timestamp.getHour();
                String deviceId = (String) data.get("deviceId");

                hourlyDeviceData
                        .computeIfAbsent(hour, k -> new HashMap<>())
                        .computeIfAbsent(timestamp.truncatedTo(ChronoUnit.MINUTES), k -> new HashMap<>())
                        .put(deviceId, queueValue);
            }
        }

        List<Map<String, Object>> hourlyPattern = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            Map<LocalDateTime, Map<String, Double>> hourData = hourlyDeviceData.get(hour);

            if (hourData != null && !hourData.isEmpty()) {
                // Calculate cumulative sums for this hour
                List<Double> cumulativeSums = hourData.values().stream()
                        .map(deviceMap -> deviceMap.values().stream()
                                .mapToDouble(Double::doubleValue)
                                .sum())
                        .collect(Collectors.toList());

                DoubleSummaryStatistics stats = cumulativeSums.stream()
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

                Map<String, Object> pattern = new HashMap<>();
                pattern.put("hour", hour);
                pattern.put("averageTotalQueue", stats.getAverage());
                pattern.put("maxTotalQueue", stats.getMax());
                pattern.put("minTotalQueue", stats.getMin());
                pattern.put("dataPointCount", stats.getCount());

                hourlyPattern.add(pattern);
            }
        }

        return hourlyPattern;
    }

    /**
     * Create device breakdown showing contribution
     */
    private List<Map<String, Object>> createDeviceBreakdown(
            Map<String, List<Map<String, Object>>> deviceDataMap,
            List<Map<String, Object>> allMqttData) {

        List<Map<String, Object>> breakdown = new ArrayList<>();

        double totalSum = allMqttData.stream()
                .map(this::extractQueueValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceDataMap.entrySet()) {
            String deviceId = entry.getKey();
            List<Map<String, Object>> deviceData = entry.getValue();

            if (!deviceData.isEmpty()) {
                DoubleSummaryStatistics stats = deviceData.stream()
                        .map(this::extractQueueValue)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

                double deviceSum = stats.getSum();
                double contribution = totalSum > 0 ? (deviceSum / totalSum) * 100 : 0;

                Map<String, Object> summary = new HashMap<>();
                summary.put("deviceId", deviceId);
                summary.put("deviceName", deviceData.get(0).get("deviceName"));
                summary.put("averageQueueLength", stats.getAverage());
                summary.put("maxQueueLength", stats.getMax());
                summary.put("minQueueLength", stats.getMin());
                summary.put("totalReadings", stats.getCount());
                summary.put("contributionPercentage", contribution);
                summary.put("status", "active");

                breakdown.add(summary);
            }
        }

        breakdown.sort((a, b) -> Double.compare(
                ((Number) b.get("contributionPercentage")).doubleValue(),
                ((Number) a.get("contributionPercentage")).doubleValue()
        ));

        return breakdown;
    }


    /**
     * Get Historical Queue Trends with Time-Based Aggregation
     * Supports hour-wise, day-wise, week-wise, and month-wise aggregation
     */
    public Map<String, Object> getHistoricalQueueTrends(
            String counterCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String granularity) {

        log.info("Fetching historical queue trends: counter={}, granularity={}, start={}, end={}",
                counterCode, granularity, startTime, endTime);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                log.warn("No devices found for counter: {}", counterCode);
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            // Collect MQTT data for all devices
            List<Map<String, Object>> allMqttData = new ArrayList<>();
            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s",
                            mqttApiBaseUrl, device.getDeviceId());

                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                        Object dataObj = response.get("data");

                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                            List<Map<String, Object>> filteredData = deviceData.stream()
                                    .filter(data -> {
                                        if (data.containsKey("timestamp")) {
                                            try {
                                                LocalDateTime dataTime = LocalDateTime.parse(
                                                        data.get("timestamp").toString(),
                                                        DateTimeFormatter.ISO_DATE_TIME);
                                                return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                            } catch (Exception e) {
                                                return false;
                                            }
                                        }
                                        return false;
                                    })
                                    .collect(Collectors.toList());

                            filteredData.forEach(data -> data.put("deviceId", device.getDeviceId()));
                            allMqttData.addAll(filteredData);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            if (allMqttData.isEmpty()) {
                log.warn("No MQTT data found for counter: {}", counterCode);
                return createEmptyResponse(counterCode, counter.getCounterName());
            }

            // Calculate trends based on granularity
            List<Map<String, Object>> trends = calculateHistoricalTrends(
                    allMqttData, granularity, devices.size());

            // Calculate statistics
            Map<String, Object> statistics = calculateCounterStatistics(trends, allMqttData);

            Map<String, Object> result = new HashMap<>();
            result.put("counterCode", counterCode);
            result.put("counterName", counter.getCounterName());
            result.put("counterType", counter.getCounterType());
            result.put("deviceCount", devices.size());
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("granularity", granularity);
            result.put("dataPointCount", trends.size());
            result.put("trends", trends);
            result.put("statistics", statistics);

            log.info("Successfully calculated {} historical trend points", trends.size());
            return result;

        } catch (Exception e) {
            log.error("Error fetching historical queue trends: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching historical queue trends", e);
        }
    }

    /**
     * Calculate historical trends with time-based aggregation
     */
    private List<Map<String, Object>> calculateHistoricalTrends(
            List<Map<String, Object>> mqttData,
            String granularity,
            int totalDeviceCount) {

        // Group by time bucket based on granularity
        Map<String, Map<String, Double>> bucketDeviceMap = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp") && data.containsKey("deviceId")) {
                Double queueLength = extractQueueValue(data);

                if (queueLength == null) {
                    continue;
                }

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);

                String bucket = getBucketKey(timestamp, granularity);
                String deviceId = (String) data.get("deviceId");

                bucketDeviceMap.computeIfAbsent(bucket, k -> new HashMap<>())
                        .put(deviceId, queueLength);
            }
        }

        // Calculate aggregates for each time bucket
        return bucketDeviceMap.entrySet().stream()
                .map(entry -> {
                    String bucketKey = entry.getKey();
                    Map<String, Double> deviceQueues = entry.getValue();

                    double totalQueue = deviceQueues.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    DoubleSummaryStatistics stats = deviceQueues.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    Map<String, Object> trend = new HashMap<>();
                    trend.put("timePeriod", bucketKey);
                    trend.put("average_queue_length", roundToOneDecimal(stats.getAverage()));
                    trend.put("total_queue_length", roundToOneDecimal(totalQueue));
                    trend.put("peak_queue", roundToOneDecimal(stats.getMax()));
                    trend.put("min_queue", roundToOneDecimal(stats.getMin()));
                    trend.put("active_device_count", deviceQueues.size());
                    trend.put("total_device_count", totalDeviceCount);

                    return trend;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get bucket key based on granularity
     */
    private String getBucketKey(LocalDateTime timestamp, String granularity) {
        return switch (granularity.toLowerCase()) {
            case "hour" -> String.format("%s %02d:00",
                    timestamp.toLocalDate(), timestamp.getHour());
            case "day" -> timestamp.toLocalDate().toString();
            case "week" -> {
                int weekOfYear = timestamp.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int year = timestamp.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
                yield String.format("%d-W%02d", year, weekOfYear);
            }
            case "month" -> String.format("%d-%02d",
                    timestamp.getYear(), timestamp.getMonthValue());
            default -> timestamp.toString();
        };
    }

    /**
     * Calculate Congestion Rate for comparison
     * Congestion Rate = (Average Queue Length / Maximum Queue Threshold) * 100
     * Using max observed queue as threshold
     */
    private double calculateCongestionRate(double averageQueue, double maxQueue) {
        if (maxQueue == 0) return 0.0;
        return roundToOneDecimal((averageQueue / maxQueue) * 100);
    }

    /**
     * Round value to 1 decimal place
     */
    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
    public List<CounterTrendResponse> getTrends(
            String counterCode,
            String periodType,
            LocalDateTime from,
            LocalDateTime to
    ) {

        // 🔥 SINGLE MQTT CALL
        List<MqttAggregationDTO> mqttData =
                mqttAggregationClient.fetchHourlyAggregation(from, to);

        List<MqttAggregationDTO> counterData = mqttData.stream()
                .filter(d -> d.getCounterName().equalsIgnoreCase(counterCode))
                .toList();

        return switch (periodType.toLowerCase()) {
            case "daily" -> aggregateDaily(counterData);
            case "weekly" -> aggregateWeekly(counterData);
            case "monthly" -> aggregateMonthly(counterData);
            default -> throw new IllegalArgumentException(
                    "periodType must be daily / weekly / monthly"
            );
        };
    }

    private List<CounterTrendResponse> aggregateDaily(
            List<MqttAggregationDTO> data
    ) {
        return data.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getPeriodStart().toLocalDate(),
                        Collectors.summingLong(MqttAggregationDTO::getTotalCount)
                ))
                .entrySet()
                .stream()
                .map(e -> new CounterTrendResponse(
                        e.getKey().atStartOfDay(),
                        e.getValue()
                ))
                .toList();
    }

    private List<CounterTrendResponse> aggregateWeekly(
            List<MqttAggregationDTO> data
    ) {
        return data.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getPeriodStart()
                                .toLocalDate()
                                .with(DayOfWeek.MONDAY),
                        Collectors.summingLong(MqttAggregationDTO::getTotalCount)
                ))
                .entrySet()
                .stream()
                .map(e -> new CounterTrendResponse(
                        e.getKey().atStartOfDay(),
                        e.getValue()
                ))
                .toList();
    }

    private List<CounterTrendResponse> aggregateMonthly(
            List<MqttAggregationDTO> data
    ) {
        return data.stream()
                .collect(Collectors.groupingBy(
                        d -> YearMonth.from(d.getPeriodStart()),
                        Collectors.summingLong(MqttAggregationDTO::getTotalCount)
                ))
                .entrySet()
                .stream()
                .map(e -> new CounterTrendResponse(
                        e.getKey().atDay(1).atStartOfDay(),
                        e.getValue()
                ))
                .toList();
    }







    /**
     * Get KPI data for current day only
     */
    public Map<String, Object> getCurrentDayKPIs(String counterCode) {
        log.info("Fetching current day KPIs for counter: {}", counterCode);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                return createEmptyKPIResponse(counterCode);
            }

            // Set time range to current day only (00:00 to now)
            LocalDateTime startTime = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endTime = LocalDateTime.now();

            // Collect MQTT data for current day
            List<Map<String, Object>> allData = new ArrayList<>();
            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s",
                            mqttApiBaseUrl, device.getDeviceId());

                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                        Object dataObj = response.get("data");

                        if (dataObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                            List<Map<String, Object>> filteredData = deviceData.stream()
                                    .filter(data -> {
                                        if (data.containsKey("timestamp")) {
                                            try {
                                                LocalDateTime dataTime = LocalDateTime.parse(
                                                        data.get("timestamp").toString(),
                                                        DateTimeFormatter.ISO_DATE_TIME);
                                                return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                            } catch (Exception e) {
                                                return false;
                                            }
                                        }
                                        return false;
                                    })
                                    .collect(Collectors.toList());

                            filteredData.forEach(d -> d.put("deviceId", device.getDeviceId()));
                            allData.addAll(filteredData);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                }
            }

            if (allData.isEmpty()) {
                return createEmptyKPIResponse(counterCode);
            }

            // Calculate KPI statistics
            Map<String, Object> stats = calculateAggregatedStats(allData, devices.size());

            Map<String, Object> kpis = new HashMap<>();
            kpis.put("counterCode", counterCode);
            kpis.put("counterName", counter.getCounterName());
            kpis.put("date", LocalDateTime.now().toLocalDate());
            kpis.put("average_queue_length", roundToOneDecimal((Double) stats.get("averageTotalQueue")));
            kpis.put("peak_queue", roundToOneDecimal((Double) stats.get("maxTotalQueue")));
            kpis.put("efficiency", roundToOneDecimal((Double) stats.get("efficiency")));
            kpis.put("total_readings", stats.get("totalReadings"));

            return kpis;

        } catch (Exception e) {
            log.error("Error calculating current day KPIs: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating current day KPIs", e);
        }
    }

    /**
     * Create empty KPI response
     */
    private Map<String, Object> createEmptyKPIResponse(String counterCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("counterCode", counterCode);
        response.put("date", LocalDateTime.now().toLocalDate());
        response.put("average_queue_length", 0.0);
        response.put("peak_queue", 0.0);
        response.put("efficiency", 0.0);
        response.put("total_readings", 0);
        return response;
    }

    /**
     * Get footfall summary for multiple counters
     */
    public Map<String, Object> getMultiCounterFootfallSummary(String[] counterCodes) {
        log.info("Fetching footfall summary for {} counters", counterCodes.length);

        try {
            List<Map<String, Object>> counterFootfalls = new ArrayList<>();

            for (String counterCode : counterCodes) {
                try {
                    Map<String, Object> footfall = getCounterFootfallSummary(counterCode.trim());
                    counterFootfalls.add(footfall);
                } catch (Exception e) {
                    log.error("Error fetching footfall for counter {}: {}", counterCode, e.getMessage());
                }
            }

            if (counterFootfalls.isEmpty()) {
                return createEmptyFootfallSummary("MULTIPLE");
            }

            // Aggregate footfall across all counters
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("scope", "MULTIPLE_COUNTERS");
            aggregated.put("counterCodes", counterCodes);
            aggregated.put("counterCount", counterFootfalls.size());
            aggregated.put("generatedAt", now);

            // Sum up all footfall data
            Map<String, Object> todayAgg = aggregateFootfallPeriod(counterFootfalls, "today");
            Map<String, Object> yesterdayAgg = aggregateFootfallPeriod(counterFootfalls, "yesterday");
            Map<String, Object> lastWeekAgg = aggregateFootfallPeriod(counterFootfalls, "lastWeekSameDay");
            Map<String, Object> lastMonthAgg = aggregateFootfallPeriod(counterFootfalls, "lastMonthSameDay");
            Map<String, Object> lastYearAgg = aggregateFootfallPeriod(counterFootfalls, "lastYearSameDay");

            aggregated.put("today", todayAgg);
            aggregated.put("yesterday", yesterdayAgg);
            aggregated.put("lastWeekSameDay", lastWeekAgg);
            aggregated.put("lastMonthSameDay", lastMonthAgg);
            aggregated.put("lastYearSameDay", lastYearAgg);

            return aggregated;

        } catch (Exception e) {
            log.error("Error calculating multi-counter footfall: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating multi-counter footfall", e);
        }
    }

    /**
     * Aggregate footfall data for a specific period across multiple counters
     */
    private Map<String, Object> aggregateFootfallPeriod(
            List<Map<String, Object>> counterFootfalls,
            String periodKey) {

        int totalCount = 0;
        String date = null;

        for (Map<String, Object> footfall : counterFootfalls) {
            if (footfall.containsKey(periodKey)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> period = (Map<String, Object>) footfall.get(periodKey);

                if (period.containsKey("count")) {
                    totalCount += ((Number) period.get("count")).intValue();
                }

                if (date == null && period.containsKey("date")) {
                    date = (String) period.get("date");
                }
            }
        }

        Map<String, Object> aggregated = new HashMap<>();
        aggregated.put("date", date);
        aggregated.put("count", totalCount);

        // Calculate percentage if not today
        if (!periodKey.equals("today") && counterFootfalls.size() > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Object> todayData = (Map<String, Object>) counterFootfalls.get(0).get("today");
            int todayCount = ((Number) todayData.get("count")).intValue() * counterFootfalls.size();

            if (totalCount > 0) {
                double percentage = ((double)(todayCount - totalCount) / totalCount) * 100;
                aggregated.put("percentage", percentage);
            } else {
                aggregated.put("percentage", 0.0);
            }
        } else {
            aggregated.put("percentage", null);
        }

        return aggregated;
    }



    /**
     * Add these methods to CounterAnalyticsService.java
     */

    /**
     * Get footfall summary for a specific counter with historical comparisons
     */
    public Map<String, Object> getCounterFootfallSummary(String counterCode) {
        log.info("Fetching footfall summary for counter: {}", counterCode);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                log.warn("No devices found for counter: {}", counterCode);
                return createEmptyFootfallSummary(counterCode);
            }

            // Calculate footfall for different time periods
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> today = calculateFootfallForPeriod(
                    devices, now.toLocalDate().atStartOfDay(), now);

            Map<String, Object> yesterday = calculateFootfallForPeriod(
                    devices,
                    now.minusDays(1).toLocalDate().atStartOfDay(),
                    now.minusDays(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastWeekSameDay = calculateFootfallForPeriod(
                    devices,
                    now.minusWeeks(1).toLocalDate().atStartOfDay(),
                    now.minusWeeks(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastMonthSameDay = calculateFootfallForPeriod(
                    devices,
                    now.minusMonths(1).toLocalDate().atStartOfDay(),
                    now.minusMonths(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastYearSameDay = calculateFootfallForPeriod(
                    devices,
                    now.minusYears(1).toLocalDate().atStartOfDay(),
                    now.minusYears(1).toLocalDate().atTime(23, 59, 59));

            // Calculate percentage changes
            double todayCount = ((Number) today.get("totalFootfall")).doubleValue();
            double yesterdayCount = ((Number) yesterday.get("totalFootfall")).doubleValue();
            double lastWeekCount = ((Number) lastWeekSameDay.get("totalFootfall")).doubleValue();
            double lastMonthCount = ((Number) lastMonthSameDay.get("totalFootfall")).doubleValue();
            double lastYearCount = ((Number) lastYearSameDay.get("totalFootfall")).doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("counterCode", counterCode);
            response.put("counterName", counter.getCounterName());
            response.put("generatedAt", now);

            // Today
            Map<String, Object> todayData = new HashMap<>();
            todayData.put("date", now.toLocalDate());
            todayData.put("count", Math.round(todayCount));
            todayData.put("percentage", null);
            response.put("today", todayData);

            // Yesterday
            Map<String, Object> yesterdayData = new HashMap<>();
            yesterdayData.put("date", now.minusDays(1).toLocalDate());
            yesterdayData.put("count", Math.round(yesterdayCount));
            yesterdayData.put("percentage", calculatePercentageChange(yesterdayCount, todayCount));
            response.put("yesterday", yesterdayData);

            // Last Week Same Day
            Map<String, Object> lastWeekData = new HashMap<>();
            lastWeekData.put("date", now.minusWeeks(1).toLocalDate());
            lastWeekData.put("count", Math.round(lastWeekCount));
            lastWeekData.put("percentage", calculatePercentageChange(lastWeekCount, todayCount));
            response.put("lastWeekSameDay", lastWeekData);

            // Last Month Same Day
            Map<String, Object> lastMonthData = new HashMap<>();
            lastMonthData.put("date", now.minusMonths(1).toLocalDate());
            lastMonthData.put("count", Math.round(lastMonthCount));
            lastMonthData.put("percentage", calculatePercentageChange(lastMonthCount, todayCount));
            response.put("lastMonthSameDay", lastMonthData);

            // Last Year Same Day
            Map<String, Object> lastYearData = new HashMap<>();
            lastYearData.put("date", now.minusYears(1).toLocalDate());
            lastYearData.put("count", Math.round(lastYearCount));
            lastYearData.put("percentage", calculatePercentageChange(lastYearCount, todayCount));
            response.put("lastYearSameDay", lastYearData);

            log.info("Successfully calculated footfall summary for counter: {}", counterCode);
            return response;

        } catch (Exception e) {
            log.error("Error calculating footfall summary: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating footfall summary", e);
        }
    }

    /**
     * Get aggregated footfall summary across all active counters
     */
    public Map<String, Object> getAllCountersFootfallSummary() {
        log.info("Fetching aggregated footfall summary for all counters");

        try {
            List<Counter> counters = counterRepository.findByActive(true);

            if (counters.isEmpty()) {
                log.warn("No active counters found");
                return createEmptyFootfallSummary("ALL");
            }

            // Collect all devices from all counters
            List<Device> allDevices = new ArrayList<>();
            for (Counter counter : counters) {
                List<Device> devices = deviceRepository.findByCounterId(counter.getId());
                allDevices.addAll(devices);
            }

            if (allDevices.isEmpty()) {
                log.warn("No devices found for any counter");
                return createEmptyFootfallSummary("ALL");
            }

            // Calculate aggregated footfall for different time periods
            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> today = calculateFootfallForPeriod(
                    allDevices, now.toLocalDate().atStartOfDay(), now);

            Map<String, Object> yesterday = calculateFootfallForPeriod(
                    allDevices,
                    now.minusDays(1).toLocalDate().atStartOfDay(),
                    now.minusDays(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastWeekSameDay = calculateFootfallForPeriod(
                    allDevices,
                    now.minusWeeks(1).toLocalDate().atStartOfDay(),
                    now.minusWeeks(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastMonthSameDay = calculateFootfallForPeriod(
                    allDevices,
                    now.minusMonths(1).toLocalDate().atStartOfDay(),
                    now.minusMonths(1).toLocalDate().atTime(23, 59, 59));

            Map<String, Object> lastYearSameDay = calculateFootfallForPeriod(
                    allDevices,
                    now.minusYears(1).toLocalDate().atStartOfDay(),
                    now.minusYears(1).toLocalDate().atTime(23, 59, 59));

            // Calculate percentage changes
            double todayCount = ((Number) today.get("totalFootfall")).doubleValue();
            double yesterdayCount = ((Number) yesterday.get("totalFootfall")).doubleValue();
            double lastWeekCount = ((Number) lastWeekSameDay.get("totalFootfall")).doubleValue();
            double lastMonthCount = ((Number) lastMonthSameDay.get("totalFootfall")).doubleValue();
            double lastYearCount = ((Number) lastYearSameDay.get("totalFootfall")).doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("scope", "ALL_COUNTERS");
            response.put("counterCount", counters.size());
            response.put("totalDevices", allDevices.size());
            response.put("generatedAt", now);

            // Today
            Map<String, Object> todayData = new HashMap<>();
            todayData.put("date", now.toLocalDate());
            todayData.put("count", Math.round(todayCount));
            todayData.put("percentage", null);
            response.put("today", todayData);

            // Yesterday
            Map<String, Object> yesterdayData = new HashMap<>();
            yesterdayData.put("date", now.minusDays(1).toLocalDate());
            yesterdayData.put("count", Math.round(yesterdayCount));
            yesterdayData.put("percentage", calculatePercentageChange(yesterdayCount, todayCount));
            response.put("yesterday", yesterdayData);

            // Last Week Same Day
            Map<String, Object> lastWeekData = new HashMap<>();
            lastWeekData.put("date", now.minusWeeks(1).toLocalDate());
            lastWeekData.put("count", Math.round(lastWeekCount));
            lastWeekData.put("percentage", calculatePercentageChange(lastWeekCount, todayCount));
            response.put("lastWeekSameDay", lastWeekData);

            // Last Month Same Day
            Map<String, Object> lastMonthData = new HashMap<>();
            lastMonthData.put("date", now.minusMonths(1).toLocalDate());
            lastMonthData.put("count", Math.round(lastMonthCount));
            lastMonthData.put("percentage", calculatePercentageChange(lastMonthCount, todayCount));
            response.put("lastMonthSameDay", lastMonthData);

            // Last Year Same Day
            Map<String, Object> lastYearData = new HashMap<>();
            lastYearData.put("date", now.minusYears(1).toLocalDate());
            lastYearData.put("count", Math.round(lastYearCount));
            lastYearData.put("percentage", calculatePercentageChange(lastYearCount, todayCount));
            response.put("lastYearSameDay", lastYearData);

            log.info("Successfully calculated aggregated footfall summary for {} counters",
                    counters.size());
            return response;

        } catch (Exception e) {
            log.error("Error calculating aggregated footfall summary: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating aggregated footfall summary", e);
        }
    }

    /**
     * Calculate footfall (cumulative inCount) for a specific time period
     */
    private Map<String, Object> calculateFootfallForPeriod(
            List<Device> devices,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.debug("Calculating footfall from {} to {}", startTime, endTime);

        double totalFootfall = 0.0;
        int devicesWithData = 0;
        Map<String, Double> deviceFootfalls = new HashMap<>();

        for (Device device : devices) {
            try {
                String url = String.format("%s/api/mqtt-data/device/%s",
                        mqttApiBaseUrl, device.getDeviceId());

                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response != null && "success".equals(response.get("status")) && response.containsKey("data")) {
                    Object dataObj = response.get("data");

                    if (dataObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> deviceData = (List<Map<String, Object>>) dataObj;

                        // Filter by time range and sum inCount values
                        double deviceTotal = deviceData.stream()
                                .filter(data -> {
                                    if (data.containsKey("timestamp")) {
                                        try {
                                            LocalDateTime dataTime = LocalDateTime.parse(
                                                    data.get("timestamp").toString(),
                                                    DateTimeFormatter.ISO_DATE_TIME);
                                            return !dataTime.isBefore(startTime) && !dataTime.isAfter(endTime);
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }
                                    return false;
                                })
                                .mapToDouble(data -> {
                                    Double value = extractQueueValue(data);
                                    return value != null ? value : 0.0;
                                })
                                .sum();

                        if (deviceTotal > 0) {
                            totalFootfall += deviceTotal;
                            deviceFootfalls.put(device.getDeviceId(), deviceTotal);
                            devicesWithData++;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalFootfall", totalFootfall);
        result.put("devicesWithData", devicesWithData);
        result.put("totalDevices", devices.size());
        result.put("deviceBreakdown", deviceFootfalls);
        result.put("startTime", startTime);
        result.put("endTime", endTime);

        return result;
    }

    /**
     * Calculate percentage change between two values
     * Returns positive for increase, negative for decrease
     */
    private Double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }

        double change = ((newValue - oldValue) / oldValue) * 100;
        return Math.round(change * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Create empty footfall summary response
     */
    private Map<String, Object> createEmptyFootfallSummary(String counterCode) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> response = new HashMap<>();
        response.put("counterCode", counterCode);
        response.put("generatedAt", now);

        Map<String, Object> emptyData = new HashMap<>();
        emptyData.put("count", 0);
        emptyData.put("percentage", null);

        response.put("today", Map.of("date", now.toLocalDate(), "count", 0, "percentage", null));
        response.put("yesterday", Map.of("date", now.minusDays(1).toLocalDate(), "count", 0, "percentage", 0.0));
        response.put("lastWeekSameDay", Map.of("date", now.minusWeeks(1).toLocalDate(), "count", 0, "percentage", 0.0));
        response.put("lastMonthSameDay", Map.of("date", now.minusMonths(1).toLocalDate(), "count", 0, "percentage", 0.0));
        response.put("lastYearSameDay", Map.of("date", now.minusYears(1).toLocalDate(), "count", 0, "percentage", 0.0));

        return response;
    }

    private double calculateEfficiency(double average, double max) {
        if (max == 0) return 100.0;
        return Math.max(0, (1 - (average / max)) * 100);
    }

    private String calculateTrend(List<Double> data) {
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

        double change = firstHalf != 0 ? ((secondHalf - firstHalf) / firstHalf) * 100 : 0;

        if (Math.abs(change) < 5) {
            return "stable";
        } else if (change > 0) {
            return "increasing";
        } else {
            return "decreasing";
        }
    }

    private List<Integer> findPeakHours(List<Map<String, Object>> hourlyPattern) {
        return hourlyPattern.stream()
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("averageTotalQueue")).doubleValue(),
                        ((Number) a.get("averageTotalQueue")).doubleValue()))
                .limit(3)
                .map(h -> (Integer) h.get("hour"))
                .collect(Collectors.toList());
    }

    private List<Integer> findLowHours(List<Map<String, Object>> hourlyPattern) {
        return hourlyPattern.stream()
                .sorted((a, b) -> Double.compare(
                        ((Number) a.get("averageTotalQueue")).doubleValue(),
                        ((Number) b.get("averageTotalQueue")).doubleValue()))
                .limit(3)
                .map(h -> (Integer) h.get("hour"))
                .collect(Collectors.toList());
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
        response.put("deviceBreakdown", Collections.emptyList());
        return response;
    }
}