package com.murong.ecp.tools.fx.application;

import com.murong.ecp.tools.fx.domain.service.database.ExecuteSqlService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.ExecuteSqlRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql")
public class ExecuteSqlController {

    private final ExecuteSqlService executeSqlService;

    public ExecuteSqlController(ExecuteSqlService executeSqlService) {
        this.executeSqlService = executeSqlService;
    }

    @PostMapping("/execute")
    public CrResult<Object> execute(@RequestBody ExecuteSqlRequest request) {
        return executeSqlService.executeSql(request.getSql(), request.getEnvName());
    }

    @PostMapping("/register")
    public CrResult<Object> register(@RequestBody ExecuteSqlRequest request) {
        return executeSqlService.registerSql(request.getSql(), request.getEnvName());
    }

    @PostMapping("/deleteRecord")
    public CrResult<Object> deleteRecord(@RequestBody ExeSqlRecordPO po) {
        return executeSqlService.deleteExeSqlRecord(po);
    }
}
