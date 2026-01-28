package com.bmsedge.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterDTO {

    private Long id;

    @NotBlank(message = "Counter code is required")
    private String counterCode;

    @NotBlank(message = "Counter name is required")
    private String counterName;

    private String description;
    private String counterType;
    private String measurementUnit;
    private Long currentValue;
    private Long maxValue;
    private Long minValue;
    private Boolean active;
}

