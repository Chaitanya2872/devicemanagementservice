package com.bmsedge.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual device queue data within a counter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceQueueDataDTO {

    private String deviceId;
    private String deviceName;
    private LocalDateTime timestamp;
    private Double queueLength;
    private String status;
}