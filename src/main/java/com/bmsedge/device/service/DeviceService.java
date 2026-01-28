package com.bmsedge.device.service;

import com.bmsedge.device.dto.DeviceDTO;
import com.bmsedge.device.mapper.DeviceMapper;
import com.bmsedge.device.model.Counter;
import com.bmsedge.device.model.Device;
import com.bmsedge.device.model.Location;
import com.bmsedge.device.model.Segment;
import com.bmsedge.device.repository.CounterRepository;
import com.bmsedge.device.repository.DeviceRepository;
import com.bmsedge.device.repository.LocationRepository;
import com.bmsedge.device.repository.SegmentRepository;
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
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final LocationRepository locationRepository;
    private final SegmentRepository segmentRepository;
    private final CounterRepository counterRepository;
    private final DeviceMapper deviceMapper;

    public DeviceDTO createDevice(DeviceDTO deviceDTO) {
        log.info("Creating device with ID: {}", deviceDTO.getDeviceId());

        // Validate input
        if (deviceDTO == null) {
            throw new IllegalArgumentException("Device data cannot be null");
        }

        Device device = deviceMapper.toEntity(deviceDTO);

        Location location = locationRepository.findByLocationCode(deviceDTO.getLocation())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + deviceDTO.getLocation()));
        device.setLocation(location);

        Segment segment = segmentRepository.findBySegmentCode(deviceDTO.getSegment())
                .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + deviceDTO.getSegment()));
        device.setSegment(segment);

        Counter counter = counterRepository.findByCounterCode(deviceDTO.getCounterName())
                .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + deviceDTO.getCounterName()));
        device.setCounter(counter);

        Device savedDevice = deviceRepository.save(device);
        log.info("Device created successfully with ID: {}", savedDevice.getId());

        return deviceMapper.toDTO(savedDevice);
    }

    @Transactional(readOnly = true)
    public DeviceDTO getDeviceById(Long id) {
        log.info("Fetching device with ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));
        return deviceMapper.toDTO(device);
    }

    @Transactional(readOnly = true)
    public DeviceDTO getDeviceByDeviceId(String deviceId) {
        log.info("Fetching device with device ID: {}", deviceId);

        // Add null and empty string checks
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Device ID is null or empty");
            return null;
        }

        try {
            return deviceRepository.findByDeviceId(deviceId.trim())
                    .map(deviceMapper::toDTO)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching device by device ID: {}", deviceId, e);
            throw new RuntimeException("Error fetching device: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getAllDevices() {
        log.info("Fetching all devices");
        try {
            return deviceRepository.findAll().stream()
                    .map(deviceMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all devices", e);
            throw new RuntimeException("Error fetching devices: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getDevicesByLocation(String locationCode) {
        log.info("Fetching devices by location: {}", locationCode);

        if (locationCode == null || locationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Location code cannot be null or empty");
        }

        try {
            return deviceRepository.findByLocationCode(locationCode).stream()
                    .map(deviceMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching devices by location: {}", locationCode, e);
            throw new RuntimeException("Error fetching devices by location: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getDevicesBySegment(String segmentCode) {
        log.info("Fetching devices by segment: {}", segmentCode);

        if (segmentCode == null || segmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Segment code cannot be null or empty");
        }

        try {
            return deviceRepository.findBySegmentCode(segmentCode).stream()
                    .map(deviceMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching devices by segment: {}", segmentCode, e);
            throw new RuntimeException("Error fetching devices by segment: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getDevicesByCounter(String counterCode) {
        log.info("Fetching devices by counter: {}", counterCode);

        if (counterCode == null || counterCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Counter code cannot be null or empty");
        }

        try {
            return deviceRepository.findByCounterCode(counterCode).stream()
                    .map(deviceMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching devices by counter: {}", counterCode, e);
            throw new RuntimeException("Error fetching devices by counter: " + e.getMessage(), e);
        }
    }

    public DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO) {
        log.info("Updating device with ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }

        if (deviceDTO == null) {
            throw new IllegalArgumentException("Device data cannot be null");
        }

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        deviceMapper.updateEntityFromDTO(deviceDTO, device);

        if (deviceDTO.getLocation() != null) {
            Location location = locationRepository.findByLocationCode(deviceDTO.getLocation())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found: " + deviceDTO.getLocation()));
            device.setLocation(location);
        }

        if (deviceDTO.getSegment() != null) {
            Segment segment = segmentRepository.findBySegmentCode(deviceDTO.getSegment())
                    .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + deviceDTO.getSegment()));
            device.setSegment(segment);
        }

        if (deviceDTO.getCounterName() != null) {
            Counter counter = counterRepository.findByCounterCode(deviceDTO.getCounterName())
                    .orElseThrow(() -> new IllegalArgumentException("Counter not found: " + deviceDTO.getCounterName()));
            device.setCounter(counter);
        }

        Device updatedDevice = deviceRepository.save(device);
        log.info("Device updated successfully with ID: {}", updatedDevice.getId());

        return deviceMapper.toDTO(updatedDevice);
    }

    public void deleteDevice(Long id) {
        log.info("Deleting device with ID: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));
        deviceRepository.delete(device);
        log.info("Device deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> searchDevices(String searchTerm) {
        log.info("Searching devices with term: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            log.warn("Search term is null or empty, returning all devices");
            return getAllDevices();
        }

        try {
            return deviceRepository.searchDevices(searchTerm).stream()
                    .map(deviceMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching devices with term: {}", searchTerm, e);
            throw new RuntimeException("Error searching devices: " + e.getMessage(), e);
        }
    }
}