package com.trackit.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.trackit.business.config")
public class BusinessLogicServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessLogicServiceApplication.class, args);
    }
}
