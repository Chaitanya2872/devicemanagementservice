package com.bmsedge.device.repository;

import com.bmsedge.device.model.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterRepository extends JpaRepository<Counter, Long> {

    Optional<Counter> findByCounterCode(String counterCode);

    List<Counter> findByActive(Boolean active);

    List<Counter> findByCounterType(String counterType);

    List<Counter> findByCounterNameContainingIgnoreCase(String counterName);

    boolean existsByCounterCode(String counterCode);
}