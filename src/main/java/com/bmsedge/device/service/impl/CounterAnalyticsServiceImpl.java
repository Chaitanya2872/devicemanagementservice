//package com.bmsedge.device.service.impl;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.*;
//
//import com.bmsedge.device.client.MqttAggregationClient;
//import com.bmsedge.device.repository.CounterRepository;
//import com.bmsedge.device.repository.CounterSessionAnalyticsRepository;
//import com.bmsedge.device.repository.DeviceRepository;
//import com.bmsedge.device.service.CounterAnalyticsService;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//
//@Service
//public class CounterAnalyticsServiceImpl extends CounterAnalyticsService {
//
//    private final MqttDataRepository mqttDataRepository;
//
//    private static final LocalTime BREAKFAST_START = LocalTime.of(6, 55);
//    private static final LocalTime BREAKFAST_END   = LocalTime.of(11, 55);
//
//    private static final LocalTime LUNCH_START     = LocalTime.of(11, 55);
//    private static final LocalTime LUNCH_END       = LocalTime.of(15, 25);
//
//    private static final LocalTime SNACKS_START    = LocalTime.of(15, 25);
//    private static final LocalTime SNACKS_END      = LocalTime.of(19, 0);
//
//    public CounterAnalyticsServiceImpl(DeviceRepository deviceRepository, CounterRepository counterRepository, RestTemplate restTemplate, CounterSessionAnalyticsRepository repository, MqttAggregationClient mqttAggregationClient) {
//        super(deviceRepository, counterRepository, restTemplate, repository, mqttAggregationClient);
//    }
//
//    // repositories injected here
//    private List<MqttAggregateData> getHourlyDataForCounter(
//            Long counterId,
//            LocalDate date
//    ) {
//        return mqttDataRepository.findHourlyByCounter(counterId, date);
//    }
//
//
//
//}
