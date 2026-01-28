package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Counter Queue Trend with Cumulative Data
 * Represents aggregated queue data across all devices in a counter at a specific time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterQueueTrendDTO {

    private LocalDateTime timestamp;
    private String counterCode;
    private String counterName;

    // Cumulative queue metrics (sum of all devices in counter at this timestamp)
    private Double cumulativeQueueLength;

    // Average metrics across devices
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;

    // Device-level details
    private Integer activeDeviceCount;  // Number of devices reporting at this timestamp
    private Integer totalDeviceCount;   // Total devices in counter

    // Data quality metrics
    private Integer dataPointCount;     // Number of readings contributing to this timestamp

    private String interval;
}