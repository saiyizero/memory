package com.murong.ecp.tools.fx.domain.service.database.sqlhandler;

import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.service.database.AbstractDataBaseHandler;
import com.murong.ecp.tools.fx.enums.IndexTypEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.utils.BusinessUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("PostgreSQLHandler")
public class PostgresqlHandler extends AbstractDataBaseHandler {

    @Override
    public CrResult<String> dropAfterCreateTabSql(TableEntity entity) {
        final String tableName = entity.getTableNameSnake();
        final StringBuilder ddlBuilder = new StringBuilder();
        // 2. 删除旧表
        ddlBuilder.append("DROP TABLE ").append(tableName).append(";\n\n");
        // 3. 使用新的定义创建表
        ddlBuilder.append(generateCreateTabSql(entity)).append(";\n\n");
        CrResult<String> crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        crResult.setData(ddlBuilder.toString());
        return crResult;
    }

    @Override
    protected CrResult<String> backupTabSql(TableEntity entity, String columnsLst) {
        final String tableName = entity.getTableNameSnake();
        final String backupTableName = tableName + "_" + MrDateUtils.getCurrentDate();

        final StringBuilder ddlBuilder = new StringBuilder();

        // 1. 备份数据到新表
        ddlBuilder.append("CREATE TABLE ").append(backupTableName)
                .append(" AS SELECT * FROM ").append(tableName).append(";\n\n");

        // 2. 删除旧表
        ddlBuilder.append("DROP TABLE ").append(tableName).append(";\n\n");

        // 3. 使用新的定义创建表
        ddlBuilder.append(generateCreateTabSql(entity)).append(";\n\n");

        // 4. 从备份表恢复数据
        ddlBuilder.append("INSERT INTO ").append(tableName).append(" (").append(columnsLst).append(")\n")
                .append("SELECT ").append(columnsLst).append(" FROM ").append(backupTableName).append(";\n");

        CrResult<String> crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        crResult.setData(ddlBuilder.toString());
        // 提示用户执行清理操作
        crResult.setDropsql("DROP TABLE " + backupTableName + ";");
        return crResult;
    }

