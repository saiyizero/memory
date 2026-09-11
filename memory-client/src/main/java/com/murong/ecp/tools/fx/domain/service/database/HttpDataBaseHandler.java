package com.murong.ecp.tools.fx.domain.service.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.HandlerInvokeRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service("httpDataBaseHandler")
public class HttpDataBaseHandler implements DataBaseHandler {

    private final MemoryHttpClient memoryHttpClient;
    private DbConfig dbConfig;

    public HttpDataBaseHandler(MemoryHttpClient memoryHttpClient) {
        this.memoryHttpClient = memoryHttpClient;
    }

    @Override
    public void setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public String getSchema() {
        if (dbConfig != null && dbConfig.getSchema() != null) {
            return dbConfig.getSchema();
        }
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/getSchema", new HandlerInvokeRequest());
        return memoryHttpClient.readData(response, String.class);
    }

    @Override
    public CrResult<String> dropAfterCreateTabSql(TableEntity entity) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableEntity(entity);
        return postCrResult("/api/handler/dropAfterCreateTabSql", request);
    }

    @Override
    public CrResult<String> backupTabSql(TableEntity entity, CommonClassPO commonClassPO) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableEntity(entity);
        request.setCommonClassPO(commonClassPO);
        return postCrResult("/api/handler/backupTabSql", request);
    }

    @Override
    public String generateCreateTabSql(TableEntity entity) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableEntity(entity);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/generateCreateTabSql", request);
        return memoryHttpClient.readData(response, String.class);
    }

    @Override
    public TableEntity handlerCreateTable(String sqlAll) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setSql(sqlAll);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/handlerCreateTable", request);
        return memoryHttpClient.readData(response, TableEntity.class);
    }

    @Override
    public TableEntity adjustTableEntity(TableEntity entity) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableEntity(entity);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/adjustTableEntity", request);
        return memoryHttpClient.readData(response, TableEntity.class);
    }

    @Override
    public String toJavaType(String dbType, Integer length) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setDbType(dbType);
        request.setLength(length);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/toJavaType", request);
        return memoryHttpClient.readData(response, String.class);
    }

    @Override
    public String toDbType(String javaType) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setJavaType(javaType);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/toDbType", request);
        return memoryHttpClient.readData(response, String.class);
    }

    @Override
    public boolean isTableExist(String tableNme) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableName(tableNme);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/isTableExist", request);
        Boolean exist = memoryHttpClient.readData(response, Boolean.class);
        return Boolean.TRUE.equals(exist);
    }

    @Override
    public boolean ifHasData(String tableName) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableName(tableName);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/ifHasData", request);
        Boolean hasData = memoryHttpClient.readData(response, Boolean.class);
        return Boolean.TRUE.equals(hasData);
    }

    @Override
    public List<String> getAllTableNameLst() {
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/getAllTableNameLst", new HandlerInvokeRequest());
        List<String> list = memoryHttpClient.readData(response, new TypeReference<List<String>>() {
        });
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public Integer executeSql(String sql) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setSql(sql);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/executeSql", request);
        return memoryHttpClient.readData(response, Integer.class);
    }

    @Override
    public TableEntity getTableEntityFromDatabase(String tableNme) {
        HandlerInvokeRequest request = new HandlerInvokeRequest();
        request.setTableName(tableNme);
        RpcDaoResponse response = memoryHttpClient.postDao("/api/handler/getTableEntityFromDatabase", request);
        return memoryHttpClient.readData(response, TableEntity.class);
    }

    @SuppressWarnings("unchecked")
    private CrResult<String> postCrResult(String path, HandlerInvokeRequest request) {
        RpcDaoResponse response = memoryHttpClient.postDao(path, request);
        return memoryHttpClient.readData(response, CrResult.class);
    }
}
