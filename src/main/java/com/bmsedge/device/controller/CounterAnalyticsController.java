package com.bmsedge.device.controller;

import com.bmsedge.device.dto.CounterTrendResponse;
import com.bmsedge.device.model.CounterSessionAnalytics;
import com.bmsedge.device.client.MqttAggregationClient;
import com.bmsedge.device.dto.CounterTrendResponse;
import com.bmsedge.device.dto.MqttAggregationDTO;
import com.bmsedge.device.service.CounterAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bmsedge.device.service.LiveCounterStatusService;
import com.bmsedge.device.repository.CounterSessionAnalyticsRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Counter Analytics Controller
 * Provides counter-specific queue analytics with cumulative aggregation
 */
@RestController
@RequestMapping("/api/counter-analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CounterAnalyticsController {

    private final CounterAnalyticsService counterAnalyticsService;

    private final LiveCounterStatusService liveCounterStatusService;

    private final MqttAggregationClient mqttAggregationClient;



    private final CounterSessionAnalyticsRepository  counterSessionAnalyticsRepository;

    /**
     * Get queue trends for a specific counter
     * GET /api/counter-analytics/counter/{counterCode}/trends
     *
     * Query params:
     * - startTime: ISO datetime (optional, defaults to 24h ago)
     * - endTime: ISO datetime (optional, defaults to now)
     * - interval: 15min, 30min, 1hour, 4hour, 1day (optional, defaults to 1hour)
     *
     * Response includes:
     * - trends: Array of aggregated queue data with totalQueueLength (SUM of all devices)
     * - statistics: Overall counter performance metrics
     * - deviceBreakdown: Per-device contribution analysis
     */
    @GetMapping("/counter/{counterCode}/trends")
    public ResponseEntity<?> getCounterQueueTrends(
            @PathVariable("counterCode") String counterCode,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "interval", defaultValue = "1hour") String interval) {

        try {
            log.info("Fetching counter queue trends: counter={}, interval={}", counterCode, interval);

            // Trim whitespace and newlines from counterCode
            counterCode = counterCode.trim();

            if (counterCode.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter code cannot be empty"));
            }

            LocalDate today = LocalDate.now();

            if (startTime == null) {
                startTime = today.atStartOfDay();
            }
            if (endTime == null) {
                endTime = today.atTime(LocalTime.MAX);
            }

            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start time must be before end time"));
            }

            Map<String, Object> response = counterAnalyticsService.getCounterQueueTrends(
                    counterCode, startTime, endTime, interval);

            log.info("Successfully fetched counter queue trends for: {}", counterCode);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching counter queue trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching counter queue trends: " + e.getMessage()));
        }
    }

    /**
     * Compare performance across multiple counters
     * GET /api/counter-analytics/compare
     *
     * Query params:
     * - counterCodes: Comma-separated counter codes (required)
     * - startTime: ISO datetime (optional)
     * - endTime: ISO datetime (optional)
     * - filterType: avg, max, min (optional, defaults to avg)
     *
     * Response includes cumulative queue metrics for each counter
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareCounterPerformance(
            @RequestParam("counterCodes") String counterCodes,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "filterType", defaultValue = "avg") String filterType) {

        try {
            log.info("Comparing counter performance: counters={}, filter={}", counterCodes, filterType);

            if (counterCodes == null || counterCodes.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter codes cannot be null or empty"));
            }

            String[] counterCodeArray = counterCodes.split(",");
            if (counterCodeArray.length < 2) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("At least 2 counter codes required for comparison"));
            }
            if (counterCodeArray.length > 10) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Maximum 10 counters can be compared at once"));
            }

            // Validate filter type
            if (!Arrays.asList("avg", "max", "min").contains(filterType.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid filter type. Use: avg, max, min"));
            }

            // Set default time range
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusHours(24);
            }

            Map<String, Object> response = counterAnalyticsService.compareCounterPerformance(
                    counterCodeArray, startTime, endTime, filterType);

            log.info("Successfully compared {} counters", counterCodeArray.length);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error comparing counter performance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error comparing counter performance: " + e.getMessage()));
        }
    }

    /**
     * Get counter performance analysis
     * GET /api/counter-analytics/counter/{counterCode}/performance
     *
     * Query params:
     * - startTime: ISO datetime (optional, defaults to 7 days ago)
     * - endTime: ISO datetime (optional, defaults to now)
     *
     * Response includes hourly pattern with cumulative queue data
     */
    @GetMapping("/counter/{counterCode}/performance")
    public ResponseEntity<?> getCounterPerformanceAnalysis(
            @PathVariable("counterCode") String counterCode,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            log.info("Analyzing counter performance: {}", counterCode);

            if (counterCode == null || counterCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter code cannot be null or empty"));
            }

            // Set default time range
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusDays(7);
            }

            Map<String, Object> response = counterAnalyticsService.getCounterPerformanceAnalysis(
                    counterCode, startTime, endTime);

            log.info("Successfully analyzed performance for counter: {}", counterCode);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error analyzing counter performance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error analyzing counter performance: " + e.getMessage()));
        }
    }

    /**
     * Get LIVE real-time status for counters
     * GET /api/counter-analytics/live-status
     *
     * Query params:
     * - counterCodes: Comma-separated counter codes (optional, defaults to all active counters)
     *
     * Returns LATEST MQTT data (not historical averages):
     * - currentQueueLength: Live queue count (sum of all devices)
     * - currentInCount: Real-time inCount from MQTT
     * - estimatedWaitTime: Calculated wait time in minutes
     * - lastUpdated: Timestamp of last MQTT message
     * - status: "active" | "inactive"
     */
    @GetMapping("/live-status")
    public ResponseEntity<?> getLiveCounterStatus(
            @RequestParam(value = "counterCodes", required = false) String counterCodes) {

        try {
            log.info("Fetching live counter status");

            String[] counterCodeArray = null;
            if (counterCodes != null && !counterCodes.trim().isEmpty()) {
                counterCodeArray = counterCodes.split(",");
                // Trim whitespace from each code
                counterCodeArray = Arrays.stream(counterCodeArray)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            }

            Map<String, Object> response = liveCounterStatusService.getLiveCounterStatus(counterCodeArray);

            log.info("Successfully fetched live status for counters");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching live counter status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching live counter status: " + e.getMessage()));
        }
    }

    /**
     * Get occupancy trends for a specific counter
     * GET /api/counter-analytics/counter/{counterCode}/occupancy-trends
     *
     * Query params:
     * - startTime: ISO datetime (optional, defaults to 24h ago)
     * - endTime: ISO datetime (optional, defaults to now)
     * - interval: 15min, 30min, 1hour (optional, defaults to 1hour)
     *
     * Response: Occupancy trend data over time
     */
    @GetMapping("/counter/{counterCode}/occupancy-trends")
    public ResponseEntity<?> getOccupancyTrends(
            @PathVariable("counterCode") String counterCode,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "interval", defaultValue = "1hour") String interval) {

        try {
            log.info("Fetching occupancy trends: counter={}, interval={}", counterCode, interval);

            counterCode = counterCode.trim();

            if (counterCode.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter code cannot be empty"));
            }

            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusHours(24);
            }

            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start time must be before end time"));
            }

            Map<String, Object> response = liveCounterStatusService.getOccupancyTrends(
                    counterCode, startTime, endTime, interval);

            log.info("Successfully fetched occupancy trends for: {}", counterCode);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching occupancy trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching occupancy trends: " + e.getMessage()));
        }
    }

    /**
     * Get Footfall Summary with Counter Filtering
     * GET /api/counter-analytics/footfall-summary
     *
     * Query params:
     * - counterCode: Single counter code (optional)
     * - counterCodes: Comma-separated counter codes (optional)
     * - If no param provided: Returns aggregated data for all counters
     *
     * Returns footfall data with historical comparisons
     */
    @GetMapping("/footfall-summary")
    public ResponseEntity<?> getFootfallSummary(
            @RequestParam(value = "counterCode", required = false) String counterCode,
            @RequestParam(value = "counterCodes", required = false) String counterCodes) {

        try {
            log.info("Fetching footfall summary - counterCode: {}, counterCodes: {}",
                    counterCode, counterCodes);

            Map<String, Object> response;

            if (counterCode != null && !counterCode.trim().isEmpty()) {
                // Single counter footfall
                response = counterAnalyticsService.getCounterFootfallSummary(counterCode.trim());
            } else if (counterCodes != null && !counterCodes.trim().isEmpty()) {
                // Multiple counters footfall
                String[] codes = counterCodes.split(",");

                // Trim and validate
                codes = Arrays.stream(codes)
                        .map(String::trim)
                        .filter(c -> !c.isEmpty())
                        .toArray(String[]::new);

                if (codes.length == 0) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("No valid counter codes provided"));
                }

                if (codes.length > 20) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Maximum 20 counters allowed"));
                }

                response = counterAnalyticsService.getMultiCounterFootfallSummary(codes);
            } else {
                // All counters footfall
                response = counterAnalyticsService.getAllCountersFootfallSummary();
            }

            log.info("Successfully fetched footfall summary");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching footfall summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching footfall summary: " + e.getMessage()));
        }
    }

    // Add these methods to CounterAnalyticsController.java

    /**
     * Get Historical Queue Trends with Time-Based Aggregation
     * GET /api/counter-analytics/counter/{counterCode}/historical-trends
     *
     * Query params:
     * - startTime: ISO datetime (required)
     * - endTime: ISO datetime (required)
     * - granularity: hour, day, week, month (required)
     *
     * Returns time-series data aggregated by selected granularity
     */
    @GetMapping("/{counterCode}/historical-trends")
    public ResponseEntity<List<CounterTrendResponse>> getHistoricalTrends(
            @PathVariable String counterCode,
            @RequestParam String periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(23, 59, 59);

        return ResponseEntity.ok(
                counterAnalyticsService.getTrends(
                        counterCode,
                        periodType,
                        from,
                        to
                )
        );
    }

