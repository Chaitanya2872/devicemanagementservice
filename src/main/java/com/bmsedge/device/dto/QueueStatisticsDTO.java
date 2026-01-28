package com.bmsedge.device.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatisticsDTO {
    private String deviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;
    private Double medianQueueLength;
    private Double standardDeviation;
    private Integer totalReadings;
    private Integer peakHour;
    private Integer lowHour;
    private String trend; // increasing, decreasing, stable
}
