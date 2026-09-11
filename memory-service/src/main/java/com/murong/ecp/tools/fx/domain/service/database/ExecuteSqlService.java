package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.ExeStatusEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.DbConnectionDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.ExeSqlRecordDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class ExecuteSqlService {
    @Autowired
    GlobalProperties globalPropes;
    @Autowired
    DbConnectionDao dbConnDao;
    @Autowired
    ExeSqlRecordDao exeSqlRecordDao;

    /**
     * 执行sql
     * 支持select和非select，select返回表格数据，非select登记表记录
     **/
    public CrResult<Object> executeSql(String sql, String envName) {
        String groupName = globalPropes.getGroupName();
        CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);

        // 获取当前group/env下所有schema
        DbConnectionPO schemaQuery = new DbConnectionPO();
        schemaQuery.setGroupName(groupName);
        schemaQuery.setEnvName(envName);
        List<DbConnectionPO> dbList = dbConnDao.queryForList(schemaQuery);
        Set<String> validSchemas = new HashSet<>();
        DbConnectionPO allSchemaPO = null;
        for (DbConnectionPO po : dbList) {
            if (po.getSchemaNm() != null && !po.getSchemaNm().isEmpty()) {
                if ("all".equalsIgnoreCase(po.getSchemaNm())) {
                    allSchemaPO = po;
                } else {
                    validSchemas.add(po.getSchemaNm().toLowerCase());
                }
            }
        }

        // 解析SQL语句
        List<SqlStatement> sqlStatements = parseSqlStatements(sql);
        if (sqlStatements.isEmpty()) {
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("未找到有效的SQL语句");
            return result;
        }

        // 验证SQL语句
        CrResult<Object> validationResult = validateSqlStatements(sqlStatements, validSchemas, allSchemaPO);
        if (!validationResult.isSucess()) {
            return validationResult;
        }

        // 按Schema分组SQL语句
        Map<String, List<SqlStatement>> schemaGroups = groupSqlBySchema(sqlStatements, validSchemas, allSchemaPO);
        
        // 构建完整的SQL文本用于记录
        String formattedSql = buildFormattedSql(sqlStatements);
        
        // 创建执行记录
        StringBuffer schemaBuffer = new StringBuffer();
        int count = 0;
        int totalSize = schemaGroups.size();
        for (Map.Entry<String, List<SqlStatement>> entry : schemaGroups.entrySet()) {
            count++;
            if (count == totalSize) {
                schemaBuffer.append(entry.getKey());
            } else {
                schemaBuffer.append(entry.getKey()).append(",\n");
            }
        }

        Boolean updStatus = false;
        ExeSqlRecordPO exeSqlRecordPO = new ExeSqlRecordPO();
        exeSqlRecordPO.setGroupName(groupName);
        exeSqlRecordPO.setEnvName(envName);
        exeSqlRecordPO.setSchemaNm(schemaBuffer.toString());
        if(formattedSql.length()>0){
            updStatus=true;
            exeSqlRecordPO.setExeSql(formattedSql);
            exeSqlRecordPO.setExeDate(MrDateUtils.getCurrentDate());
            exeSqlRecordPO.setExeTime(MrDateUtils.getCurrentShortTime());
            exeSqlRecordPO.setStatus(ExeStatusEnum.INIT.getKey());
            exeSqlRecordDao.save(exeSqlRecordPO);
        }

        // 执行SQL语句
        try {
            Object executionResult = executeSqlGroups(schemaGroups, dbList);
            // 更新执行记录为成功
            if(updStatus){
                ExeSqlRecordPO updatePO = new ExeSqlRecordPO();
                updatePO.setStatus(ExeStatusEnum.SUCCESS.getKey());
                ExeSqlRecordPO wherePO = new ExeSqlRecordPO();
                wherePO.setGroupName(exeSqlRecordPO.getGroupName());
                wherePO.setExeDate(exeSqlRecordPO.getExeDate());
                wherePO.setExeTime(exeSqlRecordPO.getExeTime());
                wherePO.setEnvName(exeSqlRecordPO.getEnvName());
                exeSqlRecordDao.updateByOne(updatePO, wherePO);
            }
            result.setData(executionResult);
        } catch (Exception e) {
            // 更新执行记录为失败
            if(updStatus){
                ExeSqlRecordPO updatePO = new ExeSqlRecordPO();
                updatePO.setStatus(ExeStatusEnum.FAIL.getKey());
                ExeSqlRecordPO wherePO = new ExeSqlRecordPO();
                wherePO.setGroupName(exeSqlRecordPO.getGroupName());
                wherePO.setExeDate(exeSqlRecordPO.getExeDate());
                wherePO.setExeTime(exeSqlRecordPO.getExeTime());
                wherePO.setEnvName(exeSqlRecordPO.getEnvName());
                exeSqlRecordDao.updateByOne(updatePO, wherePO);
            }
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("SQL执行失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 解析SQL语句
     */
    private List<SqlStatement> parseSqlStatements(String sql) {
        List<SqlStatement> statements = new ArrayList<>();
        
        // 按行分割SQL
        String[] lines = sql.split("\n");
        StringBuilder currentStatement = new StringBuilder();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 跳过注释行
            if (trimmedLine.startsWith("--")) {
                continue;
            }
            
            // 将当前行添加到语句中
            currentStatement.append(line).append("\n");
            
            // 如果当前行以分号结尾，说明一个语句结束
            if (trimmedLine.endsWith(";")) {
                String statement = currentStatement.toString().trim();
                if (!statement.isEmpty()) {
                    // 移除末尾的分号
                    statement = statement.substring(0, statement.length() - 1);
                    
                    SqlStatement sqlStatement = new SqlStatement();
                    sqlStatement.setOriginalSql(statement);
                    sqlStatement.setIsSelect(statement.toLowerCase().startsWith("select"));
                    sqlStatement.setSchemas(extractSchemasByJSqlParser(statement));
                    statements.add(sqlStatement);
                }
                currentStatement = new StringBuilder();
            }
        }
        
        // 处理最后一个没有分号的语句
        String lastStatement = currentStatement.toString().trim();
        if (!lastStatement.isEmpty()) {
            SqlStatement sqlStatement = new SqlStatement();
            sqlStatement.setOriginalSql(lastStatement);
            sqlStatement.setIsSelect(lastStatement.toLowerCase().startsWith("select"));
            sqlStatement.setSchemas(extractSchemasByJSqlParser(lastStatement));
            statements.add(sqlStatement);
        }
        
        return statements;
    }

    /**
     * 验证SQL语句
     */
    private CrResult<Object> validateSqlStatements(List<SqlStatement> statements, Set<String> validSchemas, DbConnectionPO allSchemaPO) {
        int selectCount = 0;
        
        for (SqlStatement statement : statements) {
            if (statement.isSelect()) {
                selectCount++;
            }
            
            // 检查是否解析出Schema
            if (statement.getSchemas().isEmpty()) {
                CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("未能解析出表schema，SQL: " + statement.getOriginalSql());
                return result;
            }
            
            // 检查Schema是否有效
            boolean hasValidSchema = false;
            for (String schema : statement.getSchemas()) {
                if (validSchemas.contains(schema.toLowerCase())) {
                    hasValidSchema = true;
                    break;
                }
            }
            
            // 如果不是select语句且包含多个Schema，报错
            if (!statement.isSelect() && statement.getSchemas().size() > 1) {
                CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("非select语句不能包含多个Schema: " + statement.getSchemas() + "，SQL: " + statement.getOriginalSql());
                return result;
            }
            
            // 如果是select语句且包含多个Schema，检查是否有all数据源
            if (statement.isSelect() && statement.getSchemas().size() > 1) {
                if (allSchemaPO == null) {
                    CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                    result.setMsgInf("select语句包含多个Schema但无all数据源: " + statement.getSchemas() + "，SQL: " + statement.getOriginalSql());
                    return result;
                }
            }
            
            // 如果没有有效Schema且没有all数据源，报错
            if (!hasValidSchema && allSchemaPO == null) {
                CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("schema不存在，且无all数据源: " + statement.getSchemas() + "，SQL: " + statement.getOriginalSql());
                return result;
            }
        }
        
        // 检查是否有多个select语句
        if (selectCount > 1) {
            CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("不支持多条select语句，请只保留一条select");
            return result;
        }
        
        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    /**
     * 按Schema分组SQL语句
     */
    private Map<String, List<SqlStatement>> groupSqlBySchema(List<SqlStatement> statements, Set<String> validSchemas, DbConnectionPO allSchemaPO) {
        Map<String, List<SqlStatement>> groups = new LinkedHashMap<>();
        
        for (SqlStatement statement : statements) {
            String targetSchema = null;
            
            if (statement.isSelect() && statement.getSchemas().size() > 1) {
                // select语句包含多个Schema，使用all
                targetSchema = "all";
            } else {
                // 查找第一个有效的Schema
                for (String schema : statement.getSchemas()) {
                    if (validSchemas.contains(schema.toLowerCase())) {
                        targetSchema = schema;
                        break;
                    }
                }
            }
            
            if (targetSchema == null) {
                // 如果没有找到有效Schema，使用all（如果存在）
                if (allSchemaPO != null) {
                    targetSchema = "all";
                }
            }
            
            if (targetSchema != null) {
                groups.computeIfAbsent(targetSchema, k -> new ArrayList<>()).add(statement);
            }
        }
        
        return groups;
    }

    /**
     * 构建格式化的SQL文本
     */
    private String buildFormattedSql(List<SqlStatement> statements) {
        StringBuilder sb = new StringBuilder();
        boolean firstStatement = true;
        
        for (SqlStatement statement : statements) {
            String sql = statement.getOriginalSql();
            if(StringUtils.startsWith(sql, "select")) {
                continue;
            }

            if (!firstStatement) {
                sb.append("\n");
            }

            // 检查是否有注释且不是第一行
            if (sql.contains("--") && !firstStatement) {
                sb.append("\n");
            }
            
            // 确保每条SQL语句后面都有分号
            if (!sql.trim().endsWith(";")) {
                sql = sql + ";";
            }
            
            sb.append(sql);
            firstStatement = false;
        }
        
        return sb.toString();
    }

    /**
     * 执行分组后的SQL语句
     */
    private Object executeSqlGroups(Map<String, List<SqlStatement>> schemaGroups, List<DbConnectionPO> dbList) throws Exception {
        Object lastSelectData = null;
        Integer lastUpdateCount = 0;
        
        // 存储所有数据库配置，用于统一事务管理
        List<DbConfig> dbConfigs = new ArrayList<>();
        List<String> schemas = new ArrayList<>();
        
        // 先准备所有数据库连接
        for (Map.Entry<String, List<SqlStatement>> entry : schemaGroups.entrySet()) {
            String schema = entry.getKey();
            List<SqlStatement> statements = entry.getValue();
            
            // 查找对应的数据库连接
            DbConnectionPO dbConnPO = null;
            for (DbConnectionPO po : dbList) {
                if (schema.equalsIgnoreCase(po.getSchemaNm())) {
                    dbConnPO = po;
                    break;
                }
            }
            
            if (dbConnPO == null) {
                throw new Exception("未找到Schema " + schema + " 的数据库连接信息");
            }
            
            DbConfig dbConfig = new DbConfig(dbConnPO);
            dbConfigs.add(dbConfig);
            schemas.add(schema);
        }
        
        // 开启所有数据库的事务
        for (DbConfig dbConfig : dbConfigs) {
            dbConfig.beginTransaction();
        }
        
        try {
            // 按Schema顺序执行SQL
            int configIndex = 0;
            for (Map.Entry<String, List<SqlStatement>> entry : schemaGroups.entrySet()) {
                String schema = entry.getKey();
                List<SqlStatement> statements = entry.getValue();
                DbConfig dbConfig = dbConfigs.get(configIndex);
                
                for (SqlStatement statement : statements) {
                    if (statement.isSelect()) {
                        List<Map<String, Object>> tableData = dbConfig.selectSql(statement.getOriginalSql());
                        lastSelectData = tableData;
                    } else {
                        int count = dbConfig.executeSql(statement.getOriginalSql());
                        lastUpdateCount += count;
                    }
                }
                
                configIndex++;
            }
            
            // 所有SQL执行成功，提交所有事务
            for (DbConfig dbConfig : dbConfigs) {
                dbConfig.commitTransaction();
            }
            
        } catch (Exception e) {
            // 任何SQL执行失败，回滚所有事务
            for (DbConfig dbConfig : dbConfigs) {
                try {
                    dbConfig.rollbackTransaction();
                } catch (Exception rollbackEx) {
                    // 记录回滚异常，但不影响主异常抛出
                    System.err.println("回滚事务失败: " + rollbackEx.getMessage());
                }
            }
            throw e;
        }
        
        return lastSelectData != null ? lastSelectData : lastUpdateCount;
    }

    /**
     * 使用JSqlParser解析SQL并提取所有 schema （如schema.table），支持DML和DDL，兼容所有SQL语法，自动拆分schema
     */
    public Set<String> extractSchemasByJSqlParser(String sql) {
        Set<String> schemas = new HashSet<>();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableList = tablesNamesFinder.getTableList(statement);
            for (String table : tableList) {
                String[] parts = table.split("\\.", 2);
                if (parts.length == 2) {
                    String schema = parts[0];
                    if (schema.startsWith("\"") && schema.endsWith("\"") && schema.length() > 1) {
                        schema = schema.substring(1, schema.length() - 1);
                    }
                    schema = schema.toLowerCase();
                    schemas.add(schema);
                }
            }
        } catch (Exception e) {
            // JSqlParser不支持的DDL语句，降级用正则提取
            // 扩展正则表达式，支持更多SQL语句类型
            String regex = "(?i)\\b(?:from|join|update|into|delete\\s+from|alter\\s+table|truncate\\s+table|drop\\s+table|create\\s+table|create\\s+index|drop\\s+index|on|insert\\s+into)\\s+((?:[a-zA-Z0-9_]+)|(?:\"[^\"]+\"))\\.(?:[a-zA-Z0-9_]+|\"[^\"]+\")";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(sql);
            while (m.find()) {
                String schema = m.group(1);
                if (schema.startsWith("\"") && schema.endsWith("\"") && schema.length() > 1) {
                    schema = schema.substring(1, schema.length() - 1);
                }
                schema = schema.toLowerCase();
                schemas.add(schema);
            }
            
            // 如果正则表达式没有匹配到，尝试更简单的模式
            if (schemas.isEmpty()) {
                String simpleRegex = "([a-zA-Z0-9_]+)\\.[a-zA-Z0-9_]+";
                Pattern simplePattern = Pattern.compile(simpleRegex);
                Matcher simpleMatcher = simplePattern.matcher(sql);
                while (simpleMatcher.find()) {
                    String schema = simpleMatcher.group(1).toLowerCase();
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    /**
     * 登记SQL语句到执行记录表
     * @param sql SQL语句
     * @param envName 环境名称
     * @return 登记结果
     */
    public CrResult<Object> registerSql(String sql, String envName) {
        CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        
        try {
            // 验证参数
            if (StringUtils.isBlank(sql)) {
                result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("SQL语句不能为空");
                return result;
            }
            
            if (StringUtils.isBlank(envName)) {
                result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("环境名称不能为空");
                return result;
            }
            
            // 解析SQL语句获取schema信息
            List<SqlStatement> sqlStatements = parseSqlStatements(sql);
            if (sqlStatements.isEmpty()) {
                result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("未找到有效的SQL语句");
                return result;
            }
            
            // 登记时直接提取所有schema，不校验数据源是否存在
            Set<String> allSchemas = new HashSet<>();
            for (SqlStatement statement : sqlStatements) {
                allSchemas.addAll(statement.getSchemas());
            }
            
            // 构建schema字符串
            StringBuffer schemaBuffer = new StringBuffer();
            int count = 0;
            int totalSize = allSchemas.size();
            for (String schema : allSchemas) {
                count++;
                if (count == totalSize) {
                    schemaBuffer.append(schema);
                } else {
                    schemaBuffer.append(schema).append(",\n");
                }
            }
            
            // 创建执行记录
            ExeSqlRecordPO exeSqlRecordPO = new ExeSqlRecordPO();
            exeSqlRecordPO.setGroupName(globalPropes.getGroupName());
            exeSqlRecordPO.setEnvName(envName);
            exeSqlRecordPO.setSchemaNm(schemaBuffer.toString());
            exeSqlRecordPO.setExeSql(sql.trim());
            exeSqlRecordPO.setExeDate(MrDateUtils.getCurrentDate());
            exeSqlRecordPO.setExeTime(MrDateUtils.getCurrentShortTime());
            exeSqlRecordPO.setStatus(ExeStatusEnum.INIT.getKey());
            
            // 保存到数据库
            exeSqlRecordDao.save(exeSqlRecordPO);
            result.setMsgInf("SQL登记成功！");
        } catch (Exception e) {
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("SQL登记失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 删除执行记录
     * @return 删除结果
     */
    public CrResult<Object> deleteExeSqlRecord(ExeSqlRecordPO po) {
        CrResult<Object> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        try {
            ExeSqlRecordPO wherePO = new ExeSqlRecordPO();
            wherePO.setId(po.getId());
            wherePO.setGroupName(po.getGroupName());
            exeSqlRecordDao.delete(wherePO);
            result.setMsgInf("删除成功");
            
        } catch (Exception e) {
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("删除执行记录失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * SQL语句内部类
     */
    private static class SqlStatement {
        private String originalSql;
        private boolean isSelect;
        private Set<String> schemas;

        public String getOriginalSql() {
            return originalSql;
        }

        public void setOriginalSql(String originalSql) {
            this.originalSql = originalSql;
        }

        public boolean isSelect() {
            return isSelect;
        }

        public void setIsSelect(boolean select) {
            isSelect = select;
        }

        public Set<String> getSchemas() {
            return schemas;
        }

        public void setSchemas(Set<String> schemas) {
            this.schemas = schemas;
        }
    }
}

