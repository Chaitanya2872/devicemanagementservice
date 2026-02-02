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

import java.time.*;
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

    @Value("${mqtt.api.base-url}")
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

                // ---------- Collect MQTT data ----------
                List<Map<String, Object>> allData = new ArrayList<>();

                for (Device device : devices) {
                    try {
                        String url = String.format(
                                "%s/api/mqtt-data/device/%s",
                                mqttApiBaseUrl,
                                device.getDeviceId()
                        );

                        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                        if (response != null
                                && "success".equals(response.get("status"))
                                && response.containsKey("data")) {

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> deviceData =
                                    (List<Map<String, Object>>) response.get("data");

                            List<Map<String, Object>> filtered = deviceData.stream()
                                    .filter(d -> {
                                        try {
                                            if (!d.containsKey("timestamp")) return false;
                                            LocalDateTime ts = LocalDateTime.parse(
                                                    d.get("timestamp").toString(),
                                                    DateTimeFormatter.ISO_DATE_TIME
                                            );
                                            return !ts.isBefore(startTime) && !ts.isAfter(endTime);
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    })
                                    .toList();

                            filtered.forEach(d -> d.put("deviceId", device.getDeviceId()));
                            allData.addAll(filtered);
                        }
                    } catch (Exception e) {
                        log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
                    }
                }

                if (allData.isEmpty()) continue;

                // ---------- Aggregated stats (single source of truth) ----------
                Map<String, Object> stats = calculateAggregatedStats(allData, devices.size());

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
                comparison.put("totalReadings", stats.get("totalReadings"));
                comparison.put("congestionRate", stats.get("congestionRate"));

                comparison.put("filterValue", filterValue);

                comparisons.add(comparison);

            } catch (Exception e) {
                log.error("Error processing counter {}: {}", counterCode, e.getMessage(), e);
            }
        }

        // ---------- Sort ----------
        comparisons.sort(Comparator.comparingDouble(
                c -> ((Number) c.get("filterValue")).doubleValue()
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("counterCodes", counterCodes);
        response.put("counterCount", counterCodes.length);
        response.put("filterType", filterType);
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("comparisons", comparisons);

        // ---------- Insights ----------
        if (!comparisons.isEmpty()) {
            Map<String, Object> best = comparisons.get(0);
            Map<String, Object> worst = comparisons.get(comparisons.size() - 1);

            response.put("insights", Map.of(
                    "bestPerforming", Map.of(
                            "counterCode", best.get("counterCode"),
                            "counterName", best.get("counterName"),
                            "averageQueue", best.get("averageTotalQueue"),
                            "congestionRate", best.get("congestionRate")
                    ),
                    "worstPerforming", Map.of(
                            "counterCode", worst.get("counterCode"),
                            "counterName", worst.get("counterName"),
                            "averageQueue", worst.get("averageTotalQueue"),
                            "congestionRate", worst.get("congestionRate")
                    )
            ));
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

        Counter counter = counterRepository.findByCounterCode(counterCode)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

        List<Device> devices = deviceRepository.findByCounterId(counter.getId());
        if (devices.isEmpty()) {
            return createEmptyResponse(counterCode, counter.getCounterName());
        }

        // ---------- Collect MQTT data ----------
        List<Map<String, Object>> allData = new ArrayList<>();

        for (Device device : devices) {
            try {
                String url = String.format(
                        "%s/api/mqtt-data/device/%s",
                        mqttApiBaseUrl,
                        device.getDeviceId()
                );

                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response != null
                        && "success".equals(response.get("status"))
                        && response.containsKey("data")) {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> deviceData =
                            (List<Map<String, Object>>) response.get("data");

                    List<Map<String, Object>> filtered = deviceData.stream()
                            .filter(d -> {
                                try {
                                    if (!d.containsKey("timestamp")) return false;
                                    LocalDateTime ts = LocalDateTime.parse(
                                            d.get("timestamp").toString(),
                                            DateTimeFormatter.ISO_DATE_TIME
                                    );
                                    return !ts.isBefore(startTime) && !ts.isAfter(endTime);
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .toList();

                    filtered.forEach(d -> d.put("deviceId", device.getDeviceId()));
                    allData.addAll(filtered);
                }
            } catch (Exception e) {
                log.error("Error fetching data for device {}: {}", device.getDeviceId(), e.getMessage());
            }
        }

        if (allData.isEmpty()) {
            return createEmptyResponse(counterCode, counter.getCounterName());
        }

        // ======================================================
        // 🔥 CURRENT DAY ONLY FILTER (CRITICAL FIX)
        // ======================================================
        LocalDate today = endTime.toLocalDate();

        List<Map<String, Object>> todayData = allData.stream()
                .filter(d -> {
                    try {
                        if (!d.containsKey("timestamp")) return false;
                        LocalDateTime ts = LocalDateTime.parse(
                                d.get("timestamp").toString(),
                                DateTimeFormatter.ISO_DATE_TIME
                        );
                        return ts.toLocalDate().equals(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        // ---------- Hourly pattern (TODAY ONLY) ----------
        List<Map<String, Object>> hourlyPattern =
                calculateHourlyPattern(todayData, devices.size());

        // ---------- Overall statistics (range based) ----------
        Map<String, Object> statistics =
                calculateAggregatedStats(allData, devices.size());

        // ---------- Peak / Low hours (TODAY ONLY) ----------
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

                // 🔥 ONLY CHANGE: queueLength → occupancy
                Double occupancy = extractOccupancyValue(data);

                if (occupancy == null) {
                    continue;
                }

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME);

                LocalDateTime bucket = roundToInterval(timestamp, minutes);
                String deviceId = (String) data.get("deviceId");

                // 🔥 SAME logic, just occupancy value
                bucketDeviceMap.computeIfAbsent(bucket, k -> new HashMap<>())
                        .put(deviceId, occupancy);
            }
        }

        // Calculate aggregates for each time bucket (UNCHANGED)
        return bucketDeviceMap.entrySet().stream()
                .map(entry -> {
                    LocalDateTime timestamp = entry.getKey();
                    Map<String, Double> deviceOccupancies = entry.getValue();

                    double totalOccupancy = deviceOccupancies.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    DoubleSummaryStatistics stats = deviceOccupancies.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

                    Map<String, Object> trend = new HashMap<>();
                    trend.put("timestamp", timestamp);

                    // 🔥 Rename fields if you want
                    trend.put("occupancy", totalOccupancy);          // instead of totalQueueLength
                    trend.put("averageOccupancy", stats.getAverage());
                    trend.put("maxOccupancy", stats.getMax());
                    trend.put("minOccupancy", stats.getMin());

                    trend.put("activeDeviceCount", deviceOccupancies.size());
                    trend.put("totalDeviceCount", totalDeviceCount);
                    trend.put("dataPointCount", deviceOccupancies.size());

                    return trend;
                })
                .collect(Collectors.toList());
    }

    private Double extractOccupancyValue(Map<String, Object> data) {

        Object value = data.get("occupancy"); // 🔥 THIS IS THE LOGIC

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }



    /**
     * Calculate aggregated statistics across all timestamps
     */
    private Map<String, Object> calculateCounterStatistics(
            List<Map<String, Object>> trends,
            List<Map<String, Object>> allMqttData) {

        Map<String, Object> statistics = new HashMap<>();

        if (!trends.isEmpty()) {

            // ✅ OCCUPANCY statistics (NULL SAFE)
            DoubleSummaryStatistics occupancyStats = trends.stream()
                    .map(t -> t.get("occupancy"))
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .summaryStatistics();

            statistics.put("averageOccupancy", occupancyStats.getAverage());
            statistics.put("maxOccupancy", occupancyStats.getMax());
            statistics.put("minOccupancy", occupancyStats.getMin());

            // ✅ Per-device occupancy stats
            long count = allMqttData.stream()
                    .map(this::extractOccupancyValue)   // ⬅️ make sure this exists
                    .filter(Objects::nonNull)
                    .count();

            double average = allMqttData.stream()
                    .map(this::extractOccupancyValue)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            statistics.put("averageOccupancyPerDevice", average);
            statistics.put("totalReadings", count);

            // ✅ Efficiency based on occupancy
            double avg = occupancyStats.getAverage();
            double max = occupancyStats.getMax();
            statistics.put("efficiency", calculateEfficiency(avg, max));

            // ✅ Trend based on occupancy
            List<Double> occupancies = trends.stream()
                    .map(t -> t.get("occupancy"))
                    .filter(Objects::nonNull)
                    .map(v -> ((Number) v).doubleValue())
                    .collect(Collectors.toList());

            statistics.put("trend", calculateTrend(occupancies));
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

        // Group by minute → occupancy values
        Map<LocalDateTime, List<Double>> timeOccupancy = new HashMap<>();

        List<Double> allOccupancies = new ArrayList<>();

        for (Map<String, Object> data : allData) {
            if (!data.containsKey("timestamp") || !data.containsKey("occupancy")) {
                continue;
            }

            try {
                double occupancy = ((Number) data.get("occupancy")).doubleValue();
                allOccupancies.add(occupancy);

                LocalDateTime timestamp = LocalDateTime.parse(
                        data.get("timestamp").toString(),
                        DateTimeFormatter.ISO_DATE_TIME
                ).truncatedTo(ChronoUnit.MINUTES);

                timeOccupancy
                        .computeIfAbsent(timestamp, k -> new ArrayList<>())
                        .add(occupancy);

            } catch (Exception ignored) {
            }
        }

        // ---- Average occupancy per timestamp ----
        List<Double> perTimestampAvgOccupancy = timeOccupancy.values().stream()
                .map(list -> list.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0))
                .toList();

        if (!perTimestampAvgOccupancy.isEmpty()) {
            DoubleSummaryStatistics summary =
                    perTimestampAvgOccupancy.stream()
                            .mapToDouble(Double::doubleValue)
                            .summaryStatistics();

            stats.put("averageTotalQueue", summary.getAverage()); // avg occupancy
            stats.put("maxTotalQueue", summary.getMax());         // peak occupancy
            stats.put("minTotalQueue", summary.getMin());
        } else {
            stats.put("averageTotalQueue", 0.0);
            stats.put("maxTotalQueue", 0.0);
            stats.put("minTotalQueue", 0.0);
        }

        // ---- Per-reading stats ----
        stats.put("averageQueuePerDevice",
                allOccupancies.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0));

        stats.put("totalReadings", allOccupancies.size());

        // =====================================================
        // ✅ CONGESTION RATE (as per your definition)
        // Occupancy ≥ 4  ⇒ Wait time ≥ 8 minutes
        // =====================================================
        long congestedReadings = allOccupancies.stream()
                .filter(o -> o >= 4)
                .count();

        double congestionRate = allOccupancies.isEmpty()
                ? 0.0
                : (congestedReadings * 100.0) / allOccupancies.size();

        stats.put("congestionRate", congestionRate);

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

        Counter counter = counterRepository.findByCounterCode(counterCode)
                .orElseThrow(() -> new IllegalArgumentException("Counter not found"));

        List<Device> devices = deviceRepository.findByCounterId(counter.getId());

        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> today =
                calculateFootfallForPeriod(
                        devices,
                        now.toLocalDate().atStartOfDay(),
                        now
                );

        Map<String, Object> yesterday =
                calculateFootfallForPeriod(
                        devices,
                        now.minusDays(1).toLocalDate().atStartOfDay(),
                        now.minusDays(1).toLocalDate().atTime(23,59,59)
                );

        Map<String, Object> lastWeek =
                calculateFootfallForPeriod(
                        devices,
                        now.minusWeeks(1).toLocalDate().atStartOfDay(),
                        now.minusWeeks(1).toLocalDate().atTime(23,59,59)
                );

        Map<String, Object> lastMonth =
                calculateFootfallForPeriod(
                        devices,
                        now.minusMonths(1).toLocalDate().atStartOfDay(),
                        now.minusMonths(1).toLocalDate().atTime(23,59,59)
                );

        Map<String, Object> lastYear =
                calculateFootfallForPeriod(
                        devices,
                        now.minusYears(1).toLocalDate().atStartOfDay(),
                        now.minusYears(1).toLocalDate().atTime(23,59,59)
                );

        Map<String, Object> response = new HashMap<>();
        response.put("counterCode", counterCode);
        response.put("counterName", counter.getCounterName());
        response.put("generatedAt", now);

        response.put("today", today);
        response.put("yesterday", yesterday);
        response.put("lastWeekSameDay", lastWeek);
        response.put("lastMonthSameDay", lastMonth);
        response.put("lastYearSameDay", lastYear);

        return response;
    }




    private double getFootfall(Map<String, Object> session) {
        if (session == null) return 0.0;

        Object value = session.get("totalFootfall");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }




    private Map<String, LocalTime[]> getSessions() {

        Map<String, LocalTime[]> sessions = new LinkedHashMap<>();

        sessions.put("morning", new LocalTime[]{
                LocalTime.of(7, 0),
                LocalTime.of(11, 0)
        });

        sessions.put("afternoon", new LocalTime[]{
                LocalTime.of(11, 0),
                LocalTime.of(15, 0)
        });

        sessions.put("evening", new LocalTime[]{
                LocalTime.of(15, 0),
                LocalTime.of(19, 0)
        });

        return sessions;
    }



    /**
     * Get aggregated footfall summary across all active counters
     */
    public Map<String, Object> getAllCountersFootfallSummary() {

        log.info("Fetching SESSION-based aggregated footfall summary for ALL counters");

        try {
            List<Counter> counters = counterRepository.findByActive(true);

            if (counters.isEmpty()) {
                return createEmptyFootfallSummary("ALL");
            }

            List<Device> allDevices = new ArrayList<>();
            for (Counter counter : counters) {
                allDevices.addAll(deviceRepository.findByCounterId(counter.getId()));
            }

            if (allDevices.isEmpty()) {
                return createEmptyFootfallSummary("ALL");
            }

            LocalDateTime now = LocalDateTime.now();

            Map<String, Object> today = calculateFootfallForPeriod(
                    allDevices,
                    now.toLocalDate().atStartOfDay(),
                    now
            );

            Map<String, Object> yesterday = calculateFootfallForPeriod(
                    allDevices,
                    now.minusDays(1).toLocalDate().atStartOfDay(),
                    now.minusDays(1).toLocalDate().atTime(23, 59, 59)
            );

            double todayCount = ((Number) today.get("totalFootfall")).doubleValue();
            double yesterdayCount = ((Number) yesterday.get("totalFootfall")).doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("scope", "ALL_COUNTERS");
            response.put("counterCount", counters.size());
            response.put("totalDevices", allDevices.size());
            response.put("generatedAt", now);

            response.put("today", Map.of(
                    "date", now.toLocalDate(),
                    "count", Math.round(todayCount),
                    "percentage", null
            ));

            response.put("yesterday", Map.of(
                    "date", now.minusDays(1).toLocalDate(),
                    "count", Math.round(yesterdayCount),
                    "percentage", calculatePercentageChange(yesterdayCount, todayCount)
            ));

            return response;

        } catch (Exception e) {
            log.error("Error calculating aggregated footfall summary", e);
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

        double totalFootfall = 0.0;
        int devicesWithData = 0;

        List<Map<String, Object>> trendPoints = new ArrayList<>();

        for (Device device : devices) {
            try {
                String url = String.format(
                        "%s/api/mqtt-data/device/%s",
                        mqttApiBaseUrl,
                        device.getDeviceId()
                );

                Map<String, Object> response =
                        restTemplate.getForObject(url, Map.class);

                if (response == null || !"success".equals(response.get("status")))
                    continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data =
                        (List<Map<String, Object>>) response.get("data");

                double deviceMax = 0.0;

                for (Map<String, Object> row : data) {
                    if (!row.containsKey("timestamp")) continue;

                    LocalDateTime time = LocalDateTime.parse(
                            row.get("timestamp").toString(),
                            DateTimeFormatter.ISO_DATE_TIME
                    );

                    if (time.isBefore(startTime) || time.isAfter(endTime))
                        continue;

                    Double inCount = extractQueueValue(row);
                    if (inCount == null) continue;

                    // trend point
                    Map<String, Object> point = new HashMap<>();
                    point.put("time", time);
                    point.put("value", inCount);
                    trendPoints.add(point);

                    deviceMax = Math.max(deviceMax, inCount);
                }

                if (deviceMax > 0) {
                    totalFootfall += deviceMax;
                    devicesWithData++;
                }

            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalFootfall", totalFootfall);
        result.put("devicesWithData", devicesWithData);
        result.put("totalDevices", devices.size());
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("trendPoints", trendPoints);

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

    /**
     * NEW METHOD ADDITION TO CounterAnalyticsService.java
     * Add this method to your existing CounterAnalyticsService class
     */

    /**
     * Get Daily Footfall vs Wait Time Analysis
     * Analyzes the relationship between foot traffic (inCount) and waiting times
     * throughout the day with hourly breakdown
     *
     * @param counterCode The counter to analyze
     * @param targetDate The specific date to analyze
     * @return Map containing hourly breakdown, daily summary, and peak analysis
     */
    public Map<String, Object> getDailyFootfallVsWaitTime(
            String counterCode,
            LocalDate targetDate) {

        log.info("Fetching daily footfall vs wait time for counter: {} on {}",
                counterCode, targetDate);

        try {
            Counter counter = counterRepository.findByCounterCode(counterCode)
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + counterCode));

            List<Device> devices = deviceRepository.findByCounterId(counter.getId());

            if (devices.isEmpty()) {
                return createEmptyFootfallWaitTimeResponse(counterCode, counter.getCounterName(), targetDate);
            }

            LocalDateTime startTime = targetDate.atStartOfDay();
            LocalDateTime endTime = targetDate.atTime(23, 59, 59);

            // Collect MQTT data for all devices
            List<Map<String, Object>> allMqttData = new ArrayList<>();

            for (Device device : devices) {
                try {
                    String url = String.format("%s/api/mqtt-data/device/%s",
                            mqttApiBaseUrl, device.getDeviceId());

                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                    if (response != null && "success".equals(response.get("status"))
                            && response.containsKey("data")) {

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> deviceData =
                                (List<Map<String, Object>>) response.get("data");

                        List<Map<String, Object>> filteredData = deviceData.stream()
                                .filter(data -> {
                                    if (data.containsKey("timestamp")) {
                                        try {
                                            LocalDateTime dataTime = LocalDateTime.parse(
                                                    data.get("timestamp").toString(),
                                                    DateTimeFormatter.ISO_DATE_TIME);
                                            return !dataTime.isBefore(startTime)
                                                    && !dataTime.isAfter(endTime);
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList());

                        filteredData.forEach(d -> d.put("deviceId", device.getDeviceId()));
                        allMqttData.addAll(filteredData);
                    }
                } catch (Exception e) {
                    log.error("Error fetching data for device {}: {}",
                            device.getDeviceId(), e.getMessage());
                }
            }

            if (allMqttData.isEmpty()) {
                return createEmptyFootfallWaitTimeResponse(counterCode, counter.getCounterName(), targetDate);
            }

            // Calculate hourly breakdown
            List<Map<String, Object>> hourlyBreakdown =
                    calculateHourlyFootfallVsWaitTime(allMqttData, devices.size());

            // Calculate daily summary statistics
            Map<String, Object> dailySummary =
                    calculateDailyFootfallWaitTimeSummary(allMqttData, hourlyBreakdown);

            // Identify peak periods
            Map<String, Object> peakAnalysis =
                    identifyPeakPeriodsForFootfallWaitTime(hourlyBreakdown);

            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("counterCode", counterCode);
            result.put("counterName", counter.getCounterName());
            result.put("counterType", counter.getCounterType());
            result.put("date", targetDate);
            result.put("deviceCount", devices.size());
            result.put("totalDataPoints", allMqttData.size());
            result.put("hourlyBreakdown", hourlyBreakdown);
            result.put("dailySummary", dailySummary);
            result.put("peakAnalysis", peakAnalysis);

            log.info("Successfully calculated footfall vs wait time analysis for {} hours",
                    hourlyBreakdown.size());

            return result;

        } catch (Exception e) {
            log.error("Error calculating footfall vs wait time: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating footfall vs wait time analysis", e);
        }
    }

    /**
     * Calculate hourly breakdown of footfall and wait time metrics
     */
    private List<Map<String, Object>> calculateHourlyFootfallVsWaitTime(
            List<Map<String, Object>> mqttData,
            int totalDeviceCount) {

        // Group data by hour
        Map<Integer, List<Map<String, Object>>> hourlyData = new TreeMap<>();

        for (Map<String, Object> data : mqttData) {
            if (data.containsKey("timestamp")) {
                try {
                    LocalDateTime timestamp = LocalDateTime.parse(
                            data.get("timestamp").toString(),
                            DateTimeFormatter.ISO_DATE_TIME);
                    int hour = timestamp.getHour();

                    hourlyData.computeIfAbsent(hour, k -> new ArrayList<>()).add(data);
                } catch (Exception e) {
                    log.debug("Error parsing timestamp: {}", e.getMessage());
                }
            }
        }

        // Calculate metrics for each hour
        List<Map<String, Object>> hourlyBreakdown = new ArrayList<>();

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : hourlyData.entrySet()) {
            int hour = entry.getKey();
            List<Map<String, Object>> hourData = entry.getValue();

            // Extract inCount values (footfall)
            List<Double> footfallValues = hourData.stream()
                    .map(this::extractInCount)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Extract wait time values
            List<Double> waitTimeValues = hourData.stream()
                    .map(this::extractWaitTime)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!footfallValues.isEmpty() || !waitTimeValues.isEmpty()) {
                DoubleSummaryStatistics footfallStats = footfallValues.stream()
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

                DoubleSummaryStatistics waitTimeStats = waitTimeValues.stream()
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

                Map<String, Object> hourMetrics = new HashMap<>();
                hourMetrics.put("hour", hour);
                hourMetrics.put("hourLabel", String.format("%02d:00 - %02d:00", hour, hour + 1));

                // Footfall metrics
                hourMetrics.put("totalFootfall", roundToOneDecimal(footfallStats.getSum()));
                hourMetrics.put("averageFootfall", roundToOneDecimal(footfallStats.getAverage()));
                hourMetrics.put("peakFootfall", roundToOneDecimal(footfallStats.getMax()));
                hourMetrics.put("minFootfall", roundToOneDecimal(footfallStats.getMin()));

                // Wait time metrics
                hourMetrics.put("averageWaitTime", roundToOneDecimal(waitTimeStats.getAverage()));
                hourMetrics.put("maxWaitTime", roundToOneDecimal(waitTimeStats.getMax()));
                hourMetrics.put("minWaitTime", roundToOneDecimal(waitTimeStats.getMin()));

                // Correlation metrics
                double footfallWaitRatio = footfallStats.getAverage() > 0
                        ? waitTimeStats.getAverage() / footfallStats.getAverage()
                        : 0.0;
                hourMetrics.put("footfallWaitRatio", roundToOneDecimal(footfallWaitRatio));

                // Activity level
                hourMetrics.put("dataPointCount", hourData.size());
                hourMetrics.put("activeDevices", calculateActiveDevices(hourData));

                hourlyBreakdown.add(hourMetrics);
            }
        }

        return hourlyBreakdown;
    }

    /**
     * Calculate daily summary statistics
     */
    private Map<String, Object> calculateDailyFootfallWaitTimeSummary(
            List<Map<String, Object>> allData,
            List<Map<String, Object>> hourlyBreakdown) {

        // Extract all footfall values
        List<Double> allFootfall = allData.stream()
                .map(this::extractInCount)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Extract all wait time values
        List<Double> allWaitTimes = allData.stream()
                .map(this::extractWaitTime)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();

        if (!allFootfall.isEmpty()) {
            DoubleSummaryStatistics footfallStats = allFootfall.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            summary.put("totalFootfall", roundToOneDecimal(footfallStats.getSum()));
            summary.put("averageFootfall", roundToOneDecimal(footfallStats.getAverage()));
            summary.put("peakFootfall", roundToOneDecimal(footfallStats.getMax()));
            summary.put("footfallReadings", footfallStats.getCount());
        }

        if (!allWaitTimes.isEmpty()) {
            DoubleSummaryStatistics waitTimeStats = allWaitTimes.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            summary.put("averageWaitTime", roundToOneDecimal(waitTimeStats.getAverage()));
            summary.put("maxWaitTime", roundToOneDecimal(waitTimeStats.getMax()));
            summary.put("minWaitTime", roundToOneDecimal(waitTimeStats.getMin()));
            summary.put("waitTimeReadings", waitTimeStats.getCount());

            // Calculate service level (percentage of readings with acceptable wait time)
            // Assuming acceptable wait time is <= 5 minutes
            long acceptableWaitCount = allWaitTimes.stream()
                    .filter(wt -> wt <= 5.0)
                    .count();
            double serviceLevel = (acceptableWaitCount * 100.0) / allWaitTimes.size();
            summary.put("serviceLevel", roundToOneDecimal(serviceLevel));
            summary.put("acceptableWaitThreshold", 5.0);
        }

        // Overall correlation
        if (!allFootfall.isEmpty() && !allWaitTimes.isEmpty()) {
            double avgFootfall = allFootfall.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            double avgWaitTime = allWaitTimes.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            summary.put("overallFootfallWaitRatio",
                    avgFootfall > 0 ? roundToOneDecimal(avgWaitTime / avgFootfall) : 0.0);
        }

        return summary;
    }

    /**
     * Identify peak periods for footfall and wait time
     */
    private Map<String, Object> identifyPeakPeriodsForFootfallWaitTime(
            List<Map<String, Object>> hourlyBreakdown) {

        Map<String, Object> peakAnalysis = new HashMap<>();

        if (hourlyBreakdown.isEmpty()) {
            return peakAnalysis;
        }

        // Find peak footfall hour
        Map<String, Object> peakFootfallHour = hourlyBreakdown.stream()
                .max(Comparator.comparing(h -> ((Number) h.get("totalFootfall")).doubleValue()))
                .orElse(null);

        if (peakFootfallHour != null) {
            peakAnalysis.put("peakFootfallHour", Map.of(
                    "hour", peakFootfallHour.get("hour"),
                    "hourLabel", peakFootfallHour.get("hourLabel"),
                    "footfall", peakFootfallHour.get("totalFootfall"),
                    "waitTime", peakFootfallHour.get("averageWaitTime")
            ));
        }

        // Find peak wait time hour
        Map<String, Object> peakWaitTimeHour = hourlyBreakdown.stream()
                .max(Comparator.comparing(h -> ((Number) h.get("averageWaitTime")).doubleValue()))
                .orElse(null);

        if (peakWaitTimeHour != null) {
            peakAnalysis.put("peakWaitTimeHour", Map.of(
                    "hour", peakWaitTimeHour.get("hour"),
                    "hourLabel", peakWaitTimeHour.get("hourLabel"),
                    "waitTime", peakWaitTimeHour.get("averageWaitTime"),
                    "footfall", peakWaitTimeHour.get("totalFootfall")
            ));
        }

        // Find best performing hour (high footfall, low wait time)
        Map<String, Object> bestPerformingHour = hourlyBreakdown.stream()
                .min(Comparator.comparing(h -> {
                    double footfall = ((Number) h.get("totalFootfall")).doubleValue();
                    double waitTime = ((Number) h.get("averageWaitTime")).doubleValue();
                    return footfall > 0 ? waitTime / footfall : Double.MAX_VALUE;
                }))
                .orElse(null);

        if (bestPerformingHour != null) {
            peakAnalysis.put("bestPerformingHour", Map.of(
                    "hour", bestPerformingHour.get("hour"),
                    "hourLabel", bestPerformingHour.get("hourLabel"),
                    "footfall", bestPerformingHour.get("totalFootfall"),
                    "waitTime", bestPerformingHour.get("averageWaitTime"),
                    "ratio", bestPerformingHour.get("footfallWaitRatio")
            ));
        }

        // Find worst performing hour (high wait time relative to footfall)
        Map<String, Object> worstPerformingHour = hourlyBreakdown.stream()
                .max(Comparator.comparing(h -> ((Number) h.get("footfallWaitRatio")).doubleValue()))
                .orElse(null);

        if (worstPerformingHour != null) {
            peakAnalysis.put("worstPerformingHour", Map.of(
                    "hour", worstPerformingHour.get("hour"),
                    "hourLabel", worstPerformingHour.get("hourLabel"),
                    "footfall", worstPerformingHour.get("totalFootfall"),
                    "waitTime", worstPerformingHour.get("averageWaitTime"),
                    "ratio", worstPerformingHour.get("footfallWaitRatio")
            ));
        }

        return peakAnalysis;
    }

    /**
     * Extract inCount value from MQTT data
     */
    private Double extractInCount(Map<String, Object> data) {
        if (data.containsKey("inCount")) {
            Object value = data.get("inCount");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extract wait time value from MQTT data
     */
    private Double extractWaitTime(Map<String, Object> data) {
        if (data.containsKey("waitTime")) {
            Object value = data.get("waitTime");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Calculate number of active devices in a dataset
     */
    private int calculateActiveDevices(List<Map<String, Object>> data) {
        return (int) data.stream()
                .map(d -> d.get("deviceId"))
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    /**
     * Create empty response for footfall vs wait time analysis
     */
    private Map<String, Object> createEmptyFootfallWaitTimeResponse(
            String counterCode,
            String counterName,
            LocalDate date) {

        Map<String, Object> response = new HashMap<>();
        response.put("counterCode", counterCode);
        response.put("counterName", counterName);
        response.put("date", date);
        response.put("deviceCount", 0);
        response.put("totalDataPoints", 0);
        response.put("hourlyBreakdown", Collections.emptyList());
        response.put("dailySummary", Collections.emptyMap());
        response.put("peakAnalysis", Collections.emptyMap());

        return response;
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