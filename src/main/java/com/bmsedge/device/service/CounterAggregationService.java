package com.bmsedge.device.service;

import com.bmsedge.device.client.MqttAggregationClient;
import com.bmsedge.device.dto.MqttRecentDTO;
import com.bmsedge.device.model.CounterSessionAnalytics;
import com.bmsedge.device.model.PeriodType;
import com.bmsedge.device.repository.CounterSessionAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterAggregationService {

    private final CounterSessionAnalyticsRepository repository;
    private final MqttAggregationClient mqttAggregationClient;

    public void aggregateHourly() {

        log.info("⏳ Starting hourly counter aggregation");

        List<MqttRecentDTO> rows = mqttAggregationClient.fetchRecent();

        if (rows.isEmpty()) {
            log.info("ℹ️ No MQTT data found");
            return;
        }

        for (MqttRecentDTO dto : rows) {

            long totalCount = Optional
                    .ofNullable(dto.getInCount())
                    .orElse(0L);

            if (totalCount <= 0 || dto.getPeriodStart() == null) {
                log.warn("⚠️ Skipping invalid record: {}", dto);
                continue;
            }

            CounterSessionAnalytics entity = new CounterSessionAnalytics();
            entity.setCounterCode(dto.getCounterCode());
            entity.setPeriodType(PeriodType.HOURLY.name());
            entity.setPeriodStart(dto.getPeriodStart());
            entity.setTotalCount(totalCount);
            entity.setCreatedAt(LocalDateTime.now());

            repository.save(entity);

            log.info(
                    "✅ Aggregated counter={} count={} periodStart={}",
                    dto.getCounterCode(),
                    totalCount,
                    dto.getPeriodStart()
            );
        }

        log.info("🎉 Hourly aggregation completed");
    }
}
