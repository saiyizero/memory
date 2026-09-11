package com.murong.ecp.tools.fx.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javafx.beans.property.Property;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ServiceConfiguration {

    @Value("${memory.datasource.driver-class-name}")
    private String driverClassName;
    @Value("${memory.datasource.url}")
    private String jdbcUrl;
    @Value("${memory.datasource.username}")
    private String username;
    @Value("${memory.datasource.password:}")
    private String password;
    @Value("${memory.datasource.schema:}")
    private String schema;
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;
    @Value("${spring.datasource.hikari.pool-name:MemoryServicePool}")
    private String poolName;

    @Bean(name = "businessDataSource")
    @Primary
    public DataSource businessDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        if (StringUtils.isNotBlank(username)) {
            config.setUsername(username);
        }
        if (StringUtils.isNotBlank(password)) {
            config.setPassword(password);
        }
        String driverName = driverClassName == null ? "" : driverClassName.toLowerCase();
        boolean sqlite = driverName.contains("sqlite") || (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlite:"));
        if (sqlite) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionTestQuery("SELECT 1");
        } else {
            config.setMaximumPoolSize(maximumPoolSize);
            config.setMinimumIdle(minimumIdle);
            if (driverName.contains("oracle")) {
                config.setConnectionTestQuery("SELECT 1 FROM DUAL");
            } else {
                config.setConnectionTestQuery("SELECT 1");
            }
        }
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setPoolName(poolName);
        config.setAutoCommit(true);
        if (StringUtils.isNotBlank(schema) && (driverName.contains("postgres") || (jdbcUrl != null && jdbcUrl.contains("postgresql")))) {
            config.setConnectionInitSql("SET search_path TO " + schema);
        }
        return new HikariDataSource(config);
    }

    @Bean(name = "businessJdbcTemplate")
    public JdbcTemplate businessJdbcTemplate() {
        return new JdbcTemplate(businessDataSource());
    }

    @Bean
    @Lazy
    public GlobalProperties globalProperties() {
        return new GlobalProperties(null, null, null);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .mixIn(Property.class, IgnoreJavaFxPropertyMixin.class);
    }

    @JsonIgnoreType
    private abstract static class IgnoreJavaFxPropertyMixin {
    }
}