    /**
     * 生成建表语句
     */
    @Override
    public String generateCreateTabSql(TableEntity entity) {
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
            cfg.setClassForTemplateLoading(PostgresqlHandler.class, "/templates");
            cfg.setDefaultEncoding("UTF-8");

            Template template = cfg.getTemplate("table-ddl-postgresql.ftl");

            Map<String, String> camelToSnake = new HashMap<>();
            if (entity.getRxFields() != null) {
                for (RxField rxField : entity.getRxFields()) {
                    if (rxField.getNameCamel() != null && rxField.getNameSnake() != null) {
                        camelToSnake.put(rxField.getNameCamel(), rxField.getNameSnake());
                    }
                }
            }

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("table", entity);
            dataModel.put("camelToSnake", camelToSnake);

            StringWriter out = new StringWriter();
            template.process(dataModel, out);

            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成DDL失败", e);
        }
    }

    /**
     * 支持多条SQL串（建表+注释+索引）
     */
    @Override
    public TableEntity handlerCreateTable(String sqlAll) {
        sqlAll = sqlAll.replaceAll("\r\n", "\n");
        // 去除 -- 注释
        sqlAll = sqlAll.replaceAll("--[^\n]*", "");
        TableEntity entity = new TableEntity();
        // 1. 表名
        Pattern tableNamePattern = Pattern.compile("create table\\s+([a-zA-Z0-9_\\.\"]+)", Pattern.CASE_INSENSITIVE);
        Matcher tableNameMatcher = tableNamePattern.matcher(sqlAll);
        if (tableNameMatcher.find()) {
            String tableName = tableNameMatcher.group(1).replace("\"", "");
            if(tableName.contains(".")) {
                entity.setTableNameSnake(tableName.substring(tableName.lastIndexOf(".") + 1));
            } else {
                entity.setTableNameSnake(tableName);
            }
            String tableNameCamel=entity.getTableNameSnake();
            if (tableNameCamel.startsWith("t_")) {
                tableNameCamel=tableNameCamel.substring(2);
            }
            tableNameCamel= MrStringUtils.toCamel(tableNameCamel);
            tableNameCamel=MrStringUtils.toFirstLetter(tableNameCamel);
            entity.setTableNameCamel(tableNameCamel);
        }
        // 2. 字段
        Pattern fieldPattern = Pattern.compile("create table\\s+[a-zA-Z0-9_\\.\"]+\\s*\\((.*?)\\);", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher fieldMatcher = fieldPattern.matcher(sqlAll);
        List<RxField> rxFields = new ArrayList<>();
        if (fieldMatcher.find()) {
            String fieldsBlock = fieldMatcher.group(1);
            String[] lines = fieldsBlock.split(",\n|,\r\n"); // Split by comma and newline
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.toLowerCase().startsWith("primary key") || line.toLowerCase().startsWith("unique") || line.toLowerCase().startsWith("constraint")) {
                    continue;
                }
                // 匹配字段名、类型、长度
                Pattern colPattern = Pattern.compile("[\"`]?([a-zA-Z0-9_]+)[\"`]?\\s+([a-zA-Z0-9_]+(?:\\s*\\([^)]*\\))?(\\s*\\[\\])?)(?:\\s+default\\s+[^,]+)?(?:\\s+not\\s+null)?(?:\\s+null)?", Pattern.CASE_INSENSITIVE);
                Matcher colMatcher = colPattern.matcher(line);

                if (colMatcher.find()) {
                    String name = colMatcher.group(1);
                    String dbTypeFull = colMatcher.group(2).trim();
                    String dbType;
                    String length = null;

                    Pattern typePattern = Pattern.compile("([a-zA-Z0-9_]+)(?:\\(([^)]+)\\))?(\\[\\])?");
                    Matcher typeMatcher = typePattern.matcher(dbTypeFull);
                    if (typeMatcher.find()) {
                        dbType = typeMatcher.group(1);
                        if (typeMatcher.group(2) != null) {
                            length = typeMatcher.group(2);
                        }
                        if (typeMatcher.group(3) != null) {
                            dbType += typeMatcher.group(3); // array type
                        }
                    } else {
                        dbType = dbTypeFull;
                    }

                    RxField rxField = new RxField();
                    rxField.setNameSnake(name);
                    rxField.setNameCamel(MrStringUtils.toCamel(name));
                    rxField.setDbTyp(dbType); // 存数据库类型

                    // 对于text类型，不设置长度，避免对比差异
                    String upperDbType = dbType.toUpperCase();
                    if ("TEXT".equals(upperDbType) || "LONGTEXT".equals(upperDbType) || "MEDIUMTEXT".equals(upperDbType) || "TINYTEXT".equals(upperDbType)) {
                        // text类型不设置长度
                        rxField.setLength(null);
                    } else if (length != null) {
                        String[] lenArr = length.split(",");
                        try { rxField.setLength(Integer.parseInt(lenArr[0].trim())); } catch (Exception ignore) {}
                        // 可扩展：小数位
                    }

                    // 字段注释（如有） - handled by COMMENT ON later
                    rxField.setType(toJavaType(dbType, rxField.getLength())); // 存Java类型
                    rxFields.add(rxField);
                }
            }
        }
        entity.setRxFields(rxFields);

        // 3. 解析comment on column ... is ...
        Map<String, String> commentMap = new HashMap<>();
        Pattern commentPattern = Pattern.compile("comment on column\\s+([a-zA-Z0-9_\\.\"]+)\\s+is\\s+'([^']*)'", Pattern.CASE_INSENSITIVE);
        Matcher commentMatcher = commentPattern.matcher(sqlAll);
        while (commentMatcher.find()) {
            String fullCol = commentMatcher.group(1);
            String col;
            if (fullCol.contains(".")) {
                col = fullCol.substring(fullCol.lastIndexOf(".") + 1);
            } else {
                col = fullCol;
            }
            col = col.replace("\"", "");
            String comment = commentMatcher.group(2);
            commentMap.put(col.toLowerCase(), comment);
        }
        for (RxField f : rxFields) {
            if (commentMap.containsKey(f.getNameSnake().toLowerCase())) {
                String comment = commentMap.get(f.getNameSnake().toLowerCase());
                String[] parts = comment.split("\\|");
                f.setCommentCn(parts[0]);
                if (parts.length > 1) f.setCommentEn(parts[1]);
            }
        }

        // 4. 主键
        Pattern pkPattern = Pattern.compile("primary key\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher pkMatcher = pkPattern.matcher(sqlAll);
        if (pkMatcher.find()) {
            TableEntity.Index pk = new TableEntity.Index();
            String tableName = entity.getTableNameSnake();
            pk.setName("pk_" + tableName);
            pk.setType("PRIMARY");
            String[] pkFields = pkMatcher.group(1).replace("`", "").replace("\"", "").split(",");
            List<String> pkList = new ArrayList<>();
            for (String pkf : pkFields) pkList.add(pkf.trim());
            pk.setFields(pkList);
            entity.setPrimaryKey(pk);
        }

        // 5. 索引
        List<TableEntity.Index> indexes = new ArrayList<>();
        Pattern idxPattern = Pattern.compile(
                "create\\s+(unique\\s+)?index\\s+([a-zA-Z0-9_]+)\\s+on\\s+([a-zA-Z0-9_\\.\"]+)\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher idxMatcher = idxPattern.matcher(sqlAll);
        while (idxMatcher.find()) {
            TableEntity.Index idx = new TableEntity.Index();
            idx.setName(idxMatcher.group(2));
            idx.setType(idxMatcher.group(1) != null ? "UNIQUE" : "INDEX");
            String[] idxFields = idxMatcher.group(4).replace("`", "").replace("\"", "").split(",");
            List<String> idxList = new ArrayList<>();
            for (String f : idxFields) idxList.add(f.trim());
            idx.setFields(idxList);
            indexes.add(idx);
        }
        entity.setIndexes(indexes);

        // 6. 表注释
        Pattern tableCommentPattern = Pattern.compile("comment on table\\s+([a-zA-Z0-9_\\.\"]+)\\s+is\\s+'([^']*)'", Pattern.CASE_INSENSITIVE);
        Matcher tableCommentMatcher = tableCommentPattern.matcher(sqlAll);
        if (tableCommentMatcher.find()) {
            String comment = tableCommentMatcher.group(2);
            String[] parts = comment.split("\\|");
            entity.setTableCommentCn(parts[0]);
            if (parts.length > 1) entity.setTableCommentEn(parts[1]);
        } else {
            entity.setTableCommentCn(entity.getTableNameCamel());
        }
        return entity;
    }

    @Override
    public TableEntity adjustTableEntity(TableEntity entity) {
        if(StringUtils.isBlank(entity.getTableNameCamel()) && StringUtils.isNotBlank(entity.getTableNameSnake())){
            String tableNameCamel=entity.getTableNameSnake();
            if (tableNameCamel.startsWith("t_")) {
                tableNameCamel=tableNameCamel.substring(2);
            }
            tableNameCamel= MrStringUtils.toCamel(tableNameCamel);
            tableNameCamel=MrStringUtils.toFirstLetter(tableNameCamel);
            entity.setTableNameCamel(tableNameCamel);
        }

        // 处理表注释解析逻辑
        processTableComment(entity);

        List<RxField> rxFields = entity.getRxFields();
        if (rxFields != null) {
            for (RxField rxField : rxFields) {
                // 如果type有值，dbtype没值，那么就用type转一下
                if (StringUtils.isNotBlank(rxField.getType())) {
                    String dbType = toDbType(rxField.getType());
                    if (StringUtils.isBlank(dbType) && MrStringUtils.isJavaBasicType(rxField.getType())) {
                        throw new RuntimeException("dbType is null or empty");
                    }
                    rxField.setDbTyp(dbType);
                    if(StringUtils.equals(dbType, "varchar")) {
                        rxField.setDefaultValue("' '");
                    }
                    if(StringUtils.equals(dbType, "numeric")||StringUtils.equals(dbType, "integer")) {
                        rxField.setDefaultValue("0");
                    }
                }else if(StringUtils.isNotBlank(rxField.getDbTyp())) {
                    String type = toJavaType(rxField.getDbTyp(), rxField.getLength());
                    rxField.setType(type);
                }

                // 处理字段注释解析逻辑
                processFieldComment(rxField);

            }
        }
        return entity;
    }

    /**
     * 处理字段注释解析逻辑
     * 支持以下格式：
     * - 字段中文注释|Field English Comment
     * - Field comment;Another field comment
     * - |FIELD COMMENT
     * - 字段注释
     * - Field comment
     * - TIME_ZONE|Time zone;Time zone
     * - TM_SMP|TIMESTAMP;TIMESTAMP
     * - REQ_BUS_NO|交易流水号 | Request business number
     * - BAT_NO|Batch number;Batch number
     * 遇到下划线命名的要舍弃，其余的按照原先规则处理
     * 如果commentCn包含字段名，最后用处理好的commentEn覆盖
     */
    private void processFieldComment(RxField rxField) {
        String commentCn = rxField.getCommentCn();
        String commentEn = rxField.getCommentEn();

        // 如果两个注释都为空，直接返回
        if (StringUtils.isBlank(commentCn) && StringUtils.isBlank(commentEn)) {
            return;
        }

        // 先处理commentEn，确保它是最终处理好的
        if (StringUtils.isNotBlank(commentEn)) {
            String processedCommentEn = processCommentText(commentEn, rxField.getNameSnake());
            // 如果处理后的结果为空，则清空commentEn
            if (StringUtils.isBlank(processedCommentEn)) {
                rxField.setCommentEn(null);
            } else {
                rxField.setCommentEn(processedCommentEn);
            }
        }

        // 再处理commentCn
        if (StringUtils.isNotBlank(commentCn)) {
            // 检查commentCn是否包含字段名
            if (containsFieldName(commentCn, rxField.getNameSnake())) {
                // 如果包含字段名，用处理好的commentEn覆盖
                if (StringUtils.isNotBlank(rxField.getCommentEn())) {
                    rxField.setCommentCn(rxField.getCommentEn());
                } else {
                    // 如果commentEn为空，则清空commentCn
                    rxField.setCommentCn(null);
                }
            } else {
                // 如果不包含字段名，正常处理
                String processedCommentCn = processCommentText(commentCn, rxField.getNameSnake());
                // 如果处理后的结果为空，则清空commentCn
                if (StringUtils.isBlank(processedCommentCn)) {
                    rxField.setCommentCn(null);
                } else {
                    rxField.setCommentCn(processedCommentCn);
                }
            }
        }
    }

    /**
     * 处理注释文本的通用方法
     */
    private String processCommentText(String commentText, String fieldNameSnake) {
        // 检查是否包含分隔符 | 或 ｜ 或 ;
        if (commentText.contains("|") || commentText.contains("｜") || commentText.contains(";")) {
            // 按 | 或 ｜ 或 ; 分割
            String[] parts = commentText.split("[|｜;]");
            List<String> chineseParts = new ArrayList<>();
            List<String> englishParts = new ArrayList<>();

            for (String part : parts) {
                part = part.trim();
                if (StringUtils.isBlank(part)) {
                    continue;
                }

                // 跳过下划线命名的部分（如 TIME_ZONE, TM_SMP, REQ_BUS_NO, JRN_NO）
                if (isUnderscoreNamed(part)) {
                    continue;
                }

                // 跳过字段名（如 JRN_NO, REQ_BUS_NO 等）
                if (isFieldName(part, fieldNameSnake)) {
                    continue;
                }

                // 判断是否为中文
                if (BusinessUtils.isAllChinese(part)) {
                    chineseParts.add(part);
                } else {
                    // 处理全大写英文转小写
                    String processedPart = processEnglishText(part);
                    englishParts.add(processedPart);
                }
            }

            // 返回处理结果
            if (!chineseParts.isEmpty()) {
                return chineseParts.get(0);
            } else if (!englishParts.isEmpty()) {
                if (englishParts.size() == 1) {
                    return englishParts.get(0);
                } else {
                    // 取单词最多的那组
                    String longestPart = englishParts.get(0);
                    int maxWords = longestPart.split("\\s+").length;

                    for (String part : englishParts) {
                        int wordCount = part.split("\\s+").length;
                        if (wordCount > maxWords) {
                            maxWords = wordCount;
                            longestPart = part;
                        }
                    }
                    return longestPart;
                }
            }
        } else {
            // 没有分隔符的情况
            if (BusinessUtils.isAllChinese(commentText)) {
                return commentText;
            } else {
                return processEnglishText(commentText);
            }
        }

        // 如果所有部分都被过滤掉了，返回空字符串而不是原文本
        return "";
    }

    /**
     * 判断是否为下划线命名的字段（如 TIME_ZONE, TM_SMP, REQ_BUS_NO）
     */
    private boolean isUnderscoreNamed(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        // 检查是否包含下划线且全为大写字母和下划线组成
        // 这样可以避免将单个单词如 "DATE", "TIME", "NAME" 等误判为下划线命名
        return text.contains("_") && text.matches("^[A-Z_]+$");
    }

    /**
     * 判断是否为字段名（如 JRN_NO, REQ_BUS_NO 等）
     */
    private boolean isFieldName(String text, String fieldNameSnake) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(fieldNameSnake)) {
            return false;
        }

        // 直接比较是否等于字段名
        if (text.equals(fieldNameSnake)) {
            return true;
        }

        // 比较忽略大小写
        if (text.equalsIgnoreCase(fieldNameSnake)) {
            return true;
        }

        // 检查是否为字段名的变体（如去掉下划线等）
        String normalizedText = text.replace("_", "").toUpperCase();
        String normalizedFieldName = fieldNameSnake.replace("_", "").toUpperCase();
        if (normalizedText.equals(normalizedFieldName)) {
            return true;
        }

        // 检查是否为常见的字段名模式（如 DATE, TIME, NAME, ID 等）
        if (isCommonFieldName(text)) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为常见的字段名
     */
    private boolean isCommonFieldName(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        // 常见的字段名列表
        String[] commonFieldNames = {
                "DATE", "TIME", "NAME", "ID", "CODE", "NO", "NUM", "TYPE", "STATUS",
                "FLAG", "FLG", "AMT", "QTY", "CNT", "COUNT", "DESC", "REMARK", "NOTE",
                "USER", "PWD", "PASSWORD", "EMAIL", "PHONE", "TEL", "ADDR", "ADDRESS",
                "CITY", "PROVINCE", "COUNTRY", "ZIP", "POSTCODE", "URL", "LINK",
                "CREATED", "UPDATED", "MODIFIED", "DELETED", "ACTIVE", "ENABLED",
                "VISIBLE", "HIDDEN", "PUBLIC", "PRIVATE", "SECRET", "KEY", "VALUE",
                "TEXT", "CONTENT", "DATA", "INFO", "DETAIL", "SUMMARY", "TITLE",
                "SUBJECT", "TOPIC", "CATEGORY", "GROUP", "CLASS", "LEVEL", "GRADE",
                "SCORE", "RATE", "RATING", "PRICE", "COST", "FEE", "CHARGE", "PAYMENT",
                "ORDER", "ITEM", "PRODUCT", "GOODS", "SERVICE", "TASK", "JOB", "WORK",
                "PROJECT", "PLAN", "SCHEDULE", "EVENT", "MEETING", "CONFERENCE",
                "MESSAGE", "COMMENT", "REPLY", "ANSWER", "QUESTION", "PROBLEM",
                "ISSUE", "ERROR", "EXCEPTION", "WARNING", "ALERT", "NOTIFICATION",
                "LOG", "RECORD", "HISTORY", "BACKUP", "COPY", "VERSION", "REVISION",
                "CHANGE", "UPDATE", "MODIFY", "EDIT", "DELETE", "REMOVE", "CLEAR",
                "RESET", "INIT", "START", "BEGIN", "END", "FINISH", "COMPLETE",
                "DONE", "SUCCESS", "FAIL", "FAILED", "ERROR", "EXCEPTION", "WARNING"
        };

        String upperText = text.toUpperCase();
        for (String commonName : commonFieldNames) {
            if (upperText.equals(commonName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查文本是否包含字段名
     */
    private boolean containsFieldName(String text, String fieldNameSnake) {
        if (StringUtils.isBlank(text) || StringUtils.isBlank(fieldNameSnake)) {
            return false;
        }

        // 检查是否包含字段名（忽略大小写）
        if (text.toUpperCase().contains(fieldNameSnake.toUpperCase())) {
            return true;
        }

        // 检查是否包含字段名的变体（去掉下划线）
        String normalizedFieldName = fieldNameSnake.replace("_", "").toUpperCase();
        if (text.toUpperCase().contains(normalizedFieldName)) {
            return true;
        }

        return false;
    }

    /**
     * 处理表注释解析逻辑
     */
    private void processTableComment(TableEntity entity) {
        String tableCommentCn = entity.getTableCommentCn();
        String tableCommentEn = entity.getTableCommentEn();

        // 如果两个注释都为空，直接返回
        if (StringUtils.isBlank(tableCommentCn) && StringUtils.isBlank(tableCommentEn)) {
            return;
        }

        // 优先处理tableCommentCn，如果为空则处理tableCommentEn
        String commentToProcess = StringUtils.isNotBlank(tableCommentCn) ? tableCommentCn : tableCommentEn;

        // 检查是否包含分隔符 | 或 ｜ 或 ;
        if (commentToProcess.contains("|") || commentToProcess.contains("｜") || commentToProcess.contains(";")) {
            // 按 | 或 ｜ 或 ; 分割
            String[] parts = commentToProcess.split("[|｜;]");
            List<String> chineseParts = new ArrayList<>();
            List<String> englishParts = new ArrayList<>();

            for (String part : parts) {
                part = part.trim();
                if (StringUtils.isBlank(part)) {
                    continue;
                }

                // 判断是否为中文
                if (BusinessUtils.isAllChinese(part)) {
                    chineseParts.add(part);
                } else {
                    // 处理全大写英文转小写
                    String processedPart = processEnglishText(part);
                    englishParts.add(processedPart);
                }
            }

            // 设置中文注释（取第一个中文部分）
            if (!chineseParts.isEmpty()) {
                entity.setTableCommentCn(chineseParts.get(0));
            }

            // 设置英文注释（如果两组都是英文，取单词多的那组）
            if (!englishParts.isEmpty()) {
                if (englishParts.size() == 1) {
                    entity.setTableCommentEn(englishParts.get(0));
                } else {
                    // 取单词最多的那组
                    String longestPart = englishParts.get(0);
                    int maxWords = longestPart.split("\\s+").length;

                    for (String part : englishParts) {
                        int wordCount = part.split("\\s+").length;
                        if (wordCount > maxWords) {
                            maxWords = wordCount;
                            longestPart = part;
                        }
                    }
                    entity.setTableCommentEn(longestPart);
                }
            }
        } else {
            // 没有分隔符的情况
            if (BusinessUtils.isAllChinese(commentToProcess)) {
                entity.setTableCommentCn(commentToProcess);
            } else {
                String processedComment = processEnglishText(commentToProcess);
                entity.setTableCommentEn(processedComment);
            }
        }
    }

    /**
     * 处理英文文本，将全大写转换为小写
     */
    private String processEnglishText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }

        // 检查是否全为大写字母和空格
        boolean isAllUpperCase = text.matches("^[A-Z\\s]+$");
        if (isAllUpperCase) {
            return text.toLowerCase();
        }

        return text;
    }

    @Override
    public String toJavaType(String dbType,Integer length) {
        switch (dbType) {
            case "text":
            case "varchar":
                return "String";
            case "numeric":
            case "bigint":
            case "long":
                if(length==18){
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
    @SneakyThrows
    public boolean ifHasData(String tableName) {
        Connection conn = super.dbConfig.getEnsureConnection();
        String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    @Override
    @SneakyThrows
    public List<String> getAllTableNameLst() {
        Connection conn = super.dbConfig.getEnsureConnection();
        List<String> tableNames = new ArrayList<>();
        String schema = super.dbConfig.getSchema();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableNames.add(rs.getString("table_name"));
                }
            }
        }
        return tableNames;
    }

    @Override
    @SneakyThrows
    public TableEntity getTableEntityFromDatabase(String tableName) {
        Connection conn = super.dbConfig.getEnsureConnection();
        DatabaseMetaData meta = conn.getMetaData();
        String schema = super.dbConfig.getSchema();

        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableNameSnake(tableName);

        // 获取表注释
        try (ResultSet rs = meta.getTables(null, schema, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                String remarks = rs.getString("REMARKS");
                if (StringUtils.isNotBlank(remarks)) {
                    tableEntity.setTableCommentCn(remarks);
                    tableEntity.setTableCommentEn(remarks);
                }
            }
        }

        // 获取字段信息
        List<RxField> fields = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(null, schema, tableName, null)) {
            while (rs.next()) {
                RxField field = new RxField();
                field.setNameSnake(rs.getString("COLUMN_NAME"));
                field.setNameCamel(toCamelCase(rs.getString("COLUMN_NAME")));
                field.setDbTyp(rs.getString("TYPE_NAME"));
                field.setLength(rs.getInt("COLUMN_SIZE"));
                field.setType(toJavaType(rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE")));

                // 获取字段注释
                String remarks = rs.getString("REMARKS");
                if (StringUtils.isNotBlank(remarks)) {
                    field.setCommentCn(remarks);
                    field.setCommentEn(remarks);
                }

                // 检查是否允许为空
                int nullable = rs.getInt("NULLABLE");
                field.setNotNull(nullable == DatabaseMetaData.columnNoNulls);

                // 获取默认值
                String defaultValue = rs.getString("COLUMN_DEF");
                if (StringUtils.isNotBlank(defaultValue)) {
                    if (defaultValue.equalsIgnoreCase("Error") ||
                            defaultValue.equalsIgnoreCase("NULL") ||
                            defaultValue.equalsIgnoreCase("undefined") ||
                            defaultValue.equalsIgnoreCase("none")) {
                        if(StringUtils.equals(field.getDbTyp(),"numeric")){
                            defaultValue="0";
                        }else {
                            defaultValue="' '";
                        }
                        field.setDefaultValue(defaultValue);
                    }else {
                        if(defaultValue.contains("::")){
                            String[] split = defaultValue.split("::");
                            defaultValue=split[0];
                        }
                        if(defaultValue.contains(",")){
                            String[] split = defaultValue.split(",");
                            defaultValue=split[0];
                        }
                        field.setDefaultValue(defaultValue);
                    }
                }

                if(field.getLength()>9999 && !StringUtils.equals(field.getDbTyp(),"numeric")){
                    field.setDbTyp("text");
                    field.setLength(0);
                }
                if(StringUtils.equalsAny(field.getDbTyp(),"clob","blob","jsonb","json")){
                    field.setDbTyp("text");
                    field.setLength(0);
                }
                if(field.getLength()>0 &&StringUtils.equalsAny(field.getDbTyp(),"float8","float4","int4","int8")){
                    field.setDbTyp("numeric");
                }


                fields.add(field);
            }
        }
        tableEntity.setRxFields(fields);

        // 获取主键信息
        try (ResultSet rs = meta.getPrimaryKeys(null, schema, tableName)) {
            List<String> primaryKeyColumns = new ArrayList<>();
            while (rs.next()) {
                primaryKeyColumns.add(rs.getString("COLUMN_NAME"));
            }
            if (!primaryKeyColumns.isEmpty()) {
                TableEntity.Index primaryKey = new TableEntity.Index();
                primaryKey.setName("pk_" + tableName);
                primaryKey.setType(IndexTypEnum.PRIMARY.getCode());
                primaryKey.setFields(primaryKeyColumns);
                tableEntity.setPrimaryKey(primaryKey);
            }
        }

        // 获取索引信息
        TableEntity.Index primaryKeyIndex = null;
        List<TableEntity.Index> indexes = new ArrayList<>();
        try (ResultSet rs = meta.getIndexInfo(null, schema, tableName, false, false)) {
            Map<String, TableEntity.Index> indexMap = new HashMap<>();
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (StringUtils.isBlank(indexName)) {
                    continue; // 跳过空索引名
                }

                // 跳过主键索引，因为主键已经在上面单独处理了
                boolean isUnique = rs.getBoolean("NON_UNIQUE");
                if (StringUtils.startsWith(indexName.toLowerCase(),"pk_")) {
                    if(tableEntity.getPrimaryKey()==null){
                        if(primaryKeyIndex==null){
                            primaryKeyIndex = new TableEntity.Index();
                            primaryKeyIndex.setName(indexName);
                            primaryKeyIndex.setType(IndexTypEnum.PRIMARY.getCode());
                            primaryKeyIndex.setFields(new ArrayList<>());
                        }else {
                            String columnName = rs.getString("COLUMN_NAME");
                            if (StringUtils.isNotBlank(columnName)) {
                                primaryKeyIndex.getFields().add(columnName);
                            }
                        }
                    }
                    continue;
                }

                TableEntity.Index index = indexMap.computeIfAbsent(indexName, k -> {
                    TableEntity.Index idx = new TableEntity.Index();
                    idx.setName(indexName);
                    idx.setFields(new ArrayList<>());
                    return idx;
                });

                String columnName = rs.getString("COLUMN_NAME");
                if (StringUtils.isNotBlank(columnName)) {
                    index.getFields().add(columnName);
                }

                // 获取索引类型信息
                if (!isUnique) {
                    // NON_UNIQUE为false表示唯一索引
                    index.setType(IndexTypEnum.UNIQUE.getCode());
                } else {
                    // NON_UNIQUE为true表示普通索引
                    index.setType(IndexTypEnum.INDEX.getCode());
                }

            }
            indexes.addAll(indexMap.values());
        }

        if (tableEntity.getPrimaryKey() == null && primaryKeyIndex != null) {
            tableEntity.setPrimaryKey(primaryKeyIndex);
        }

        tableEntity.setIndexes(indexes);
        return tableEntity;
    }

    /**
     * 将下划线命名转换为驼峰命名
     */
    private String toCamelCase(String snakeCase) {
        if (StringUtils.isBlank(snakeCase)) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    @Override
    @SneakyThrows
    public boolean isTableExist(String tableNme) {
        Connection conn = super.dbConfig.getEnsureConnection();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableNme, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableNme.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableNme.toLowerCase(), new String[]{"TABLE"})) {
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
