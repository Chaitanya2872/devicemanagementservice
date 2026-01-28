package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Queue Analytics DTOs
 */

// Queue Trend DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTrendDTO {
    private LocalDateTime timestamp;
    private String deviceId;
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;
    private Integer dataPoints;
    private String interval;
}