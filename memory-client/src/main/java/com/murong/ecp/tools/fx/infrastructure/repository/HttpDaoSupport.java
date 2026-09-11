package com.murong.ecp.tools.fx.infrastructure.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 将原 DaoSupport 的 JDBC 调用转发到 memory-service。
 */
public class HttpDaoSupport<T> {

    @Autowired
    private MemoryHttpClient memoryHttpClient;

    @Autowired
    private ObjectMapper objectMapper;

    public int updateBySql(String sql) {
        return updateBySql(sql, new Object[0]);
    }

    public int updateBySql(String sql, Object... params) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setParams(params == null ? Collections.emptyList() : Arrays.asList(params));
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/updateBySql", request);
        Integer count = memoryHttpClient.readData(response, Integer.class);
        return count == null ? 0 : count;
    }

    public T queryOneBySql(String sql) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setEntityType(getGenericType().getName());
        request.setResultType(getGenericType().getName());
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryOneBySql", request);
        return memoryHttpClient.readData(response, getGenericType());
    }

    public <E> E queryOneBySql(String sql, Class<E> clazz) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setEntityType(clazz.getName());
        request.setResultType(clazz.getName());
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryOneBySql", request);
        return memoryHttpClient.readData(response, clazz);
    }

    public <E> List<E> queryListBySql(String sql, Class<E> clazz) {
        return queryListBySql(sql, clazz, new Object[0]);
    }

    public <E> List<E> queryListBySql(String sql, Class<E> clazz, Object... params) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setEntityType(clazz.getName());
        request.setResultType(clazz.getName());
        request.setParams(params == null ? Collections.emptyList() : Arrays.asList(params));
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryListBySql", request);
        return readList(response, clazz);
    }

    public T queryOne(T entity) {
        RpcDaoRequest request = entityRequest(entity);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryOne", request);
        return memoryHttpClient.readData(response, getGenericType());
    }

    public List<T> queryForList(T entity, String orderBy) {
        RpcDaoRequest request = entityRequest(entity);
        request.setOrderBy(orderBy);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryForList", request);
        return readList(response, getGenericType());
    }

    public List<T> queryForList(T entity) {
        return queryForList(entity, null);
    }

    public void insert(T entity) {
        memoryHttpClient.postDao("/api/dao/insert", entityRequest(entity));
    }

    public void delete(T entity) {
        memoryHttpClient.postDao("/api/dao/delete", entityRequest(entity));
    }

    public void updateByOne(T updateEntity, T whereEntity) {
        RpcDaoRequest request = entityRequest(updateEntity);
        request.setWhereEntity(objectMapper.valueToTree(whereEntity));
        memoryHttpClient.postDao("/api/dao/update", request);
    }

    public Integer queryCount(String sql, Object... params) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setParams(params == null ? Collections.emptyList() : Arrays.asList(params));
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryCount", request);
        Integer count = memoryHttpClient.readData(response, Integer.class);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> queryForMaps(String sql, Object... params) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setParams(params == null ? Collections.emptyList() : Arrays.asList(params));
        RpcDaoResponse response = memoryHttpClient.postDao("/api/dao/queryForMaps", request);
        List<Map<String, Object>> list = memoryHttpClient.readData(response, new TypeReference<List<Map<String, Object>>>() {
        });
        return list == null ? Collections.emptyList() : list;
    }

    private RpcDaoRequest entityRequest(T entity) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setEntityType(entity.getClass().getName());
        request.setEntity(objectMapper.valueToTree(entity));
        return request;
    }

    @SuppressWarnings("unchecked")
    private <E> List<E> readList(RpcDaoResponse response, Class<E> clazz) {
        JsonNode data = response.getData();
        if (data == null || data.isNull()) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(data, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    @SuppressWarnings("unchecked")
    private Class<T> getGenericType() {
        try {
            Type genericSuperclass = this.getClass().getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                return (Class<T>) parameterizedType.getActualTypeArguments()[0];
            }
            throw new RuntimeException("无法获取泛型类型");
        } catch (Exception e) {
            throw new RuntimeException("获取泛型类型失败: " + e.getMessage());
        }
    }
}
