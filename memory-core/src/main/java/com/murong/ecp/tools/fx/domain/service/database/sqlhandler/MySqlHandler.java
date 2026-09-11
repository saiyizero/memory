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

@Service("mySqlHandler")
public class MySqlHandler extends DataBaseHandler {

    /**
     * 生成建表语句
     */
    @Override
    public String generateCreateTabSql(TableEntity entity) {
        // TODO: 可用FreeMarker模板实现，暂留空
        return null;
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
                    if("varchar".equals(dbType)) {
                        rxField.setDefaultValue("' '");
                    }
                    if("numeric".equals(dbType)||"integer".equals(dbType)) {
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

    /**
     * 支持多条SQL串（建表+注释+索引）
     */
    @Override
    public TableEntity handlerCreateTable(String sqlAll) {
        sqlAll = sqlAll.replaceAll("\r\n", "\n");
        sqlAll = sqlAll.replaceAll("--[^\n]*", "");
        TableEntity entity = new TableEntity();
        // 1. 表名
        Pattern tableNamePattern = Pattern.compile("create table [`]?([a-zA-Z0-9_]+)[`]?");
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
        Pattern fieldPattern = Pattern.compile("create table [`]?[a-zA-Z0-9_]+[`]?\\s*\\((.*?)\\)\\s*(ENGINE|COMMENT|;|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher fieldMatcher = fieldPattern.matcher(sqlAll);
        List<RxField> rxFields = new ArrayList<>();
        if (fieldMatcher.find()) {
            String fieldsBlock = fieldMatcher.group(1);
            String[] lines = fieldsBlock.split(",\n|,\r\n|,\r");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.toLowerCase().startsWith("primary key") || line.toLowerCase().startsWith("unique key") || line.toLowerCase().startsWith("key") || line.toLowerCase().startsWith("constraint")) {
                    continue;
                }
                Pattern colPattern = Pattern.compile("[`]?([a-zA-Z0-9_]+)[`]?\\s+([a-zA-Z0-9_]+)(\\(([^)]+)\\))?", Pattern.CASE_INSENSITIVE);
                Matcher colMatcher = colPattern.matcher(line);
                if (colMatcher.find()) {
                    String name = colMatcher.group(1);
                    String dbType = colMatcher.group(2);
                    String length = colMatcher.group(4);
                    RxField rxField = new RxField();
                    rxField.setNameSnake(name);
                    rxField.setNameCamel(MrStringUtils.toCamel(name));
                    rxField.setDbTyp(dbType);
                    
                    // 对于text类型，不设置长度，避免对比差异
                    String upperDbType = dbType.toUpperCase();
                    if ("TEXT".equals(upperDbType) || "LONGTEXT".equals(upperDbType) || "MEDIUMTEXT".equals(upperDbType) || "TINYTEXT".equals(upperDbType)) {
                        // text类型不设置长度
                        rxField.setLength(null);
                    } else if (length != null) {
                        String[] lenArr = length.split(",");
                        try { rxField.setLength(Integer.parseInt(lenArr[0].trim())); } catch (Exception ignore) {}
                    }
                    
                    rxField.setType(toJavaType(dbType, rxField.getLength()));
                    Pattern commentPattern = Pattern.compile("comment ['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
                    Matcher commentMatcher = commentPattern.matcher(line);
                    if (commentMatcher.find()) {
                        String comment = commentMatcher.group(1);
                        String[] parts = comment.split("\\|");
                        rxField.setCommentCn(parts[0]);
                        if (parts.length > 1) rxField.setCommentEn(parts[1]);
                    }
                    rxFields.add(rxField);
                }
            }
        }
        // 3. 解析comment on column ... is ...
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
        entity.setRxFields(rxFields);
        // 4. 主键
        Pattern pkPattern = Pattern.compile("primary key \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher pkMatcher = pkPattern.matcher(sqlAll);
        if (pkMatcher.find()) {
            TableEntity.Index pk = new TableEntity.Index();
            String tableName = entity.getTableNameSnake();
            pk.setName("pk_" + tableName);
            pk.setType("PRIMARY");
            String[] pkFields = pkMatcher.group(1).replace("`", "").split(",");
            List<String> pkList = new ArrayList<>();
            for (String pkf : pkFields) pkList.add(pkf.trim());
            pk.setFields(pkList);
            entity.setPrimaryKey(pk);
        }
        // 5. 索引
        List<TableEntity.Index> indexes = new ArrayList<>();
        Pattern idxPattern = Pattern.compile(
            "create\\s+(unique\\s+)?index\\s+[`]?([a-zA-Z0-9_]+)[`]?\\s+on\\s+[`]?([a-zA-Z0-9_]+)[`]?\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE);
        Matcher idxMatcher = idxPattern.matcher(sqlAll);
        while (idxMatcher.find()) {
            TableEntity.Index idx = new TableEntity.Index();
            idx.setName(idxMatcher.group(2));
            idx.setType(idxMatcher.group(1) != null ? "UNIQUE" : "INDEX");
            String[] idxFields = idxMatcher.group(4).replace("`", "").split(",");
            List<String> idxList = new ArrayList<>();
            for (String f : idxFields) idxList.add(f.trim());
            idx.setFields(idxList);
            indexes.add(idx);
        }
        entity.setIndexes(indexes);
        // 6. 表注释
        Pattern tableCommentPattern = Pattern.compile("comment=['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher tableCommentMatcher = tableCommentPattern.matcher(sqlAll);
        if (tableCommentMatcher.find()) {
            String comment = tableCommentMatcher.group(1);
            String[] parts = comment.split("\\|");
            entity.setTableCommentCn(parts[0]);
            if (parts.length > 1) entity.setTableCommentEn(parts[1]);
        } else {
            entity.setTableCommentCn(entity.getTableNameCamel());
        }
        return entity;
    }


    @Override
    public String toJavaType(String dbType,Integer length) {
        switch (dbType) {
            case "varchar":
                return "String";
            case "numeric":
            case "bigint":
            case "long":
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
                return "varchar";
            case "BigDecimal":
                return "numeric";
            case "Integer":
            case "int":
                return "integer";
            case "Long":
            case "long":
                return "numeric";
            case "YGAmt":
                return "numeric";
            default:
                return null;
        }
    }

    @Override
    public CrResult<String> dropAfterCreateTabSql(TableEntity entity) {
        // TODO: MySQL实现可参考MuDbSqlHandler
        return null;
    }

    @Override
    protected CrResult<String> backupTabSql(TableEntity entity, String columnsLst) {
        // TODO: MySQL实现可参考MuDbSqlHandler
        return null;
    }

    @Override
    @SneakyThrows
    public boolean ifHasData(String tableName) {
        Connection conn = super.dbConfig.getEnsureConnection();
        String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    @Override
    public List<String> getAllTableNameLst() {
        return List.of();
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
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableNme, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
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
