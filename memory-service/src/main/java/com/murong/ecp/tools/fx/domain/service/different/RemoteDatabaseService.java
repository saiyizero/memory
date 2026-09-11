package com.murong.ecp.tools.fx.domain.service.different;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.enums.IndexTypEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.DbConnectionDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程数据库服务 - 优化版本，支持连接池和批量处理
 */
@Service
public class RemoteDatabaseService {

    @Autowired
    private DbConnectionDao dbConnectionDao;

    @Autowired
    private GlobalProperties globalProps;

    // 连接池缓存，key为环境名，value为数据库连接
    private final Map<String, Connection> connectionPool = new ConcurrentHashMap<>();
    
    // 连接配置缓存，避免重复查询
    private final Map<String, DbConnectionPO> connectionConfigCache = new ConcurrentHashMap<>();

    /**
     * 获取远程表结构（单表）
     * 
     * @param tableName 表名
     * @param compareEnv 对比环境
     * @return 远程表结构，如果表不存在返回null，如果连接失败抛出异常
     */
    public TableEntity getRemoteTableStructure(String tableName, String compareEnv) {
        try {
            Connection connection = getOrCreateConnection(compareEnv);
            DbConnectionPO dbConnection = getDbConnection(compareEnv);
            return getTableStructureFromConnection(connection, tableName, dbConnection.getSchemaNm());
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取远程表结构（优化版本，带进度回调）
     * 
     * @param tableNames 表名列表
     * @param compareEnv 对比环境
     * @param progressCallback 进度回调函数，参数为(进度, 当前表名)
     * @return Map<表名, 表结构>，不存在的表返回null
     */
    public Map<String, TableEntity> batchGetRemoteTableStructure(List<String> tableNames, String compareEnv, 
                                                               java.util.function.BiConsumer<Double, String> progressCallback) {
        Map<String, TableEntity> result = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        System.out.println("开始批量获取 " + tableNames.size() + " 张表的结构，环境: " + compareEnv);
        
        try {
            Connection connection = getOrCreateConnection(compareEnv);
            DbConnectionPO dbConnection = getDbConnection(compareEnv);
            String schema = dbConnection.getSchemaNm();
            
            // 批量获取表结构
            int totalTables = tableNames.size();
            int processedTables = 0;
            
            for (String tableName : tableNames) {
                processedTables++;
                double progress = (double) processedTables / totalTables;
                
                try {
                    if (progressCallback != null) {
                        progressCallback.accept(progress, tableName);
                    }
                    
                    TableEntity tableEntity = getTableStructureFromConnection(connection, tableName, schema);
                    result.put(tableName, tableEntity);
                } catch (Exception e) {
                    // 单个表获取失败，记录错误但不影响其他表
                    System.err.println("获取表 " + tableName + " 结构失败: " + e.getMessage());
                    result.put(tableName, null);
                }
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("批量获取表结构完成，耗时: " + (endTime - startTime) + "ms，成功获取: " + result.size() + " 张表");
            
        } catch (Exception e) {
            throw new RuntimeException("批量获取表结构失败: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * 批量获取远程表结构（优化版本，兼容旧版本）
     * 
     * @param tableNames 表名列表
     * @param compareEnv 对比环境
     * @return Map<表名, 表结构>，不存在的表返回null
     */
    public Map<String, TableEntity> batchGetRemoteTableStructure(List<String> tableNames, String compareEnv) {
        return batchGetRemoteTableStructure(tableNames, compareEnv, null);
    }

    /**
     * 获取或创建数据库连接（带连接池管理）
     */
    private Connection getOrCreateConnection(String compareEnv) throws SQLException {
        return connectionPool.computeIfAbsent(compareEnv, env -> {
            try {
                System.out.println("创建新的数据库连接到环境: " + env);
                DbConnectionPO dbConnection = getDbConnection(env);
                if (dbConnection == null) {
                    throw new RuntimeException("无法找到环境 " + env + " 的数据库连接配置");
                }
                return createConnection(dbConnection);
            } catch (SQLException e) {
                throw new RuntimeException("创建数据库连接失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 获取数据库连接配置（带缓存）
     */
    private DbConnectionPO getDbConnection(String compareEnv) {
        return connectionConfigCache.computeIfAbsent(compareEnv, env -> {
            String groupName = globalProps.getGroupName();
            String projectName = globalProps.getProjectName();
            String appName = globalProps.getAppName();
            String currentSchema = globalProps.getDDLSchema();

            // 1. 首先尝试查找完全匹配的连接（环境+项目+应用+Schema）
            List<DbConnectionPO> connections = dbConnectionDao.queryByEnvAndProject(env, groupName, projectName, appName);
            for (DbConnectionPO conn : connections) {
                if (currentSchema.equals(conn.getSchemaNm())) {
                    return conn;
                }
            }

            // 2. 查找环境+项目+应用，Schema为all的连接
            for (DbConnectionPO conn : connections) {
                if ("all".equalsIgnoreCase(conn.getSchemaNm())) {
                    return conn;
                }
            }

            // 3. 查找环境+项目，应用为all的连接
            connections = dbConnectionDao.queryByEnvAndProject(env, groupName, projectName, "all");
            for (DbConnectionPO conn : connections) {
                if (currentSchema.equals(conn.getSchemaNm()) || "all".equalsIgnoreCase(conn.getSchemaNm())) {
                    return conn;
                }
            }

            // 4. 查找环境+项目群，项目为all的连接
            connections = dbConnectionDao.queryByEnvAndProject(env, groupName, "all", "all");
            for (DbConnectionPO conn : connections) {
                if (currentSchema.equals(conn.getSchemaNm()) || "all".equalsIgnoreCase(conn.getSchemaNm())) {
                    return conn;
                }
            }

            // 5. 最后查找环境为all的连接
            connections = dbConnectionDao.queryByEnvAndProject("all", groupName, projectName, appName);
            for (DbConnectionPO conn : connections) {
                if (currentSchema.equals(conn.getSchemaNm()) || "all".equalsIgnoreCase(conn.getSchemaNm())) {
                    return conn;
                }
            }

            return null;
        });
    }

    /**
     * 创建数据库连接
     */
    private Connection createConnection(DbConnectionPO dbConnection) throws SQLException {
        try {
            // 加载驱动
            Class.forName(dbConnection.getDriver());
            
            // 创建连接
            return java.sql.DriverManager.getConnection(
                dbConnection.getJdbcUrl(),
                dbConnection.getUsername(),
                dbConnection.getPassword()
            );
        } catch (ClassNotFoundException e) {
            throw new SQLException("无法加载数据库驱动: " + dbConnection.getDriver(), e);
        }
    }

    /**
     * 从数据库连接获取表结构
     */
    private TableEntity getTableStructureFromConnection(Connection connection, String tableName, String schema) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        
        // 获取表信息
        ResultSet tableRs = metaData.getTables(null, schema, tableName, new String[]{"TABLE"});
        if (!tableRs.next()) {
            return null; // 表不存在
        }

        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableNameSnake(tableName);
        
        // 生成驼峰表名
        tableEntity.setTableNameCamel(convertToCamelCase(tableName));

        // 获取字段信息
        List<RxField> fields = new ArrayList<>();
        ResultSet columnRs = metaData.getColumns(null, schema, tableName, null);
        
        while (columnRs.next()) {
            RxField field = new RxField();
            String columnName = columnRs.getString("COLUMN_NAME");
            field.setNameSnake(columnName);
            
            // 生成驼峰命名
            field.setNameCamel(convertToCamelCase(columnName));
            
            // 处理字段类型
            String dbType = columnRs.getString("TYPE_NAME");
            field.setDbTyp(dbType);
            
            // 将数据库类型转换为Java类型
            String javaType = convertDbTypeToJavaType(dbType);
            field.setType(javaType);
            
            // 处理字段长度
            int columnSize = columnRs.getInt("COLUMN_SIZE");
            int decimalDigits = columnRs.getInt("DECIMAL_DIGITS");
            
            // 对于text类型，不设置长度，避免对比差异
            String upperDbType = dbType.toUpperCase();
            if ("TEXT".equals(upperDbType) || "LONGTEXT".equals(upperDbType) || "MEDIUMTEXT".equals(upperDbType) || "TINYTEXT".equals(upperDbType)) {
                // text类型不设置长度
                field.setLength(null);
            } else if (decimalDigits > 0) {
                // 对于小数类型，我们需要特殊处理
                // 由于RxField的length是Integer，我们暂时只存储总长度
                field.setLength(columnSize);
            } else if (columnSize > 0) {
                field.setLength(columnSize);
            }
            
            // 处理默认值
            String defaultValue = columnRs.getString("COLUMN_DEF");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                field.setDefaultValue(defaultValue);
            }
            
            // 处理是否允许空值
            int nullable = columnRs.getInt("NULLABLE");
            field.setNotNull(nullable == DatabaseMetaData.columnNoNulls);
            
            // 处理字段注释
            String remarks = columnRs.getString("REMARKS");
            if (remarks != null && !remarks.isEmpty()) {
                field.setCommentCn(remarks);
                field.setCommentEn(remarks);
            }
            
            fields.add(field);
        }
        tableEntity.setRxFields(fields);

        // 获取主键信息
        ResultSet pkRs = metaData.getPrimaryKeys(null, schema, tableName);
        List<String> primaryKeyFields = new ArrayList<>();
        while (pkRs.next()) {
            String pkColumn = pkRs.getString("COLUMN_NAME");
            // 主键字段使用下划线命名（DDL中需要）
            primaryKeyFields.add(pkColumn);
        }
        
        if (!primaryKeyFields.isEmpty()) {
            TableEntity.Index primaryKey = new TableEntity.Index();
            primaryKey.setName("pk_" + tableName);
            primaryKey.setIndexType(IndexTypEnum.PRIMARY);
            primaryKey.setFields(primaryKeyFields);
            tableEntity.setPrimaryKey(primaryKey);
        }

        return tableEntity;
    }
    
    /**
     * 将数据库类型转换为Java类型
     */
    private String convertDbTypeToJavaType(String dbType) {
        if (dbType == null) {
            return "String";
        }
        
        String upperDbType = dbType.toUpperCase();
        
        switch (upperDbType) {
            case "INTEGER":
            case "INT":
            case "SMALLINT":
            case "BIGINT":
                return "Integer";
            case "DECIMAL":
            case "NUMERIC":
            case "DOUBLE":
            case "FLOAT":
            case "REAL":
                return "BigDecimal";
            case "DATE":
            case "TIMESTAMP":
            case "TIME":
                return "String";
            case "BOOLEAN":
            case "BOOL":
                return "Boolean";
            case "BLOB":
            case "BINARY":
            case "VARBINARY":
                return "byte[]";
            default:
                return "String";
        }
    }
    
    /**
     * 将下划线命名转换为驼峰命名
     */
    private String convertToCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
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

    /**
     * 关闭指定环境的连接
     */
    public void closeConnection(String compareEnv) {
        Connection connection = connectionPool.remove(compareEnv);
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
        // 清除配置缓存
        connectionConfigCache.remove(compareEnv);
    }

    /**
     * 关闭所有连接
     */
    public void closeAllConnections() {
        for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
            try {
                entry.getValue().close();
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
        connectionPool.clear();
        connectionConfigCache.clear();
    }

    /**
     * 应用销毁时关闭所有连接
     */
    @PreDestroy
    public void destroy() {
        closeAllConnections();
    }
} 