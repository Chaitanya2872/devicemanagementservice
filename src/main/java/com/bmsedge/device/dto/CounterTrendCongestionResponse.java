package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CounterTrendCongestionResponse {

    private LocalDateTime timestamp;

    // Existing trend metric (footfall / peakQueue / peakWaitTime)
    private Long value;

    // 🔥 Congestion (optional but present)
    private String congestionLevel;
    private Integer congestionWeight;
    private Long congestionDurationMinutes;
    private Double peakWaitTime;
}
