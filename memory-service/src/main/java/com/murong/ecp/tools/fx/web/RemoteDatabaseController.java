package com.murong.ecp.tools.fx.web;

import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.service.different.RemoteDatabaseService;
import com.murong.ecp.tools.fx.infrastructure.rpc.RemoteTableRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/remote")
public class RemoteDatabaseController {

    private final RemoteDatabaseService remoteDatabaseService;

    public RemoteDatabaseController(RemoteDatabaseService remoteDatabaseService) {
        this.remoteDatabaseService = remoteDatabaseService;
    }

    @PostMapping("/table/structure")
    public TableEntity getRemoteTableStructure(@RequestBody RemoteTableRequest request) {
        return remoteDatabaseService.getRemoteTableStructure(request.getTableName(), request.getCompareEnv());
    }

    @PostMapping("/table/structure/batch")
    public Map<String, TableEntity> batchGetRemoteTableStructure(@RequestBody RemoteTableRequest request) {
        return remoteDatabaseService.batchGetRemoteTableStructure(request.getTableNames(), request.getCompareEnv());
    }

    @PostMapping("/connection/close")
    public void closeConnection(@RequestBody RemoteTableRequest request) {
        remoteDatabaseService.closeConnection(request.getCompareEnv());
    }

    @PostMapping("/connection/closeAll")
    public void closeAllConnections() {
        remoteDatabaseService.closeAllConnections();
    }
}
