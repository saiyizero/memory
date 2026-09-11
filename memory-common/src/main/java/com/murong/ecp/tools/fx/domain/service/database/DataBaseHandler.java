package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;

import java.util.List;

/**
 * 数据库方言处理接口。
 * memory-service 提供 JDBC 实现，memory-client 通过 HTTP 调用。
 */
public interface DataBaseHandler {

    void setDbConfig(DbConfig dbConfig);

    String getSchema();

    CrResult<String> dropAfterCreateTabSql(TableEntity entity);

    CrResult<String> backupTabSql(TableEntity entity, CommonClassPO commonClassPO);

    String generateCreateTabSql(TableEntity entity);

    TableEntity handlerCreateTable(String sqlAll);

    TableEntity adjustTableEntity(TableEntity entity);

    String toJavaType(String dbType, Integer length);

    String toDbType(String javaType);

    boolean isTableExist(String tableNme);

    boolean ifHasData(String tableName);

    List<String> getAllTableNameLst();

    Integer executeSql(String sql);

    TableEntity getTableEntityFromDatabase(String tableNme);
}
