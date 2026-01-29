package com.bmsedge.device.client;

import com.bmsedge.device.dto.MqttRecentDTO;
import com.bmsedge.device.dto.MqttRecentResponse;
import com.bmsedge.device.dto.MqttAggregationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Component
public class MqttAggregationClient {

    @Value("${mqtt.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch recent MQTT data (existing method)
     */
    public List<MqttRecentDTO> fetchRecent() {
        String url = baseUrl + "/api/mqtt-data/recent";

        try {
            MqttRecentResponse response =
                    restTemplate.getForObject(url, MqttRecentResponse.class);

            if (response == null || response.getData() == null) {
                return List.of();
            }

            return response.getData();

        } catch (Exception ex) {
            log.error("❌ Failed to fetch MQTT recent data", ex);
            return List.of();
        }
    }

    /**
     * Fetch hourly aggregation for a specific date
     */
    public List<MqttAggregationDTO> fetchHourlyAggregation(LocalDate date) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/mqtt-data/aggregate/hourly")
                .queryParam("date", date.format(DateTimeFormatter.ISO_DATE))
                .toUriString();

        try {
            log.info("🔍 Fetching hourly aggregation for date: {}", date);

            ResponseEntity<List<MqttAggregationDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<MqttAggregationDTO>>() {}
            );

            if (response.getBody() == null) {
                log.warn("⚠️ No data returned for date: {}", date);
                return List.of();
            }

            log.info("✅ Successfully fetched {} records for date: {}",
                    response.getBody().size(), date);
            return response.getBody();

        } catch (Exception ex) {
            log.error("❌ Failed to fetch hourly aggregation for date: {}", date, ex);
            return List.of();
        }
    }

    public List<MqttAggregationDTO> fetchHourlyAggregation(
            LocalDateTime from,
            LocalDateTime to
    ) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/api/mqtt-data/aggregate/hourly")
                .queryParam("from", from)
                .queryParam("to", to)
                .toUriString();

        try {
            log.info("🔍 Fetching MQTT aggregation from {} to {}", from, to);

            ResponseEntity<List<MqttAggregationDTO>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            return response.getBody() == null
                    ? List.of()
                    : response.getBody();

        } catch (Exception ex) {
            log.error("❌ Failed MQTT aggregation call", ex);
            return List.of();
        }
    }

}