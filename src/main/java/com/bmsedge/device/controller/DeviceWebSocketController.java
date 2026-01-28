package com.bmsedge.device.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket Controller for real-time device MQTT data streaming
 *
 * Usage:
 * 1. Client connects to: ws://localhost:8080/ws/device-data
 * 2. Client subscribes to: /topic/device/{deviceId}
 * 3. Server pushes data to subscribed clients
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DeviceWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends a subscription request for specific device
     * Client sends to: /app/device/subscribe
     * Server broadcasts to: /topic/device/{deviceId}
     */
    @MessageMapping("/device/subscribe")
    @SendTo("/topic/devices")
    public Map<String, Object> subscribeToDevice(Map<String, String> request) {
        String deviceId = request.get("deviceId");
        log.info("Client subscribed to device: {}", deviceId);

        return Map.of(
                "status", "subscribed",
                "deviceId", deviceId,
                "message", "Successfully subscribed to device updates"
        );
    }

    /**
     * Broadcast MQTT data to specific device subscribers
     * This method should be called when new MQTT data arrives
     */
    public void broadcastDeviceData(String deviceId, Map<String, Object> mqttData) {
        String destination = "/topic/device/" + deviceId;
        log.debug("Broadcasting MQTT data to {}: {}", destination, mqttData);
        messagingTemplate.convertAndSend(destination, mqttData);
    }

    /**
     * Broadcast MQTT data to counter subscribers
     */
    public void broadcastCounterData(String counterName, Map<String, Object> mqttData) {
        String destination = "/topic/counter/" + counterName;
        log.debug("Broadcasting MQTT data to {}: {}", destination, mqttData);
        messagingTemplate.convertAndSend(destination, mqttData);
    }

    /**
     * Broadcast to all devices
     */
    public void broadcastToAll(Map<String, Object> mqttData) {
        log.debug("Broadcasting MQTT data to all subscribers: {}", mqttData);
        messagingTemplate.convertAndSend("/topic/devices/all", mqttData);
    }
}