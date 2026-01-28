package com.bmsedge.device.controller;



import com.bmsedge.device.dto.CounterDTO;
import com.bmsedge.device.service.CounterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/counters")
@RequiredArgsConstructor
@Slf4j
public class CounterController {

    private final CounterService counterService;

    @PostMapping
    public ResponseEntity<CounterDTO> createCounter(@Valid @RequestBody CounterDTO counterDTO) {
        log.info("REST request to create counter: {}", counterDTO.getCounterCode());
        CounterDTO result = counterService.createCounter(counterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CounterDTO> getCounter(@PathVariable Long id) {
        log.info("REST request to get counter: {}", id);
        CounterDTO counter = counterService.getCounterById(id);
        return ResponseEntity.ok(counter);
    }

    @GetMapping
    public ResponseEntity<List<CounterDTO>> getAllCounters() {
        log.info("REST request to get all counters");
        List<CounterDTO> counters = counterService.getAllCounters();
        return ResponseEntity.ok(counters);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CounterDTO> updateCounter(@PathVariable Long id, @Valid @RequestBody CounterDTO counterDTO) {
        log.info("REST request to update counter: {}", id);
        CounterDTO result = counterService.updateCounter(id, counterDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCounter(@PathVariable Long id) {
        log.info("REST request to delete counter: {}", id);
        counterService.deleteCounter(id);
        return ResponseEntity.noContent().build();
    }
}
