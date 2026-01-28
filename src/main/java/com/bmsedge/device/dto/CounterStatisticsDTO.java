package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Counter Statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterStatisticsDTO {

    // Cumulative statistics (total across all devices)
    private Double totalCumulativeAverage;
    private Double totalCumulativeMax;
    private Double totalCumulativeMin;

    // Per-device averages
    private Double averageQueueLengthPerDevice;
    private Double maxQueueLengthAcrossDevices;
    private Double minQueueLengthAcrossDevices;

    // Efficiency and performance
    private Double efficiency;  // (1 - avg/max) * 100
    private Double utilization; // avg/max * 100

    // Data quality
    private Long totalReadings;
    private Integer activeDevices;

    // Trends
    private String trend;  // "increasing", "decreasing", "stable"
    private Double trendPercentage;
}