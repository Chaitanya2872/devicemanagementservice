package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Counter Queue Trends API
 * Contains cumulative queue data and breakdown by device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterQueueTrendResponseDTO {

    private String counterCode;
    private String counterName;
    private String counterType;
    private Integer totalDeviceCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String interval;

    // Cumulative trend data (sum across all devices per timestamp)
    private List<CounterQueueTrendDTO> cumulativeTrends;

    // Overall statistics for the entire time range
    private CounterStatisticsDTO statistics;

    // Device breakdown (optional, for detailed analysis)
    private List<DeviceQueueSummaryDTO> deviceBreakdown;

    // Metadata
    private Integer dataPointCount;
    private String trend;  // "increasing", "decreasing", "stable"
}