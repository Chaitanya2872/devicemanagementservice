package com.bmsedge.device.controller;



import com.bmsedge.device.dto.SegmentDTO;
import com.bmsedge.device.service.SegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
@Slf4j
public class SegmentController {

    private final SegmentService segmentService;

    @PostMapping
    public ResponseEntity<SegmentDTO> createSegment(@Valid @RequestBody SegmentDTO segmentDTO) {
        log.info("REST request to create segment: {}", segmentDTO.getSegmentCode());
        SegmentDTO result = segmentService.createSegment(segmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SegmentDTO> getSegment(@PathVariable Long id) {
        log.info("REST request to get segment: {}", id);
        SegmentDTO segment = segmentService.getSegmentById(id);
        return ResponseEntity.ok(segment);
    }

    @GetMapping
    public ResponseEntity<List<SegmentDTO>> getAllSegments() {
        log.info("REST request to get all segments");
        List<SegmentDTO> segments = segmentService.getAllSegments();
        return ResponseEntity.ok(segments);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SegmentDTO> updateSegment(@PathVariable Long id, @Valid @RequestBody SegmentDTO segmentDTO) {
        log.info("REST request to update segment: {}", id);
        SegmentDTO result = segmentService.updateSegment(id, segmentDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Long id) {
        log.info("REST request to delete segment: {}", id);
        segmentService.deleteSegment(id);
        return ResponseEntity.noContent().build();
    }
}
