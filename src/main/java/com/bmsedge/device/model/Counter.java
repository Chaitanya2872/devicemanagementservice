package com.bmsedge.device.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "counters", indexes = {
        @Index(name = "idx_counter_code", columnList = "counter_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_code", nullable = false, unique = true, length = 50)
    private String counterCode;

    @Column(name = "counter_name", nullable = false, length = 200)
    private String counterName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "counter_type", length = 100)
    private String counterType;

    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;

    @Column(name = "current_value")
    private Long currentValue;

    @Column(name = "max_value")
    private Long maxValue;

    @Column(name = "min_value")
    private Long minValue;

    @Column(name = "active")
    private Boolean active = true;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Device> devices = new HashSet<>();

    @Version
    private Long version;
}
