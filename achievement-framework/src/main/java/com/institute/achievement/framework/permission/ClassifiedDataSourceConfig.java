package com.institute.achievement.framework.permission;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DataSource configuration for both the primary (achievement_db) and
 * classified (achievement_classified) MySQL schemas.
 *
 * The primary DataSource is explicitly defined with @Primary so that
 * MyBatis-Plus, Spring Security, and all default auto-configuration
 * use achievement_db.  Without an explicit @Primary, Spring Boot's
 * DataSourceAutoConfiguration backs off when it sees *any* DataSource
 * bean — and the classifiedDataSource would become the sole DataSource,
 * routing ALL queries to the empty achievement_classified schema.
 *
 * The classified schema is a separate schema within the same MySQL
 * instance, providing schema-level isolation for classified achievement
 * data (D-39).  Actual classified data tables will be created in a
 * future phase.
 *
 * Usage:
 * {@code @Qualifier("classifiedDataSource") DataSource classifiedDataSource}
 */
@Configuration
public class ClassifiedDataSourceConfig {

    /**
     * Primary data source bean — achievement_db.
     * Uses standard spring.datasource.* properties (url, username, password, etc.).
     * Marked @Primary so that all default consumers (MyBatis-Plus, Security, Actuator)
     * pick this one instead of the classified data source.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    /**
     * Classified data source bean — achievement_classified.
     * Configured via spring.datasource.classified.* properties in application-dev.yml.
     * Not @Primary — only injected explicitly via @Qualifier.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.classified")
    public DataSource classifiedDataSource() {
        return new HikariDataSource();
    }
}
