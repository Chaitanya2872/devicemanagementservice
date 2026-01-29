package com.bmsedge.device.service;

import com.bmsedge.device.client.MqttAggregationClient;
import com.bmsedge.device.dto.MqttAggregationDTO;
import com.bmsedge.device.dto.MqttRecentDTO;
import com.bmsedge.device.model.CounterSessionAnalytics;
import com.bmsedge.device.repository.CounterSessionAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CounterAggregationJob {

    private final MqttAggregationClient mqttClient;
    private final CounterSessionAnalyticsRepository repository;

    public CounterAggregationJob(
            MqttAggregationClient mqttClient,
            CounterSessionAnalyticsRepository repository
    ) {
        this.mqttClient = mqttClient;
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void hourlyAggregation() {

        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime from = to.minusHours(1);

        // 🔴 FIX: fetchRecent(), NOT fetchHourly()
        List<MqttRecentDTO> data = mqttClient.fetchRecent();

        for (MqttRecentDTO dto : data) {

            boolean exists =
                    repository.existsByCounterCodeAndPeriodTypeAndPeriodStart(
                            dto.getCounterCode(),
                            "HOURLY",
                            from
                    );

            if (exists) continue;

            repository.save(
                    CounterSessionAnalytics.hourly(
                            dto.getCounterCode(),
                            from,
                            dto.getInCount()
                    )
            );
        }
    }
}