//
//    @GetMapping("/{counterCode}/analytics/trends")
//    public ResponseEntity<List<CounterTrendResponse>> getCounterTrends(
//            @PathVariable String counterCode,
//            @RequestParam String periodType,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        log.info("📊 Fetching trends for counter: {}, period: {}, from: {} to: {}",
//                counterCode, periodType, startDate, endDate);
//
//        List<CounterTrendResponse> response = new ArrayList<>();
//
//        if ("daily".equalsIgnoreCase(periodType)) {
//            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
//
//            for (long i = 0; i < daysBetween; i++) {
//                LocalDate currentDate = startDate.plusDays(i);
//
//                // Call MQTT aggregation API for each day
//                List<MqttAggregationDTO> dailyData =
//                        mqttAggregationClient.fetchHourlyAggregation(currentDate);
//
//                // Filter by counterCode and sum the counts
//                Long totalForDay = dailyData.stream()
//                        .filter(dto -> dto.getCounterName().equals(counterCode))
//                        .mapToLong(MqttAggregationDTO::getTotalCount)
//                        .sum();
//
//                if (totalForDay > 0) {
//                    response.add(new CounterTrendResponse(
//                            currentDate.atStartOfDay(),
//                            totalForDay
//                    ));
//                }
//            }
//
//            log.info("✅ Returned {} daily trend records for counter: {}",
//                    response.size(), counterCode);
//
//        } else if ("weekly".equalsIgnoreCase(periodType)) {
//            // Implement weekly aggregation
//            log.warn("⚠️ Weekly period type not yet implemented");
//            throw new UnsupportedOperationException("Weekly aggregation not yet implemented");
//
//        } else if ("monthly".equalsIgnoreCase(periodType)) {
//            // Implement monthly aggregation
//            log.warn("⚠️ Monthly period type not yet implemented");
//            throw new UnsupportedOperationException("Monthly aggregation not yet implemented");
//
//        } else {
//            log.error("❌ Invalid periodType: {}", periodType);
//            throw new IllegalArgumentException("Invalid periodType. Must be: daily, weekly, or monthly");
//        }
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/{counterCode}/trends")
    public ResponseEntity<List<CounterTrendResponse>> getCounterTrends(
            @PathVariable String counterCode,
            @RequestParam String periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                counterAnalyticsService.getTrends(
                        counterCode,
                        periodType,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                )
        );
    }






    /**
     * Get Current Day KPIs
     * GET /api/counter-analytics/counter/{counterCode}/current-day-kpis
     *
     * Returns KPI metrics for current day only (00:00 to now)
     * Ignores any date range selections
     */
    @GetMapping("/counter/{counterCode}/current-day-kpis")
    public ResponseEntity<?> getCurrentDayKPIs(
            @PathVariable("counterCode") String counterCode) {

        try {
            log.info("Fetching current day KPIs for counter: {}", counterCode);

            counterCode = counterCode.trim();

            if (counterCode.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter code cannot be empty"));
            }

            Map<String, Object> response = counterAnalyticsService.getCurrentDayKPIs(counterCode);

            log.info("Successfully fetched current day KPIs for: {}", counterCode);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching current day KPIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching current day KPIs: " + e.getMessage()));
        }
    }


    /**
     * Get all counters summary
     * GET /api/counter-analytics/summary
     *
     * Query params:
     * - startTime: ISO datetime (optional, defaults to 24h ago)
     * - endTime: ISO datetime (optional, defaults to now)
     *
     * Returns aggregated metrics for all active counters
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAllCountersSummary(
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            log.info("Fetching all counters summary");

            // Set default time range
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusHours(24);
            }

            List<Map<String, Object>> summaries = counterAnalyticsService.getAllCountersSummary(
                    startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("counterCount", summaries.size());
            response.put("counters", summaries);

            log.info("Successfully fetched summary for {} counters", summaries.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching counters summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching counters summary: " + e.getMessage()));
        }
    }

    // Add this mapping at the top of your CounterAnalyticsController class (after the class declaration)

    private static final Map<String, String> COUNTER_CODE_TO_MQTT_NAME = Map.of(
            "CNT001", "pan_pacific",
            "CNT002", "Mediterranean",
            "CNT003", "Tandoor"
            // Add more mappings as needed
    );

    /**
     * Get weekly peak queue data for chart display
     * GET /api/counter-analytics/{counterCode}/weekly-peak-queue
     *
     * Query params:
     * - startDate: Start date (optional, defaults to 7 days ago)
     * - endDate: End date (optional, defaults to today)
     *
     * Returns peak queue values for each day of the week
     */
    @GetMapping("/{counterCode}/weekly-peak-queue")
    public ResponseEntity<?> getWeeklyPeakQueue(
            @PathVariable String counterCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            log.info("Fetching weekly peak queue for counter: {}", counterCode);

            // Map counter code to MQTT name
            String mqttCounterName = COUNTER_CODE_TO_MQTT_NAME.get(counterCode);
            if (mqttCounterName == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Counter code not found: " + counterCode));
            }

            log.info("Mapped counter code {} to MQTT name: {}", counterCode, mqttCounterName);

            // Set defaults: last 7 days if not provided
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            if (startDate == null) {
                startDate = endDate.minusDays(6);
            }

            List<Map<String, Object>> weeklyData = new ArrayList<>();

            // Iterate through each date
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                // Call MQTT aggregation API for the date
                List<MqttAggregationDTO> dailyData = mqttAggregationClient.fetchHourlyAggregation(currentDate);

                // Find data using MQTT counter name
                Optional<MqttAggregationDTO> counterData = dailyData.stream()
                        .filter(dto -> dto.getCounterName().equalsIgnoreCase(mqttCounterName))
                        .findFirst();

                if (counterData.isPresent()) {
                    MqttAggregationDTO data = counterData.get();
                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("date", currentDate);
                    dayData.put("dayName", currentDate.getDayOfWeek().toString());
                    dayData.put("peakQueue", data.getPeakQueue());
                    dayData.put("totalCount", data.getTotalCount());
                    dayData.put("peakWaitTime", data.getPeakWaitTime());
                    dayData.put("congestionIndex", data.getCongestionIndex());
                    dayData.put("peakCongestion", data.getPeakCongestion());
                    dayData.put("periodStart", data.getPeriodStart());
                    weeklyData.add(dayData);
                }

                currentDate = currentDate.plusDays(1);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("counterCode", counterCode);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("data", weeklyData);

            log.info("Successfully fetched {} days of peak queue data for: {}", weeklyData.size(), counterCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching weekly peak queue: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching weekly peak queue: " + e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", "error");
        return errorResponse;
    }
}