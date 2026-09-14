package com.murong.ecp.tools.fx.infrastructure.config;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.Property;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class MemoryConfiguration {

    @Value("${memory.local.datasource.driver-class-name}")
    private String driverClassName;
    @Value("${memory.local.datasource.file}")
    private String dbFilePath;
    @Value("${memory.local.datasource.url}")
    private String jdbcUrl;
    @Value("${memory.local.datasource.init-resource:}")
    private String initResource;

    private final ResourceLoader resourceLoader;

    public MemoryConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    @Primary
    @Qualifier("localDataSource")
    public DataSource localDataSource() throws IOException {
        Path dbFile = resolvePersistentDatabaseFile();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(resolveJdbcUrl(dbFile));
        return dataSource;
    }

    private Path resolvePersistentDatabaseFile() throws IOException {
        Path dbFile = Path.of(dbFilePath).toAbsolutePath().normalize();
        Path parent = dbFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(dbFile) && StringUtils.isNotBlank(initResource)) {
            Resource resource = resourceLoader.getResource(initResource);
            try (var inputStream = resource.getInputStream()) {
                Files.copy(inputStream, dbFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dbFile;
    }

    private String resolveJdbcUrl(Path dbFile) {
        if (StringUtils.isNotBlank(jdbcUrl)) {
            return jdbcUrl;
        }
        return "jdbc:sqlite:" + dbFile;
    }

    @Bean
    @Primary
    @Qualifier("localJdbcTemplate")
    @DependsOn("localDataSource")
    public JdbcTemplate localJdbcTemplate(@Qualifier("localDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Lazy
    public GlobalProperties globalProperties() {
        return new GlobalProperties(null, null, null);
    }

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(300000);
        RestTemplate restTemplate = new RestTemplate(factory);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.addMixIn(Property.class, IgnoreJavaFxPropertyMixin.class);
        return mapper;
    }

    @JsonIgnoreType
    private abstract static class IgnoreJavaFxPropertyMixin {
    }
}
