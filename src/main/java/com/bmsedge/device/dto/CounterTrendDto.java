package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CounterTrendDto {

    private String period;     // MON / Week-1 / JAN
    private Long footfall;
    private Integer peakQueue;
    private Double peakWaitTime;
    private Double congestionRate;
}
