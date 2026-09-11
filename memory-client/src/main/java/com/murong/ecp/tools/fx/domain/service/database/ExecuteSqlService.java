package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.ExecuteSqlRequest;
import org.springframework.stereotype.Service;

@Service
public class ExecuteSqlService {

    private final MemoryHttpClient memoryHttpClient;

    public ExecuteSqlService(MemoryHttpClient memoryHttpClient) {
        this.memoryHttpClient = memoryHttpClient;
    }

    public CrResult<Object> executeSql(String sql, String envName) {
        ExecuteSqlRequest request = new ExecuteSqlRequest();
        request.setSql(sql);
        request.setEnvName(envName);
        return memoryHttpClient.post("/api/sql/execute", request, CrResult.class);
    }

    public CrResult<Object> registerSql(String sql, String envName) {
        ExecuteSqlRequest request = new ExecuteSqlRequest();
        request.setSql(sql);
        request.setEnvName(envName);
        return memoryHttpClient.post("/api/sql/register", request, CrResult.class);
    }

    public CrResult<Object> deleteExeSqlRecord(ExeSqlRecordPO po) {
        return memoryHttpClient.post("/api/sql/deleteRecord", po, CrResult.class);
    }
}
