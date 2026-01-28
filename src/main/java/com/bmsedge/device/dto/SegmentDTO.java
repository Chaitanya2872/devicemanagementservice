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
public class SegmentDTO {

    private Long id;

    @NotBlank(message = "Segment code is required")
    private String segmentCode;

    @NotBlank(message = "Segment name is required")
    private String segmentName;

    private String description;
    private String category;
    private String businessUnit;
    private String department;
    private Boolean active;
}