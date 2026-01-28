package com.bmsedge.device.controller;


import com.bmsedge.device.dto.LocationDTO;
import com.bmsedge.device.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationDTO> createLocation(@Valid @RequestBody LocationDTO locationDTO) {
        log.info("REST request to create location: {}", locationDTO.getLocationCode());
        LocationDTO result = locationService.createLocation(locationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocation(@PathVariable Long id) {
        log.info("REST request to get location: {}", id);
        LocationDTO location = locationService.getLocationById(id);
        return ResponseEntity.ok(location);
    }

    @GetMapping
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        log.info("REST request to get all locations");
        List<LocationDTO> locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationDTO> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDTO locationDTO) {
        log.info("REST request to update location: {}", id);
        LocationDTO result = locationService.updateLocation(id, locationDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        log.info("REST request to delete location: {}", id);
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
