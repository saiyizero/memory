package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceRpcService {

    private final MemoryHttpClient memoryHttpClient;

    public WorkspaceRpcService(MemoryHttpClient memoryHttpClient) {
        this.memoryHttpClient = memoryHttpClient;
    }

    public WorkspaceBootstrapVO bootstrap() {
        return readResult(memoryHttpClient.post("/api/workspace/bootstrap", null,
                new TypeReference<CrResult<WorkspaceBootstrapVO>>() {
                }));
    }

    public WorkspaceBootstrapVO switchGroup(String groupName) {
        WorkspaceSwitchGroupRequest request = new WorkspaceSwitchGroupRequest();
        request.setGroupName(groupName);
        return readResult(memoryHttpClient.post("/api/workspace/switch-group", request,
                new TypeReference<CrResult<WorkspaceBootstrapVO>>() {
                }));
    }

    private WorkspaceBootstrapVO readResult(CrResult<WorkspaceBootstrapVO> result) {
        if (result == null) {
            throw new RuntimeException("memory-service 无响应");
        }
        if (!result.isSucess() || result.getData() == null) {
            throw new RuntimeException(StringUtils.defaultIfBlank(result.getMsgInf(), "workspace 接口调用失败"));
        }
        return result.getData();
    }
}
