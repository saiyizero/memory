package com.murong.ecp.tools.fx.web;

import com.murong.ecp.tools.fx.domain.service.database.DataMigrationService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.DataMigrateRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrate")
public class DataMigrateController {

    private final DataMigrationService dataMigrationService;

    public DataMigrateController(DataMigrationService dataMigrationService) {
        this.dataMigrationService = dataMigrationService;
    }

    @PostMapping
    public CrResult<String> migrate(@RequestBody DataMigrateRequest request) {
        return dataMigrationService.migrateWithProgress(
                request.getSource(), request.getTarget(), request.getTableList(), null);
    }
}
