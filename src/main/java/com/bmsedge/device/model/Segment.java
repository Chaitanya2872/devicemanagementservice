package com.bmsedge.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "segments", indexes = {
        @Index(name = "idx_segment_code", columnList = "segment_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "segment_code", nullable = false, unique = true, length = 50)
    private String segmentCode;

    @Column(name = "segment_name", nullable = false, length = 200)
    private String segmentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "business_unit", length = 100)
    private String businessUnit;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Device> devices = new HashSet<>();

    @Version
    private Long version;
}
