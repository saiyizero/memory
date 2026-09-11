package com.murong.ecp.tools.fx.domain.service.database.sqlhandler;

import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.service.database.DataBaseHandler;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("oracleSqlHandler")
public class OracleSqlHandler extends DataBaseHandler {

    @Override
    public CrResult<String> dropAfterCreateTabSql(TableEntity entity) {
        // TODO: Oracle实现可参考MuDbSqlHandler
        return null;
    }

    @Override
    protected CrResult<String> backupTabSql(TableEntity entity, String columnsLst) {
        // TODO: Oracle实现可参考MuDbSqlHandler
        return null;
    }

    @Override
    public String generateCreateTabSql(TableEntity entity) {
        // TODO: 可用FreeMarker模板实现，暂留空
        return null;
    }


    @Override
    public TableEntity handlerCreateTable(String sqlAll) {
        sqlAll = sqlAll.replaceAll("\r\n", "\n");
        sqlAll = sqlAll.replaceAll("--[^\n]*", "");
        TableEntity entity = new TableEntity();
        // 1. 表名
        Pattern tableNamePattern = Pattern.compile("create table ([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher tableNameMatcher = tableNamePattern.matcher(sqlAll);
        if (tableNameMatcher.find()) {
            String tableName = tableNameMatcher.group(1);
            entity.setTableNameSnake(tableName);
            String tableNameCamel=tableName;
            if (tableNameCamel.startsWith("t_")) {
                tableNameCamel=tableNameCamel.substring(2);
            }
            tableNameCamel= MrStringUtils.toCamel(tableNameCamel);
            tableNameCamel=MrStringUtils.toFirstLetter(tableNameCamel);
            entity.setTableNameCamel(tableNameCamel);
        }
        // 2. 字段
        Pattern fieldPattern = Pattern.compile("\\((.*?)\\)\\s*(;|$)", Pattern.DOTALL);
        Matcher fieldMatcher = fieldPattern.matcher(sqlAll);
        List<RxField> rxFields = new ArrayList<>();
        if (fieldMatcher.find()) {
            String fieldsBlock = fieldMatcher.group(1);
            Pattern linePattern = Pattern.compile("^\\s*([A-Z0-9_]+)\\s+([A-Z0-9_]+)(\\(([^)]+)\\))?.*?(not null)?", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher lineMatcher = linePattern.matcher(fieldsBlock);
            while (lineMatcher.find()) {
                String name = lineMatcher.group(1);
                String dbType = lineMatcher.group(2);
                String length = lineMatcher.group(4);
                RxField rxField = new RxField();
                rxField.setNameSnake(name);
                rxField.setNameCamel(MrStringUtils.toCamel(name));
                rxField.setDbTyp(dbType);
                if (length != null) {
                    try { rxField.setLength(Integer.parseInt(length.split(",")[0].trim())); } catch (Exception ignore) {}
                }
                rxField.setType(toJavaType(dbType, rxField.getLength()));
                rxFields.add(rxField);
            }
        }
        entity.setRxFields(rxFields);
        // 3. 主键
        Pattern pkPattern = Pattern.compile("primary key \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher pkMatcher = pkPattern.matcher(sqlAll);
        if (pkMatcher.find()) {
            TableEntity.Index pk = new TableEntity.Index();
            pk.setName("pk_" + entity.getTableNameSnake());
            pk.setType("PRIMARY");
            String[] pkFields = pkMatcher.group(1).split(",");
            List<String> pkList = new ArrayList<>();
            for (String pkf : pkFields) pkList.add(pkf.trim());
            pk.setFields(pkList);
            entity.setPrimaryKey(pk);
        }
        // 4. 索引
        List<TableEntity.Index> indexes = new ArrayList<>();
        Pattern idxPattern = Pattern.compile("create index ([A-Z0-9_]+)\\s+on\\s+[A-Z0-9_]+\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher idxMatcher = idxPattern.matcher(sqlAll);
        while (idxMatcher.find()) {
            TableEntity.Index idx = new TableEntity.Index();
            idx.setName(idxMatcher.group(1));
            String[] idxFields = idxMatcher.group(2).split(",");
            List<String> idxList = new ArrayList<>();
            for (String f : idxFields) idxList.add(f.trim());
            idx.setFields(idxList);
            indexes.add(idx);
        }
        entity.setIndexes(indexes);
        // 5. 字段注释
        Map<String, String> commentMap = new HashMap<>();
        Pattern commentPattern = Pattern.compile("comment on column ([A-Z0-9_]+)\\.([A-Z0-9_]+) is '([^']*)'", Pattern.CASE_INSENSITIVE);
        Matcher commentMatcher = commentPattern.matcher(sqlAll);
        while (commentMatcher.find()) {
            String col = commentMatcher.group(2);
            String comment = commentMatcher.group(3);
            commentMap.put(col, comment);
        }
        for (RxField f : rxFields) {
            if (commentMap.containsKey(f.getNameSnake())) {
                String comment = commentMap.get(f.getNameSnake());
                String[] parts = comment.split("\\|");
                f.setCommentCn(parts[0]);
                if (parts.length > 1) f.setCommentEn(parts[1]);
            }
        }
        // 6. 表注释（如有，可扩展）
        return entity;
    }

    @Override
    public TableEntity adjustTableEntity(TableEntity entity) {
        if(entity.getTableNameCamel() == null && entity.getTableNameSnake() != null){
            String tableNameCamel=entity.getTableNameSnake();
            if (tableNameCamel.startsWith("t_")) {
                tableNameCamel=tableNameCamel.substring(2);
            }
            tableNameCamel= MrStringUtils.toCamel(tableNameCamel);
            tableNameCamel=MrStringUtils.toFirstLetter(tableNameCamel);
            entity.setTableNameCamel(tableNameCamel);
        }
        List<RxField> rxFields = entity.getRxFields();
        if (rxFields != null) {
            for (RxField rxField : rxFields) {
                if (rxField.getType() != null && !rxField.getType().isEmpty()) {
                    String dbType = toDbType(rxField.getType());
                    rxField.setDbTyp(dbType);
                    if("varchar2".equals(dbType)) {
                        rxField.setDefaultValue("' '");
                    }
                    if("number".equals(dbType)||"integer".equals(dbType)) {
                        rxField.setDefaultValue("0");
                    }
                }else if(rxField.getDbTyp() != null && !rxField.getDbTyp().isEmpty()) {
                    String type = toJavaType(rxField.getDbTyp(), rxField.getLength());
                    rxField.setType(type);
                }
            }
        }
        return entity;
    }

    @Override
    public String toJavaType(String dbType,Integer length) {
        switch (dbType.toLowerCase()) {
            case "varchar2":
            case "varchar":
                return "String";
            case "number":
            case "bigint":
                if(length!=null && length==18){
                    return "YGAmt";
                }else {
                    return "Long";
                }
            case "int":
            case "integer":
                return "Integer";
            default:
                return null;
        }
    }

    @Override
    public String toDbType(String javaType) {
        switch (javaType) {
            case "String":
                return "varchar2";
            case "BigDecimal":
                return "number";
            case "Integer":
            case "int":
                return "integer";
            case "Long":
            case "long":
                return "number";
            case "YGAmt":
                return "number";
            default:
                return null;
        }
    }

    @Override
    @SneakyThrows
    public boolean ifHasData(String tableName) {
        Connection conn = super.dbConfig.getEnsureConnection();
        String sql = "SELECT 1 FROM " + tableName + " WHERE ROWNUM = 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    @Override
    @SneakyThrows
    public List<String> getAllTableNameLst() {
        Connection conn = super.dbConfig.getEnsureConnection();
        List<String> tableNames = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        String dbName = meta.getDatabaseProductName();
        String schema = meta.getUserName();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
        }
        return tableNames;
    }

    @Override
    public TableEntity getTableEntityFromDatabase(String tableNme) {
        return null;
    }

    @Override
    @SneakyThrows
    public boolean isTableExist(String tableNme) {
        Connection conn = super.dbConfig.getEnsureConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, meta.getUserName(), tableNme.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    @SneakyThrows
    @Override
    public List<String> getTableColumns(Connection conn, String tableName, String schema) {

        List<String> columns = new ArrayList<>();

        // 解析表名，支持schema.tableName格式
        String actualSchema = schema;
        String actualTableName = tableName;

        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            actualSchema = parts[0];
            actualTableName = parts[1];
        }

        try (ResultSet rs = conn.getMetaData().getColumns(actualSchema, null, actualTableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName != null && !columnName.trim().isEmpty()) {
                    columns.add(columnName);
                }
            }
        }

        // 如果没有找到列信息，尝试使用当前连接的默认schema
        if (columns.isEmpty() && actualSchema == null) {
            try {
                String currentSchema = conn.getSchema();
                if (currentSchema != null && !currentSchema.trim().isEmpty()) {
                    try (ResultSet rs = conn.getMetaData().getColumns(currentSchema, null, actualTableName, null)) {
                        while (rs.next()) {
                            String columnName = rs.getString("COLUMN_NAME");
                            if (columnName != null && !columnName.trim().isEmpty()) {
                                columns.add(columnName);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw e;
            }
        }

        return columns;
    }
}
