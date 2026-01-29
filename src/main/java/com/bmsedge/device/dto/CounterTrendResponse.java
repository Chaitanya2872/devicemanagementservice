package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CounterTrendResponse {

    private LocalDateTime periodStart;
    private Long totalCount;
}
