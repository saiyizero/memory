package com.murong.ecp.tools.fx.infrastructure.config;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
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

    @Bean
    @Primary
    @Qualifier("localDataSource")
    public DataSource localDataSource() throws IOException {
        Path dbFile = resolvePersistentDatabaseFile();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }

    private Path resolvePersistentDatabaseFile() throws IOException {
        Path homeDir = Path.of(System.getProperty("user.home"), ".memory");
        Files.createDirectories(homeDir);
        Path dbFile = homeDir.resolve("memory_embedded.db");
        if (!Files.exists(dbFile)) {
            ClassPathResource resource = new ClassPathResource("database/memory_embedded.db");
            try (var inputStream = resource.getInputStream()) {
                Files.copy(inputStream, dbFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dbFile;
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
