package com.bmsedge.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Device Management Application
 * Main Spring Boot Application Class
 */
@SpringBootApplication(scanBasePackages = "com.bmsedge.device")
@EnableJpaRepositories(basePackages = "com.bmsedge.device.repository")
@EntityScan(basePackages = "com.bmsedge.device.model")
public class DeviceManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceManagementApplication.class, args);
        System.out.println("Jack's devicehub  Started");
    }
}