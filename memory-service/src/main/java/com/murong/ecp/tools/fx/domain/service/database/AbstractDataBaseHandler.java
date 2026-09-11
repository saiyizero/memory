package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.converter.DatabaseConvert;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.TableDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

public abstract class AbstractDataBaseHandler implements DataBaseHandler {
    @Autowired
    private TableDataDao tableDataDao;

    protected DbConfig dbConfig;

    @Override
    public void setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public String getSchema(){
        return this.dbConfig.getSchema();
    }

    @Override
    public CrResult<String> backupTabSql(TableEntity entity, CommonClassPO commonClassPO) {
        TableDataPO reqPO = new TableDataPO();
        reqPO.setGroupName(entity.getGroupName());
        reqPO.setProjectName(entity.getProjectName());
        reqPO.setAppName(entity.getAppName());
        reqPO.setTableNameSnake(entity.getTableNameSnake());
        TableDataPO rspPO = tableDataDao.queryOne(reqPO);
        StringBuilder sqlBuilder = new StringBuilder();
        TableEntity orgEntity = DatabaseConvert.toEntity(rspPO);
        if (orgEntity == null) {
            orgEntity=entity;
        }

        orgEntity.getRxFields().forEach(field -> {
            sqlBuilder.append(field.getNameSnake()).append(",");
        });

        if(commonClassPO != null) {
            List<RxField> rxFieldList = DatabaseConvert.jsonToFields(commonClassPO.getFieldsJson());
            rxFieldList.forEach(field -> {
                sqlBuilder.append(field.getNameSnake()).append(",");
            });
        }

        if (sqlBuilder.length() > 0) {
            sqlBuilder.setLength(sqlBuilder.length() - 1);
        }
        return backupTabSql(entity, sqlBuilder.toString());
    }

    protected abstract CrResult<String> backupTabSql(TableEntity entity, String columnsLst);

    @Override
    public Integer executeSql(String sql) {
        return this.dbConfig.executeSql(sql);
    }

    public abstract List<String> getTableColumns(Connection conn, String tableName, String schema);
}
