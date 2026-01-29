package com.bmsedge.device.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class MqttRecentDTO {

    private String counterCode;
    private Long inCount;
    private LocalDateTime timestamp;

    /**
     * Derived field for hourly aggregation
     */
    public LocalDateTime getPeriodStart() {
        if (timestamp == null) {
            return null;
        }
        return timestamp.truncatedTo(ChronoUnit.HOURS);
    }
}
