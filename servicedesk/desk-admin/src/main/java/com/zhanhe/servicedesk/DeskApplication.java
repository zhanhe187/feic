package com.zhanhe.servicedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * Application entry point.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class DeskApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(DeskApplication.class, args);
        System.out.println("Service Desk started successfully.");
    }
}
