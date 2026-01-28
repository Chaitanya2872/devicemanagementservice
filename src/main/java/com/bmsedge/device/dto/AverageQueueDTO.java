package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageQueueDTO {
    private LocalDateTime timestamp;
    private String location;
    private String segment;
    private Double averageQueueLength;
    private Double maxQueueLength;
    private Double minQueueLength;
    private Integer deviceCount;
    private Integer totalReadings;
}