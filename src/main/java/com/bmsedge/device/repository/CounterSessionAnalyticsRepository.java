package com.bmsedge.device.repository;

import com.bmsedge.device.model.CounterSessionAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CounterSessionAnalyticsRepository
        extends JpaRepository<CounterSessionAnalytics, Long> {

    boolean existsByCounterCodeAndPeriodTypeAndPeriodStart(
            String counterCode,
            String periodType,
            LocalDateTime periodStart
    );

    @Query("""
        SELECT c
        FROM CounterSessionAnalytics c
        WHERE c.counterCode = :counterCode
          AND c.periodType = :periodType
          AND c.periodStart BETWEEN :from AND :to
        ORDER BY c.periodStart
    """)
    List<CounterSessionAnalytics> findTrends(
            String counterCode,
            String periodType,
            LocalDateTime from,
            LocalDateTime to
    );


}

