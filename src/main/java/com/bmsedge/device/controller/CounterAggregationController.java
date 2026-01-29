package com.bmsedge.device.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.bmsedge.device.service.CounterAggregationService;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/aggregation")
public class CounterAggregationController {

    private final CounterAggregationService aggregationService;

    public CounterAggregationController(
            CounterAggregationService aggregationService
    ) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/hourly/run")
    public ResponseEntity<String> runHourlyAggregation() {
        aggregationService.aggregateHourly();
        return ResponseEntity.ok("Hourly aggregation executed");
    }
}
