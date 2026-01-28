package com.bmsedge.device.service;



import com.bmsedge.device.dto.CounterDTO;
import com.bmsedge.device.mapper.CounterMapper;
import com.bmsedge.device.model.Counter;
import com.bmsedge.device.repository.CounterRepository;
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
public class CounterService {

    private final CounterRepository counterRepository;
    private final CounterMapper counterMapper;

    public CounterDTO createCounter(CounterDTO counterDTO) {
        log.info("Creating counter: {}", counterDTO.getCounterCode());
        Counter counter = counterMapper.toEntity(counterDTO);
        Counter savedCounter = counterRepository.save(counter);
        return counterMapper.toDTO(savedCounter);
    }

    @Transactional(readOnly = true)
    public CounterDTO getCounterById(Long id) {
        log.info("Fetching counter with ID: {}", id);
        Counter counter = counterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Counter not found with ID: " + id));
        return counterMapper.toDTO(counter);
    }

    @Transactional(readOnly = true)
    public List<CounterDTO> getAllCounters() {
        log.info("Fetching all counters");
        return counterRepository.findAll().stream()
                .map(counterMapper::toDTO)
                .collect(Collectors.toList());
    }

    public CounterDTO updateCounter(Long id, CounterDTO counterDTO) {
        log.info("Updating counter with ID: {}", id);
        Counter counter = counterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Counter not found with ID: " + id));
        counterMapper.updateEntityFromDTO(counterDTO, counter);
        Counter updatedCounter = counterRepository.save(counter);
        return counterMapper.toDTO(updatedCounter);
    }

    public void deleteCounter(Long id) {
        log.info("Deleting counter with ID: {}", id);
        counterRepository.deleteById(id);
    }
}

