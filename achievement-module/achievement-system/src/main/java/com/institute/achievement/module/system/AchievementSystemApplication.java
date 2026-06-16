package com.institute.achievement.module.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Achievement Management System.
 * <p>
 * Scans all com.institute.achievement packages for components,
 * enabling auto-configuration for the entire multi-module project.
 */
@SpringBootApplication(scanBasePackages = "com.institute.achievement")
public class AchievementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AchievementSystemApplication.class, args);
    }
}
