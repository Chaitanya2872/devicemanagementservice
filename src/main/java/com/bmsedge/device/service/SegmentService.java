package com.bmsedge.device.service;

import com.bmsedge.device.dto.SegmentDTO;
import com.bmsedge.device.mapper.SegmentMapper;
import com.bmsedge.device.model.Segment;
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
public class SegmentService {

    private final SegmentRepository segmentRepository;
    private final SegmentMapper segmentMapper;

    public SegmentDTO createSegment(SegmentDTO segmentDTO) {
        log.info("Creating segment: {}", segmentDTO.getSegmentCode());
        Segment segment = segmentMapper.toEntity(segmentDTO);
        Segment savedSegment = segmentRepository.save(segment);
        return segmentMapper.toDTO(savedSegment);
    }

    @Transactional(readOnly = true)
    public SegmentDTO getSegmentById(Long id) {
        log.info("Fetching segment with ID: {}", id);
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Segment not found with ID: " + id));
        return segmentMapper.toDTO(segment);
    }

    @Transactional(readOnly = true)
    public List<SegmentDTO> getAllSegments() {
        log.info("Fetching all segments");
        return segmentRepository.findAll().stream()
                .map(segmentMapper::toDTO)
                .collect(Collectors.toList());
    }

    public SegmentDTO updateSegment(Long id, SegmentDTO segmentDTO) {
        log.info("Updating segment with ID: {}", id);
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Segment not found with ID: " + id));
        segmentMapper.updateEntityFromDTO(segmentDTO, segment);
        Segment updatedSegment = segmentRepository.save(segment);
        return segmentMapper.toDTO(updatedSegment);
    }

    public void deleteSegment(Long id) {
        log.info("Deleting segment with ID: {}", id);
        segmentRepository.deleteById(id);
    }
}
