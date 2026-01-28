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
public class LocationDTO {

    private Long id;

    @NotBlank(message = "Location code is required")
    private String locationCode;

    @NotBlank(message = "Location name is required")
    private String locationName;

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String description;
    private Boolean active;
}

