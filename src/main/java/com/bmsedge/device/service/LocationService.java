package com.bmsedge.device.service;



import com.bmsedge.device.dto.LocationDTO;
import com.bmsedge.device.mapper.LocationMapper;
import com.bmsedge.device.model.Location;
import com.bmsedge.device.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    public LocationDTO createLocation(LocationDTO locationDTO) {
        log.info("Creating location: {}", locationDTO.getLocationCode());
        Location location = locationMapper.toEntity(locationDTO);
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toDTO(savedLocation);
    }

    @Transactional(readOnly = true)
    public LocationDTO getLocationById(Long id) {
        log.info("Fetching location with ID: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with ID: " + id));
        return locationMapper.toDTO(location);
    }

    @Transactional(readOnly = true)
    public List<LocationDTO> getAllLocations() {
        log.info("Fetching all locations");
        return locationRepository.findAll().stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public LocationDTO updateLocation(Long id, LocationDTO locationDTO) {
        log.info("Updating location with ID: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with ID: " + id));
        locationMapper.updateEntityFromDTO(locationDTO, location);
        Location updatedLocation = locationRepository.save(location);
        return locationMapper.toDTO(updatedLocation);
    }

    public void deleteLocation(Long id) {
        log.info("Deleting location with ID: {}", id);
        locationRepository.deleteById(id);
    }
}
