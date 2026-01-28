package com.bmsedge.device.repository;

import com.bmsedge.device.model.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Long> {

    Optional<Segment> findBySegmentCode(String segmentCode);

    List<Segment> findByActive(Boolean active);

    List<Segment> findByCategory(String category);

    List<Segment> findByBusinessUnit(String businessUnit);
}

