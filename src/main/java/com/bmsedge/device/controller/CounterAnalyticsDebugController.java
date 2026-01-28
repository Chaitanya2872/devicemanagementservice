package com.bmsedge.device.controller;

import com.bmsedge.device.model.Counter;
import com.bmsedge.device.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug Controller for Counter Analytics
 * Use this to test counter lookup and validate data
 */
@RestController
@RequestMapping("/api/counter-analytics/debug")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CounterAnalyticsDebugController {

    private final CounterRepository counterRepository;

    /**
     * List all counters
     * GET /api/counter-analytics/debug/counters
     */
    @GetMapping("/counters")
    public ResponseEntity<?> listAllCounters() {
        try {
            log.info("Listing all counters");

            List<Counter> counters = counterRepository.findAll();

            List<Map<String, Object>> counterList = counters.stream()
                    .map(c -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", c.getId());
                        info.put("counterCode", c.getCounterCode());
                        info.put("counterName", c.getCounterName());
                        info.put("counterType", c.getCounterType());
                        info.put("active", c.getActive());
                        return info;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("totalCounters", counterList.size());
            response.put("counters", counterList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error listing counters: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if a specific counter exists
     * GET /api/counter-analytics/debug/counter/{counterCode}/exists
     */
    @GetMapping("/counter/{counterCode}/exists")
    public ResponseEntity<?> checkCounterExists(@PathVariable("counterCode") String counterCode) {
        try {
            log.info("Checking if counter exists: '{}'", counterCode);
            log.info("Counter code length: {}", counterCode.length());
            log.info("Counter code bytes: {}", counterCode.getBytes());

            // Trim any whitespace
            String trimmedCode = counterCode.trim();

            Counter counter = counterRepository.findByCounterCode(trimmedCode)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("requestedCode", counterCode);
            response.put("trimmedCode", trimmedCode);
            response.put("exists", counter != null);

            if (counter != null) {
                response.put("counter", Map.of(
                        "id", counter.getId(),
                        "counterCode", counter.getCounterCode(),
                        "counterName", counter.getCounterName(),
                        "counterType", counter.getCounterType(),
                        "active", counter.getActive()
                ));
            } else {
                // List similar counters
                List<Counter> allCounters = counterRepository.findAll();
                List<String> allCodes = allCounters.stream()
                        .map(Counter::getCounterCode)
                        .toList();
                response.put("availableCounterCodes", allCodes);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking counter: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Echo test to check URL encoding
     * GET /api/counter-analytics/debug/echo/{value}
     */
    @GetMapping("/echo/{value}")
    public ResponseEntity<?> echoTest(@PathVariable("value") String value) {
        log.info("Echo test received: '{}'", value);
        log.info("Value length: {}", value.length());

        Map<String, Object> response = new HashMap<>();
        response.put("received", value);
        response.put("length", value.length());
        response.put("trimmed", value.trim());
        response.put("trimmedLength", value.trim().length());

        // Show each character
        char[] chars = value.toCharArray();
        StringBuilder charInfo = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            charInfo.append(String.format("char[%d]='%c' (code=%d) ", i, chars[i], (int)chars[i]));
        }
        response.put("characters", charInfo.toString());

        return ResponseEntity.ok(response);
    }
}