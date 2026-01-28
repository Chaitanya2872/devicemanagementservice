package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyPatternDTO {
    private Integer hour; // 0-23
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;
    private Integer dataPoints;
    private String dayOfWeek; // Optional: Monday, Tuesday, etc.
}