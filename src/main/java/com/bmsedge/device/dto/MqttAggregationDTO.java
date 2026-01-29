package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttAggregationDTO {
    private String counterName;
    private Long totalCount;
    private Integer peakQueue;
    private Double peakWaitTime;
    private LocalDateTime periodStart;
    private Double congestionIndex;
    private PeakCongestion peakCongestion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakCongestion {
        private String level;
        private Integer weight;
        private Double peakWaitTimeInBlock;
        private LocalDateTime start;
        private LocalDateTime end;
        private Integer durationMinutes;
    }
}