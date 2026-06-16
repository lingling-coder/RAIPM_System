package com.institute.achievement.framework.permission;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Classified data schema DataSource configuration (D-39).
 * Configures a secondary DataSource for the achievement_classified MySQL schema.
 *
 * The classified schema is a separate MySQL schema within the same MySQL instance,
 * providing schema-level isolation for classified achievement data.
 * Actual classified data tables will be created in Phase 1.
 *
 * Usage:
 * {@code @Qualifier("classifiedDataSource") DataSource classifiedDataSource}
 */
@Configuration
public class ClassifiedDataSourceConfig {

    /**
     * Classified data source bean.
     * Configured via spring.datasource.classified.* properties in application-dev.yml.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.classified")
    public DataSource classifiedDataSource() {
        return new HikariDataSource();
    }
}
