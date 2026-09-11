package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.DataMigrateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataMigrationService {

    private final MemoryHttpClient memoryHttpClient;

    public DataMigrationService(MemoryHttpClient memoryHttpClient) {
        this.memoryHttpClient = memoryHttpClient;
    }

    public CrResult<String> migrateWithProgress(DbConnectionPO source, DbConnectionPO target, List<String> tableList,
                                               ProgressCallback progressCallback) {
        if (progressCallback != null) {
            progressCallback.onProgress(0, 0, 0, "正在提交迁移任务...");
        }
        DataMigrateRequest request = new DataMigrateRequest();
        request.setSource(source);
        request.setTarget(target);
        request.setTableList(tableList);
        CrResult<String> result = memoryHttpClient.post("/api/migrate", request, CrResult.class);
        if (progressCallback != null) {
            progressCallback.onProgress(1, 1, 100, "迁移完成");
        }
        return result;
    }

    public interface ProgressCallback {
        void onProgress(int currentRows, int totalRows, double progress);

        default void onProgress(int currentRows, int totalRows, double progress, String tableName) {
            onProgress(currentRows, totalRows, progress);
        }
    }

    public interface TableProgressCallback {
        void onProgress(int currentRows, int totalRows, double progress, String tableName);
    }
}
