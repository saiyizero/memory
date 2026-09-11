package com.murong.ecp.tools.fx.infrastructure.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 客户端远程表访问门面：把原 JDBC 调用转发到 memory-service，不直连远程库。
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
        return queryOneBySql(sql, getGenericType(), new Object[0]);
    }

    public <E> E queryOneBySql(String sql, Class<E> clazz) {
        return queryOneBySql(sql, clazz, new Object[0]);
    }

    public <E> E queryOneBySql(String sql, Class<E> clazz, Object... params) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setSql(sql);
        request.setEntityType(clazz.getName());
        request.setResultType(clazz.getName());
        request.setParams(params == null ? Collections.emptyList() : Arrays.asList(params));
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

    /**
     * 调用 memory-service 上同名 DAO 方法，SQL 只在服务端执行。
     */
    protected <R> R invoke(String methodName, Class<R> returnType, Object... args) {
        RpcDaoResponse response = doInvoke(methodName, args);
        JsonNode data = response.getData();
        if (data == null || data.isNull() || returnType == Void.class || returnType == void.class) {
            return null;
        }
        return objectMapper.convertValue(data, returnType);
    }

    protected <R> List<R> invokeList(String methodName, Class<R> elementType, Object... args) {
        return readList(doInvoke(methodName, args), elementType);
    }

    protected void invokeVoid(String methodName, Object... args) {
        doInvoke(methodName, args);
    }

    private RpcDaoResponse doInvoke(String methodName, Object... args) {
        RpcDaoRequest request = new RpcDaoRequest();
        request.setDaoType(AopUtils.getTargetClass(this).getName());
        request.setMethodName(methodName);
        request.setMethodArgs(objectMapper.valueToTree(args == null ? Collections.emptyList() : Arrays.asList(args)));
        return memoryHttpClient.postDao("/api/dao/invoke", request);
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
            Class<?> clazz = AopUtils.getTargetClass(this);
            Type genericSuperclass = clazz.getGenericSuperclass();
            while (!(genericSuperclass instanceof ParameterizedType) && clazz.getSuperclass() != null) {
                clazz = clazz.getSuperclass();
                genericSuperclass = clazz.getGenericSuperclass();
            }
            if (genericSuperclass instanceof ParameterizedType parameterizedType) {
                return (Class<T>) parameterizedType.getActualTypeArguments()[0];
            }
            throw new RuntimeException("无法获取泛型类型");
        } catch (Exception e) {
            throw new RuntimeException("获取泛型类型失败: " + e.getMessage());
        }
    }
}
