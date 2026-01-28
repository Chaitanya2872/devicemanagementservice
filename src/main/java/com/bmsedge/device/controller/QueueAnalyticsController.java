package com.bmsedge.device.controller;

import com.bmsedge.device.dto.QueueTrendDTO;
import com.bmsedge.device.dto.QueueComparisonDTO;
import com.bmsedge.device.dto.AverageQueueDTO;
import com.bmsedge.device.service.QueueAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Queue Analytics Controller
 * Provides queue trends, comparisons, and average queue data with time filters
 */
@RestController
@RequestMapping("/api/queue-analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueueAnalyticsController {

    private final QueueAnalyticsService queueAnalyticsService;

    /**
     * Get queue trends for a device with time filter
     * GET /api/queue-analytics/device/{deviceId}/trends
     *
     * Query params:
     * - startTime: ISO datetime (optional, defaults to 24h ago)
     * - endTime: ISO datetime (optional, defaults to now)
     * - interval: 15min, 30min, 1hour, 4hour, 1day (optional, defaults to 1hour)
     */
    @GetMapping("/device/{deviceId}/trends")
    public ResponseEntity<?> getQueueTrends(
            @PathVariable("deviceId") String deviceId,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "interval", defaultValue = "1hour") String interval) {

        try {
            log.info("Fetching queue trends for device: {}, interval: {}", deviceId, interval);

            // Validate deviceId
            if (deviceId == null || deviceId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device ID cannot be null or empty"));
            }

            // Set default time range (last 24 hours)
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusHours(24);
            }

            // Validate time range
            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Start time must be before end time"));
            }

            // Get queue trends
            List<QueueTrendDTO> trends = queueAnalyticsService.getQueueTrends(
                    deviceId, startTime, endTime, interval);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("interval", interval);
            response.put("dataPoints", trends.size());
            response.put("trends", trends);

            // Calculate summary statistics
            if (!trends.isEmpty()) {
                double avgQueue = trends.stream()
                        .mapToDouble(QueueTrendDTO::getAverageQueueLength)
                        .average()
                        .orElse(0.0);

                double maxQueue = trends.stream()
                        .mapToDouble(QueueTrendDTO::getMaxQueueLength)
                        .max()
                        .orElse(0.0);

                double minQueue = trends.stream()
                        .mapToDouble(QueueTrendDTO::getMinQueueLength)
                        .min()
                        .orElse(0.0);

                Map<String, Object> statistics = new HashMap<>();
                statistics.put("averageQueueLength", avgQueue);
                statistics.put("maxQueueLength", maxQueue);
                statistics.put("minQueueLength", minQueue);
                statistics.put("trend", calculateTrend(trends));

                response.put("statistics", statistics);
            }

            log.info("Successfully fetched {} queue trend data points for device: {}",
                    trends.size(), deviceId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching queue trends for device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching queue trends: " + e.getMessage()));
        }
    }



    /**
     * Get queue comparison between multiple devices
     * GET /api/queue-analytics/comparison
     *
     * Query params:
     * - deviceIds: Comma-separated device IDs (required)
     * - startTime: ISO datetime (optional)
     * - endTime: ISO datetime (optional)
     */
    @GetMapping("/comparison")
    public ResponseEntity<?> getQueueComparison(
            @RequestParam("deviceIds") String deviceIds,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            log.info("Fetching queue comparison for devices: {}", deviceIds);

            // Validate and parse device IDs
            if (deviceIds == null || deviceIds.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device IDs cannot be null or empty"));
            }

            String[] deviceIdArray = deviceIds.split(",");
            if (deviceIdArray.length < 2) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("At least 2 device IDs required for comparison"));
            }
            if (deviceIdArray.length > 10) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Maximum 10 devices can be compared at once"));
            }

            // Set default time range
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusHours(24);
            }

            // Get comparison data
            List<QueueComparisonDTO> comparison = queueAnalyticsService.getQueueComparison(
                    deviceIdArray, startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceIds", deviceIdArray);
            response.put("deviceCount", deviceIdArray.length);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("comparison", comparison);

            // Find best and worst performing devices
            if (!comparison.isEmpty()) {
                QueueComparisonDTO bestDevice = comparison.stream()
                        .min((a, b) -> Double.compare(a.getAverageQueueLength(), b.getAverageQueueLength()))
                        .orElse(null);

                QueueComparisonDTO worstDevice = comparison.stream()
                        .max((a, b) -> Double.compare(a.getAverageQueueLength(), b.getAverageQueueLength()))
                        .orElse(null);

                Map<String, Object> insights = new HashMap<>();
                if (bestDevice != null) {
                    insights.put("bestPerforming", Map.of(
                            "deviceId", bestDevice.getDeviceId(),
                            "averageQueue", bestDevice.getAverageQueueLength()
                    ));
                }
                if (worstDevice != null) {
                    insights.put("worstPerforming", Map.of(
                            "deviceId", worstDevice.getDeviceId(),
                            "averageQueue", worstDevice.getAverageQueueLength()
                    ));
                }
                response.put("insights", insights);
            }

            log.info("Successfully fetched queue comparison for {} devices", deviceIdArray.length);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching queue comparison: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching queue comparison: " + e.getMessage()));
        }
    }

    /**
     * Get average queue data for a location/segment
     * GET /api/queue-analytics/average
     *
     * Query params:
     * - location: Location name (optional)
     * - segment: Segment name (optional)
     * - startTime: ISO datetime (optional)
     * - endTime: ISO datetime (optional)
     * - groupBy: hour, day, week, month (optional, defaults to hour)
     */
    @GetMapping("/average")
    public ResponseEntity<?> getAverageQueue(
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "segment", required = false) String segment,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "groupBy", defaultValue = "hour") String groupBy) {

        try {
            log.info("Fetching average queue - location: {}, segment: {}, groupBy: {}",
                    location, segment, groupBy);

            // At least one filter must be provided
            if ((location == null || location.trim().isEmpty()) &&
                    (segment == null || segment.trim().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Either location or segment must be provided"));
            }

            // Set default time range
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            if (startTime == null) {
                startTime = endTime.minusDays(7); // Default to last 7 days
            }

            // Get average queue data
            List<AverageQueueDTO> averageData = queueAnalyticsService.getAverageQueue(
                    location, segment, startTime, endTime, groupBy);

            Map<String, Object> response = new HashMap<>();
            response.put("location", location);
            response.put("segment", segment);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("groupBy", groupBy);
            response.put("dataPoints", averageData.size());
            response.put("averageData", averageData);

            // Calculate overall statistics
            if (!averageData.isEmpty()) {
                double overallAvg = averageData.stream()
                        .mapToDouble(AverageQueueDTO::getAverageQueueLength)
                        .average()
                        .orElse(0.0);

                int totalDevices = averageData.stream()
                        .mapToInt(AverageQueueDTO::getDeviceCount)
                        .sum();

                Map<String, Object> summary = new HashMap<>();
                summary.put("overallAverageQueue", overallAvg);
                summary.put("totalDevices", totalDevices);
                summary.put("peakPeriod", findPeakPeriod(averageData));
                summary.put("lowPeriod", findLowPeriod(averageData));

                response.put("summary", summary);
            }

            log.info("Successfully fetched {} average queue data points", averageData.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching average queue: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching average queue: " + e.getMessage()));
        }
    }

    /**
     * Get queue trends by time of day (hourly pattern)
     * GET /api/queue-analytics/device/{deviceId}/hourly-pattern
     *
     * Query params:
     * - days: Number of days to analyze (default: 7)
     */
    @GetMapping("/device/{deviceId}/hourly-pattern")
    public ResponseEntity<?> getHourlyPattern(
            @PathVariable("deviceId") String deviceId,
            @RequestParam(value = "days", defaultValue = "7") int days) {

        try {
            log.info("Fetching hourly pattern for device: {}, days: {}", deviceId, days);

            if (deviceId == null || deviceId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device ID cannot be null or empty"));
            }

            if (days < 1 || days > 90) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Days must be between 1 and 90"));
            }

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // Get hourly pattern data
            List<Map<String, Object>> hourlyPattern = queueAnalyticsService.getHourlyPattern(
                    deviceId, startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("analyzedDays", days);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("hourlyPattern", hourlyPattern);

            // Find peak hours
            if (!hourlyPattern.isEmpty()) {
                List<Integer> peakHours = queueAnalyticsService.findPeakHours(hourlyPattern);
                List<Integer> lowHours = queueAnalyticsService.findLowHours(hourlyPattern);

                response.put("peakHours", peakHours);
                response.put("lowHours", lowHours);
            }

            log.info("Successfully fetched hourly pattern for device: {}", deviceId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching hourly pattern for device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching hourly pattern: " + e.getMessage()));
        }
    }

    /**
     * Get queue statistics summary
     * GET /api/queue-analytics/device/{deviceId}/statistics
     *
     * Query params:
     * - period: today, yesterday, last7days, last30days (default: today)
     */
    @GetMapping("/device/{deviceId}/statistics")
    public ResponseEntity<?> getQueueStatistics(
            @PathVariable("deviceId") String deviceId,
            @RequestParam(value = "period", defaultValue = "today") String period) {

        try {
            log.info("Fetching queue statistics for device: {}, period: {}", deviceId, period);

            if (deviceId == null || deviceId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Device ID cannot be null or empty"));
            }

            // Calculate time range based on period
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime;

            switch (period.toLowerCase()) {
                case "today":
                    startTime = endTime.toLocalDate().atStartOfDay();
                    break;
                case "yesterday":
                    startTime = endTime.minusDays(1).toLocalDate().atStartOfDay();
                    endTime = endTime.toLocalDate().atStartOfDay();
                    break;
                case "last7days":
                    startTime = endTime.minusDays(7);
                    break;
                case "last30days":
                    startTime = endTime.minusDays(30);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Invalid period. Use: today, yesterday, last7days, last30days"));
            }

            // Get statistics
            Map<String, Object> statistics = queueAnalyticsService.getQueueStatistics(
                    deviceId, startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("deviceId", deviceId);
            response.put("period", period);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("statistics", statistics);

            log.info("Successfully fetched queue statistics for device: {}", deviceId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching queue statistics for device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching queue statistics: " + e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private String calculateTrend(List<QueueTrendDTO> trends) {
        if (trends.size() < 2) {
            return "insufficient_data";
        }

        double firstHalf = trends.stream()
                .limit(trends.size() / 2)
                .mapToDouble(QueueTrendDTO::getAverageQueueLength)
                .average()
                .orElse(0.0);

        double secondHalf = trends.stream()
                .skip(trends.size() / 2)
                .mapToDouble(QueueTrendDTO::getAverageQueueLength)
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

    private Map<String, Object> findPeakPeriod(List<AverageQueueDTO> data) {
        return data.stream()
                .max((a, b) -> Double.compare(a.getAverageQueueLength(), b.getAverageQueueLength()))
                .map(peak -> {
                    Map<String, Object> peakMap = new HashMap<>();
                    peakMap.put("timestamp", peak.getTimestamp());
                    peakMap.put("averageQueue", peak.getAverageQueueLength());
                    return peakMap;
                })
                .orElse(null);
    }

    private Map<String, Object> findLowPeriod(List<AverageQueueDTO> data) {
        return data.stream()
                .min((a, b) -> Double.compare(a.getAverageQueueLength(), b.getAverageQueueLength()))
                .map(low -> {
                    Map<String, Object> lowMap = new HashMap<>();
                    lowMap.put("timestamp", low.getTimestamp());
                    lowMap.put("averageQueue", low.getAverageQueueLength());
                    return lowMap;
                })
                .orElse(null);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", "error");
        return errorResponse;
    }
}