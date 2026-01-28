package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Queue Comparison DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueComparisonDTO {
    private String deviceId;
    private String deviceName;
    private String location;
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;
    private Integer totalReadings;
    private LocalDateTime firstReading;
    private LocalDateTime lastReading;
}