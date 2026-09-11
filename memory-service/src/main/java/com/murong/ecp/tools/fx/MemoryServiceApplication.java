package com.murong.ecp.tools.fx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MemoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryServiceApplication.class, args);
    }
}
