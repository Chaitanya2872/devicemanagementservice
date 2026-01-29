package com.bmsedge.device.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "counter_session_analytics",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"counter_code", "period_type", "period_start"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CounterSessionAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_code", nullable = false)
    private String counterCode;

    @Column(name = "period_type", nullable = false)
    private String periodType; // HOURLY / DAILY

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "total_count", nullable = false)
    private Long totalCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /* factory */
    public static CounterSessionAnalytics hourly(
            String counterCode,
            LocalDateTime hour,
            long totalCount
    ) {
        CounterSessionAnalytics a = new CounterSessionAnalytics();
        a.counterCode = counterCode;
        a.periodType = "HOURLY";
        a.periodStart = hour;
        a.totalCount = totalCount;
        return a;
    }
}

