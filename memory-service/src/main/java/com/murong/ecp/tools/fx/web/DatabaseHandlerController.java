package com.murong.ecp.tools.fx.web;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.database.DataBaseHandler;
import com.murong.ecp.tools.fx.infrastructure.rpc.HandlerInvokeRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/handler")
public class DatabaseHandlerController {

    private final GlobalProperties globalProperties;
    private final ObjectMapper objectMapper;

    public DatabaseHandlerController(GlobalProperties globalProperties, ObjectMapper objectMapper) {
        this.globalProperties = globalProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/executeSql")
    public RpcDaoResponse executeSql(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().executeSql(request.getSql())));
    }

    @PostMapping("/isTableExist")
    public RpcDaoResponse isTableExist(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().isTableExist(request.getTableName())));
    }

    @PostMapping("/ifHasData")
    public RpcDaoResponse ifHasData(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().ifHasData(request.getTableName())));
    }

    @PostMapping("/getAllTableNameLst")
    public RpcDaoResponse getAllTableNameLst() {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().getAllTableNameLst()));
    }

    @PostMapping("/getTableEntityFromDatabase")
    public RpcDaoResponse getTableEntityFromDatabase(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().getTableEntityFromDatabase(request.getTableName())));
    }

    @PostMapping("/generateCreateTabSql")
    public RpcDaoResponse generateCreateTabSql(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().generateCreateTabSql(request.getTableEntity())));
    }

    @PostMapping("/dropAfterCreateTabSql")
    public RpcDaoResponse dropAfterCreateTabSql(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().dropAfterCreateTabSql(request.getTableEntity())));
    }

    @PostMapping("/backupTabSql")
    public RpcDaoResponse backupTabSql(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().backupTabSql(request.getTableEntity(), request.getCommonClassPO())));
    }

    @PostMapping("/handlerCreateTable")
    public RpcDaoResponse handlerCreateTable(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().handlerCreateTable(request.getSql())));
    }

    @PostMapping("/adjustTableEntity")
    public RpcDaoResponse adjustTableEntity(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().adjustTableEntity(request.getTableEntity())));
    }

    @PostMapping("/toJavaType")
    public RpcDaoResponse toJavaType(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().toJavaType(request.getDbType(), request.getLength())));
    }

    @PostMapping("/toDbType")
    public RpcDaoResponse toDbType(@RequestBody HandlerInvokeRequest request) {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().toDbType(request.getJavaType())));
    }

    @PostMapping("/getSchema")
    public RpcDaoResponse getSchema() {
        return RpcDaoResponse.ok(objectMapper.valueToTree(handler().getSchema()));
    }

    private DataBaseHandler handler() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && ctx.getDbConfig() == null && ctx.getGroupName() != null && ctx.getProjectName() != null) {
            com.murong.ecp.tools.fx.infrastructure.repository.dao.DbConnectionDao dao =
                    com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder.getBean(
                            com.murong.ecp.tools.fx.infrastructure.repository.dao.DbConnectionDao.class);
            com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO req =
                    new com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO();
            req.setGroupName(ctx.getGroupName());
            req.setProjectName(ctx.getProjectName());
            req.setMainFlg("1");
            com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO main = dao.queryOne(req);
            if (main != null) {
                ctx.setDbConfig(new com.murong.ecp.tools.fx.domain.entity.DbConfig(main));
            }
        }
        return globalProperties.getInputDataBaseHandler();
    }
}
