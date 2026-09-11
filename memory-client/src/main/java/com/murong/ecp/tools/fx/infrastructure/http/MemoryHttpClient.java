package com.murong.ecp.tools.fx.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.config.DatabaseConnectionException;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.MemoryHttpHeaders;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Component
public class MemoryHttpClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LocalSettingDao localSettingDao;
    private final GlobalProperties globalProperties;

    public MemoryHttpClient(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            LocalSettingDao localSettingDao,
                            @Lazy GlobalProperties globalProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.localSettingDao = localSettingDao;
        this.globalProperties = globalProperties;
    }

    public String getServiceBaseUrl() {
        LocalSettingPO query = new LocalSettingPO();
        query.setStatus(FlgEnum.YES.getValue());
        LocalSettingPO setting = localSettingDao.queryOne(query);
        if (setting != null && StringUtils.isNotBlank(setting.getDbUrl()) && setting.getDbUrl().startsWith("http")) {
            return StringUtils.removeEnd(setting.getDbUrl().trim(), "/");
        }
        String fileUrl = readServiceUrlFromFile();
        if (StringUtils.isNotBlank(fileUrl)) {
            return StringUtils.removeEnd(fileUrl.trim(), "/");
        }
        return "http://127.0.0.1:9090";
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(path, body, responseType, null);
    }

    public <T> T post(String path, Object body, TypeReference<T> typeReference) {
        JsonNode node = exchange(path, body, JsonNode.class, null);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, typeReference);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("解析服务响应失败: " + e.getMessage(), e);
        }
    }

    public RpcDaoResponse postDao(String path, Object body) {
        RpcDaoResponse response = post(path, body, RpcDaoResponse.class);
        if (response == null) {
            throw new RuntimeException("memory-service 无响应: " + path);
        }
        if (!response.isSuccess()) {
            throw new RuntimeException(StringUtils.defaultIfBlank(response.getMessage(), "memory-service 调用失败"));
        }
        return response;
    }

    public <T> T readData(RpcDaoResponse response, Class<T> type) {
        if (response.getData() == null || response.getData().isNull()) {
            return null;
        }
        return objectMapper.convertValue(response.getData(), type);
    }

    public <T> T readData(RpcDaoResponse response, TypeReference<T> typeReference) {
        if (response.getData() == null || response.getData().isNull()) {
            return null;
        }
        return objectMapper.convertValue(response.getData(), typeReference);
    }

    private <T> T exchange(String path, Object body, Class<T> responseType, Void ignored) {
        String url = getServiceBaseUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        fillContextHeaders(headers);
        HttpEntity<Object> entity = new HttpEntity<>(body == null ? "{}" : body, headers);
        try {
            ResponseEntity<T> response = restTemplate.postForEntity(url, entity, responseType);
            return response.getBody();
        } catch (ResourceAccessException e) {
            throw new DatabaseConnectionException("无法连接 memory-service: " + url, e);
        } catch (Exception e) {
            if (e instanceof DatabaseConnectionException) {
                throw e;
            }
            throw new RuntimeException("调用 memory-service 失败 [" + path + "]: " + e.getMessage(), e);
        }
    }

    public <T> T get(String path, Class<T> responseType) {
        String url = getServiceBaseUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        fillContextHeaders(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (ResourceAccessException e) {
            throw new DatabaseConnectionException("无法连接 memory-service: " + url, e);
        }
    }

    private void fillContextHeaders(HttpHeaders headers) {
        if (globalProperties == null) {
            return;
        }
        if (globalProperties.getOperator() != null) {
            putHeader(headers, MemoryHttpHeaders.USER_ID, globalProperties.getOperator().getUserId());
            putHeader(headers, MemoryHttpHeaders.USERNAME, globalProperties.getOperator().getUsername());
            putHeader(headers, MemoryHttpHeaders.REAL_NAME, globalProperties.getOperator().getRealname());
            putHeader(headers, MemoryHttpHeaders.ROLES, globalProperties.getOperator().getRoles());
        }
        putHeader(headers, MemoryHttpHeaders.GROUP_NAME, globalProperties.getGroupName());
        putHeader(headers, MemoryHttpHeaders.PROJECT_NAME, globalProperties.getProjectName());
        putHeader(headers, MemoryHttpHeaders.APP_NAME, globalProperties.getAppName());
    }

    private void putHeader(HttpHeaders headers, String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            headers.set(name, value);
        }
    }

    private String readServiceUrlFromFile() {
        try {
            Path configFile = Path.of(System.getProperty("user.home"), ".memory", "database.properties");
            if (!Files.exists(configFile)) {
                return null;
            }
            Properties props = new Properties();
            try (var in = Files.newInputStream(configFile)) {
                props.load(in);
            }
            String url = props.getProperty("memory.service.url");
            if (StringUtils.isBlank(url)) {
                url = props.getProperty("db.url");
            }
            if (StringUtils.isNotBlank(url) && url.startsWith("http")) {
                return url;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
