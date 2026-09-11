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
import java.sql.SQLException;
import java.util.List;

public abstract class DataBaseHandler {
    @Autowired
    private TableDataDao tableDataDao;

    protected DbConfig dbConfig;

    public void setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public String getSchema(){
        return this.dbConfig.getSchema();
    }
    /**
     * 生成重新建表语句
     */
    public abstract CrResult<String> dropAfterCreateTabSql(TableEntity entity);

    /**
     * 生成备份数据后的重新建表语句
     */
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

    /**
     * 生成备份数据后的重新建表语句
     */
    protected abstract CrResult<String> backupTabSql(TableEntity entity, String columnsLst);

    /**
     * 生成建表语句
     */
    public abstract String generateCreateTabSql(TableEntity entity);

    /**
     * 支持多条SQL串（建表+注释+索引）
     */
    public abstract TableEntity handlerCreateTable(String sqlAll);

    /**
     * 调整完善TableEntity内的数据
     */
    public abstract TableEntity adjustTableEntity(TableEntity entity);

    /**
     * 数据库类型转java类型
     */
    public abstract String toJavaType(String dbType,Integer length);

    /**
     * java类型转数据库类型
     */
    public abstract String toDbType(String javaType);

    /**
     * 判断表是否存在
     */
    public abstract boolean isTableExist(String tableNme);

    /**
     * 判断表中是否有数据
     */
    public abstract boolean ifHasData(String tableName);

    /**
     * 获取当前用户下所有表名
     */
    public abstract List<String> getAllTableNameLst();

    /**
     * 执行sql
     */
    public Integer executeSql(String sql) {
        return this.dbConfig.executeSql(sql);
    }

    /**
     * 从数据库中获取表实体
     */
    public abstract TableEntity getTableEntityFromDatabase(String tableNme);

    public abstract List<String> getTableColumns(Connection conn, String tableName, String schema);

}
