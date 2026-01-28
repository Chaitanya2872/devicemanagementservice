package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Device Queue Summary within a Counter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceQueueSummaryDTO {

    private String deviceId;
    private String deviceName;

    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;

    private Long totalReadings;
    private Double contributionPercentage;  // % of total counter queue

    private String status;  // "active", "inactive", "error"
}