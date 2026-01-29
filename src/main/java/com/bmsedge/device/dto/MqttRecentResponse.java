package com.bmsedge.device.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MqttRecentResponse {

    private List<MqttRecentDTO> data;
    private Integer count;
    private LocalDateTime timestamp;
}
