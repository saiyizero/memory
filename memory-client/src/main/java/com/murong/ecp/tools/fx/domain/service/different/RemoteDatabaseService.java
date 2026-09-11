package com.murong.ecp.tools.fx.domain.service.different;

import com.fasterxml.jackson.core.type.TypeReference;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.rpc.RemoteTableRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
public class RemoteDatabaseService {

    private final MemoryHttpClient memoryHttpClient;

    public RemoteDatabaseService(MemoryHttpClient memoryHttpClient) {
        this.memoryHttpClient = memoryHttpClient;
    }

    public TableEntity getRemoteTableStructure(String tableName, String compareEnv) {
        RemoteTableRequest request = new RemoteTableRequest();
        request.setTableName(tableName);
        request.setCompareEnv(compareEnv);
        return memoryHttpClient.post("/api/remote/table/structure", request, TableEntity.class);
    }

    public Map<String, TableEntity> batchGetRemoteTableStructure(List<String> tableNames, String compareEnv,
                                                               BiConsumer<Double, String> progressCallback) {
        if (progressCallback != null) {
            progressCallback.accept(0.0, "正在请求远程表结构...");
        }
        Map<String, TableEntity> result = batchGetRemoteTableStructure(tableNames, compareEnv);
        if (progressCallback != null) {
            progressCallback.accept(1.0, "远程表结构获取完成");
        }
        return result;
    }

    public Map<String, TableEntity> batchGetRemoteTableStructure(List<String> tableNames, String compareEnv) {
        RemoteTableRequest request = new RemoteTableRequest();
        request.setTableNames(tableNames);
        request.setCompareEnv(compareEnv);
        Map<String, TableEntity> result = memoryHttpClient.post("/api/remote/table/structure/batch", request,
                new TypeReference<Map<String, TableEntity>>() {
                });
        return result == null ? Collections.emptyMap() : result;
    }

    public void closeConnection(String compareEnv) {
        RemoteTableRequest request = new RemoteTableRequest();
        request.setCompareEnv(compareEnv);
        memoryHttpClient.post("/api/remote/connection/close", request, String.class);
    }

    public void closeAllConnections() {
        memoryHttpClient.post("/api/remote/connection/closeAll", Collections.emptyMap(), String.class);
    }
}
