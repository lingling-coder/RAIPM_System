package com.institute.achievement.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the achievement management system.
 * <p>
 * Configures CORS for development environment (allows frontend dev server)
 * and multipart upload limits.
 */
@Configuration
public class WebMvcConfig {

    /**
     * CORS configuration for development.
     * Allows the Vite dev server on localhost:5173 to access the API.
     * In production, CORS should be handled by Nginx reverse proxy.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
