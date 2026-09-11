package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DataMigrationService {

    private static final int BATCH_SIZE = 500; // 批量处理大小

    /**
     * 检查表是否存在
     */
    private boolean isTableExist(Connection conn, String tableName, String schema) throws SQLException {
        // 解析表名，支持schema.tableName格式
        String actualSchema = schema;
        String actualTableName = tableName;
        
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            actualSchema = parts[0];
            actualTableName = parts[1];
        }
        
        try (ResultSet rs = conn.getMetaData().getTables(actualSchema, null, actualTableName, new String[]{"TABLE"})) {
            boolean exists = rs.next();
            if (exists) {
                log.info("表 {} 存在，schema: {}, 实际表名: {}", tableName, actualSchema, actualTableName);
            } else {
                log.warn("表 {} 不存在，schema: {}, 实际表名: {}", tableName, actualSchema, actualTableName);
            }
            return exists;
        }
    }

    /**
     * 安全关闭数据库连接
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("关闭数据库连接失败", e);
            }
        }
    }

    /**
     * 带进度回调的数据迁移方法
     * @param source 源数据源连接信息
     * @param target 目标数据源连接信息
     * @param tableList 要迁移的表名列表
     * @param progressCallback 进度回调接口
     */
    public CrResult<String> migrateWithProgress(DbConnectionPO source, DbConnectionPO target, List<String> tableList, 
                                               ProgressCallback progressCallback) {
        CrResult<String> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        
        if (source == null || target == null || tableList == null || tableList.isEmpty()) {
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("参数不能为空");
            return result;
        }

        DbConfig sourceConfig = null;
        DbConfig targetConfig = null;
        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            // 创建数据源配置
            sourceConfig = new DbConfig(source);
            targetConfig = new DbConfig(target);

            // 获取连接
            sourceConn = sourceConfig.getEnsureConnection();
            targetConn = targetConfig.getEnsureConnection();
            
            // 获取schema信息
            String sourceSchema = source.getSchemaNm();
            String targetSchema = target.getSchemaNm();
            if(StringUtils.equals(sourceSchema,"all")) {
                sourceSchema=targetSchema;
            }

            // 验证所有表是否存在
            for (String tableName : tableList) {
                if (!isTableExist(sourceConn, tableName, sourceSchema)) {
                    result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                    result.setMsgInf("源数据库中表 " + tableName + " 不存在");
                    return result;
                }

                if (!isTableExist(targetConn, tableName, targetSchema)) {
                    result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                    result.setMsgInf("目标数据库中表 " + tableName + " 不存在");
                    return result;
                }
            }

            // 开始批量迁移
            long startTime = System.currentTimeMillis();
            int totalTables = tableList.size();
            int successTables = 0;
            final int[] totalRows = {0};
            final int[] totalMigratedRows = {0};
            StringBuilder resultMessage = new StringBuilder();

            for (int i = 0; i < tableList.size(); i++) {
                final int tableIndex = i;
                String tableName = tableList.get(i);
                log.info("开始迁移表 {}/{}: {}", i + 1, totalTables, tableName);

                try {
                    // 获取总行数
                    int tableTotalRows = getTableRowCount(sourceConn, tableName, sourceSchema);
                    if (tableTotalRows == 0) {
                        log.info("表 {} 没有数据需要迁移", tableName);
                        resultMessage.append(String.format("表 %s: 无数据需要迁移", tableName)).append("\n");
                        continue;
                    }

                    // 获取表结构信息
                    AbstractDataBaseHandler inputDataBaseHandler = getInputDataBaseHandler(sourceConfig);
                    List<String> columns = inputDataBaseHandler.getTableColumns(sourceConn, tableName, sourceSchema);
                    if (columns.isEmpty()) {
                        log.warn("表 {} 没有列信息，跳过", tableName);
                        resultMessage.append(String.format("表 %s: 无列信息，跳过", tableName)).append("\n");
                        continue;
                    }

                    // 迁移表数据
                    int migratedRows = migrateTableDataWithProgress(sourceConn, targetConn, tableName, columns, tableTotalRows, 
                        new TableProgressCallback() {
                            @Override
                            public void onProgress(int currentRows, int totalTableRows, double progress, String currentTableName) {
                                // 计算总体进度：当前表进度 + 已完成表的进度
                                double tableWeight = 100.0 / totalTables; // 每个表的权重
                                double completedTablesProgress = tableIndex * tableWeight; // 已完成表的进度
                                double currentTableProgress = (progress / 100.0) * tableWeight; // 当前表的进度
                                double overallProgress = completedTablesProgress + currentTableProgress;
                                
                                if (progressCallback != null) {
                                    // 使用带表名的进度回调
                                    progressCallback.onProgress(totalMigratedRows[0] + currentRows, totalRows[0] + tableTotalRows, overallProgress, currentTableName);
                                }
                            }
                        }, sourceSchema, targetSchema);

                    totalRows[0] += tableTotalRows;
                    totalMigratedRows[0] += migratedRows;
                    successTables++;

                    String tableResult = String.format("表 %s: 迁移 %d/%d 行数据", tableName, migratedRows, tableTotalRows);
                    resultMessage.append(tableResult).append("\n");
                    log.info(tableResult);

                } catch (Exception e) {
                    log.error("迁移表 {} 失败", tableName, e);
                    resultMessage.append(String.format("表 %s: 迁移失败 - %s", tableName, e.getMessage())).append("\n");
                }
            }

            long endTime = System.currentTimeMillis();
            String summaryMessage = String.format("批量数据迁移完成！\n成功迁移表数: %d/%d\n总迁移行数: %d/%d\n总耗时: %d ms\n\n详细结果:\n%s", 
                successTables, totalTables, totalMigratedRows[0], totalRows[0], (endTime - startTime), resultMessage.toString());
            
            result.setData(summaryMessage);
            log.info("批量数据迁移完成，成功迁移 {} 个表，总迁移行数: {}/{}", successTables, totalMigratedRows[0], totalRows[0]);

        } catch (Exception e) {
            log.error("数据迁移失败", e);
            result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("数据迁移失败: " + e.getMessage());
        } finally {
            // 关闭连接
            closeConnection(sourceConn);
            closeConnection(targetConn);
        }

        return result;
    }

    /**
     * 获取表的行数
     */
    private int getTableRowCount(Connection conn, String tableName, String schema) throws SQLException {
        // 解析表名，支持schema.tableName格式
        String actualSchema = schema;
        String actualTableName = tableName;
        
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            actualSchema = parts[0];
            actualTableName = parts[1];
        }
        
        // 构建带schema的SQL语句
        String countSql;
        if (actualSchema != null && !actualSchema.trim().isEmpty()) {
            countSql = "SELECT COUNT(*) FROM " + actualSchema + "." + actualTableName;
        } else {
            countSql = "SELECT COUNT(*) FROM " + actualTableName;
        }
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                int rowCount = rs.getInt(1);
                log.info("表 {} 行数统计: {} 行", tableName, rowCount);
                return rowCount;
            }
        }
        return 0;
    }

    /**
     * 带进度回调的数据迁移
     */
    private int migrateTableDataWithProgress(Connection sourceConn, Connection targetConn, String tableName, 
                                           List<String> columns, int totalRows, TableProgressCallback progressCallback,
                                           String sourceSchema, String targetSchema) throws SQLException {
        int migratedRows = 0;
        
        // 检查连接状态
        if (sourceConn == null || sourceConn.isClosed()) {
            throw new SQLException("源数据库连接已关闭或为空");
        }
        if (targetConn == null || targetConn.isClosed()) {
            throw new SQLException("目标数据库连接已关闭或为空");
        }
        
        // 设置连接属性以优化大数据量处理
        try {
            sourceConn.setReadOnly(true); // 源连接设为只读
            sourceConn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            targetConn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            
            // 设置网络超时
            if (sourceConn.getNetworkTimeout() == 0) {
                sourceConn.setNetworkTimeout(java.util.concurrent.Executors.newSingleThreadExecutor(), 60000); // 60秒网络超时
            }
            if (targetConn.getNetworkTimeout() == 0) {
                targetConn.setNetworkTimeout(java.util.concurrent.Executors.newSingleThreadExecutor(), 60000); // 60秒网络超时
            }
            
            log.info("连接优化设置完成，源连接只读模式: {}，事务隔离级别: {}", 
                sourceConn.isReadOnly(), sourceConn.getTransactionIsolation());
        } catch (SQLException e) {
            log.warn("设置连接属性失败，继续执行: {}", e.getMessage());
        }
        
        // 解析表名，支持schema.tableName格式
        String actualSourceSchema = sourceSchema;
        String actualTargetSchema = targetSchema;
        String actualTableName = tableName;
        
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            actualSourceSchema = parts[0];
            actualTargetSchema = parts[0];
            actualTableName = parts[1];
        }
        
        // 构建带schema的表名
        String sourceFullTableName = (actualSourceSchema != null && !actualSourceSchema.trim().isEmpty()) ? 
            actualSourceSchema + "." + actualTableName : actualTableName;
        String targetFullTableName = (actualTargetSchema != null && !actualTargetSchema.trim().isEmpty()) ? 
            actualTargetSchema + "." + actualTableName : actualTableName;
        
        // 先对目标表进行truncate操作，确保目标表为空
        try (Statement truncateStmt = targetConn.createStatement()) {
            log.info("开始清空目标表: {}", targetFullTableName);
            truncateStmt.execute("TRUNCATE TABLE " + targetFullTableName);
            log.info("目标表 {} 清空完成", targetFullTableName);
        } catch (SQLException e) {
            log.warn("目标表 {} 不支持TRUNCATE操作，尝试使用DELETE: {}", targetFullTableName, e.getMessage());
            // 如果truncate失败，尝试使用delete清空表
            try (Statement deleteStmt = targetConn.createStatement()) {
                deleteStmt.execute("DELETE FROM " + targetFullTableName);
                log.info("目标表 {} 使用DELETE清空完成", targetFullTableName);
            } catch (SQLException deleteEx) {
                log.error("清空目标表 {} 失败", targetFullTableName, deleteEx);
                throw new SQLException("无法清空目标表 " + targetFullTableName + ": " + deleteEx.getMessage());
            }
        }
        
        // 构建列名字符串
        String columnNames = String.join(", ", columns);
        String placeholders = String.join(", ", columns.stream().map(col -> "?").toList());
        
        // 查询源数据 - 使用分批查询避免内存问题
        String selectSql = "SELECT " + columnNames + " FROM " + sourceFullTableName;
        
        // 准备插入语句
        String insertSql = "INSERT INTO " + targetFullTableName + " (" + columnNames + ") VALUES (" + placeholders + ")";
        
        log.info("开始查询源表数据: {}，预计行数: {}", sourceFullTableName, totalRows);
        
        // 分批处理，避免一次性加载大量数据
        final int QUERY_BATCH_SIZE = 10000; // 每次查询1万行
        int offset = 0;
        int totalMigratedRows = 0;
        
        while (offset < totalRows) {
            String batchSelectSql = selectSql;
            // 根据数据库类型添加分页支持
            if (isOracleDatabase(sourceConn)) {
                // Oracle 12c+ 使用 OFFSET FETCH
                if (isOracle12cOrHigher(sourceConn)) {
                    batchSelectSql = selectSql + " OFFSET " + offset + " ROWS FETCH NEXT " + QUERY_BATCH_SIZE + " ROWS ONLY";
                } else {
                    // 老版本Oracle使用ROWNUM
                    batchSelectSql = "SELECT * FROM (" + selectSql + ") WHERE ROWNUM <= " + QUERY_BATCH_SIZE + " OFFSET " + offset;
                }
            } else if (isMySQLDatabase(sourceConn)) {
                batchSelectSql = selectSql + " LIMIT " + QUERY_BATCH_SIZE + " OFFSET " + offset;
            } else if (isPostgreSQLDatabase(sourceConn)) {
                batchSelectSql = selectSql + " LIMIT " + QUERY_BATCH_SIZE + " OFFSET " + offset;
            } else if (isSQLServerDatabase(sourceConn)) {
                batchSelectSql = selectSql + " OFFSET " + offset + " ROWS FETCH NEXT " + QUERY_BATCH_SIZE + " ROWS ONLY";
            } else {
                // 对于不支持的数据库，使用简单查询但添加警告
                log.warn("数据库类型不支持分页查询，将一次性查询所有数据，可能导致内存问题");
                if (offset > 0) {
                    log.warn("跳过分页处理，直接处理所有数据");
                    break;
                }
            }
            
            log.info("开始查询第 {} 批数据，偏移量: {}，批次大小: {}", (offset / QUERY_BATCH_SIZE + 1), offset, QUERY_BATCH_SIZE);
            
            // 诊断连接状态
            try {
                log.info("源连接状态 - 只读: {}, 自动提交: {}, 事务隔离级别: {}, 网络超时: {}ms", 
                    sourceConn.isReadOnly(), sourceConn.getAutoCommit(), 
                    sourceConn.getTransactionIsolation(), sourceConn.getNetworkTimeout());
                log.info("目标连接状态 - 只读: {}, 自动提交: {}, 事务隔离级别: {}, 网络超时: {}ms", 
                    targetConn.isReadOnly(), targetConn.getAutoCommit(), 
                    targetConn.getTransactionIsolation(), targetConn.getNetworkTimeout());
            } catch (SQLException e) {
                log.warn("无法获取连接状态信息: {}", e.getMessage());
            }
            
            try (PreparedStatement selectStmt = sourceConn.prepareStatement(batchSelectSql);
                 PreparedStatement insertStmt = targetConn.prepareStatement(insertSql)) {
                
                // 设置查询超时时间（30秒）
                selectStmt.setQueryTimeout(30);
                log.info("设置查询超时时间为30秒");
                
                // 设置目标连接为手动提交模式
                targetConn.setAutoCommit(false);
                
                log.info("开始执行查询语句: {}", batchSelectSql);
                long queryStartTime = System.currentTimeMillis();
                
                try (ResultSet rs = selectStmt.executeQuery()) {
                    long queryEndTime = System.currentTimeMillis();
                    log.info("查询执行完成，耗时: {} ms，开始处理结果集", (queryEndTime - queryStartTime));
                    
                    int batchCount = 0;
                    int lastProgressUpdate = 0; // 记录上次进度更新的行数
                    int progressUpdateInterval = Math.max(1, Math.min(100, Math.min(QUERY_BATCH_SIZE, totalRows - offset) / 100)); // 动态计算进度更新间隔
                    
                    log.info("开始逐行处理数据，进度更新间隔: {} 行", progressUpdateInterval);
                    
                    while (rs.next()) {
                        // 设置插入参数
                        for (int i = 0; i < columns.size(); i++) {
                            insertStmt.setObject(i + 1, rs.getObject(i + 1));
                        }
                        
                        insertStmt.addBatch();
                        batchCount++;
                        totalMigratedRows++;
                        
                        // 更频繁地更新进度（每处理一定行数或每批次后）
                        if (totalMigratedRows - lastProgressUpdate >= progressUpdateInterval || batchCount >= BATCH_SIZE) {
                            if (progressCallback != null) {
                                double progress = (double) totalMigratedRows / totalRows * 100;
                                progressCallback.onProgress(totalMigratedRows, totalRows, progress, tableName);
                            }
                            lastProgressUpdate = totalMigratedRows;
                        }
                        
                        // 批量执行
                        if (batchCount >= BATCH_SIZE) {
                            insertStmt.executeBatch();
                            targetConn.commit();
                            log.info("表 {} 批量提交完成: 第 {} 批，本批 {} 条，累计 {} 条，进度 {}%", 
                                targetFullTableName, (totalMigratedRows / BATCH_SIZE), BATCH_SIZE, totalMigratedRows,
                                String.format("%.1f", (double) totalMigratedRows / totalRows * 100));
                            batchCount = 0;
                            
                            // 批次完成后再次更新进度
                            if (progressCallback != null) {
                                double progress = (double) totalMigratedRows / totalRows * 100;
                                progressCallback.onProgress(totalMigratedRows, totalRows, progress, tableName);
                            }
                        }
                    }
                    
                    log.info("第 {} 批结果集处理完成，本批处理行数: {}，累计处理行数: {}", (offset / QUERY_BATCH_SIZE + 1), (totalMigratedRows - offset), totalMigratedRows);
                    
                    // 执行剩余的批次
                    if (batchCount > 0) {
                        insertStmt.executeBatch();
                        targetConn.commit();
                        log.info("表 {} 最后批次提交完成: 本批 {} 条，累计 {} 条", 
                            targetFullTableName, batchCount, totalMigratedRows);
                    }
                }
                
                // 恢复自动提交
                targetConn.setAutoCommit(true);
                
            } catch (SQLException e) {
                log.error("第 {} 批数据迁移过程中发生SQL异常: {}", (offset / QUERY_BATCH_SIZE + 1), e.getMessage(), e);
                // 回滚事务
                if (targetConn != null && !targetConn.getAutoCommit()) {
                    targetConn.rollback();
                    targetConn.setAutoCommit(true);
                }
                throw e;
            } catch (Exception e) {
                log.error("第 {} 批数据迁移过程中发生未知异常: {}", (offset / QUERY_BATCH_SIZE + 1), e.getMessage(), e);
                // 回滚事务
                if (targetConn != null && !targetConn.getAutoCommit()) {
                    try {
                        targetConn.rollback();
                        targetConn.setAutoCommit(true);
                    } catch (SQLException rollbackEx) {
                        log.error("回滚事务失败", rollbackEx);
                    }
                }
                throw new RuntimeException("数据迁移失败: " + e.getMessage(), e);
            }
            
            offset += QUERY_BATCH_SIZE;
        }
        
        // 最终进度回调
        if (progressCallback != null) {
            progressCallback.onProgress(totalMigratedRows, totalRows, 100.0, tableName);
        }
        
        return totalMigratedRows;
    }

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        /**
         * 进度回调
         * @param currentRows 当前已迁移的行数
         * @param totalRows 总行数
         * @param progress 进度百分比
         */
        void onProgress(int currentRows, int totalRows, double progress);
        
        /**
         * 带表名的进度回调
         * @param currentRows 当前已迁移的行数
         * @param totalRows 总行数
         * @param progress 进度百分比
         * @param tableName 当前正在处理的表名
         */
        default void onProgress(int currentRows, int totalRows, double progress, String tableName) {
            onProgress(currentRows, totalRows, progress);
        }
    }
    
    /**
     * 带表名的进度回调接口
     */
    public interface TableProgressCallback {
        /**
         * 进度回调
         * @param currentRows 当前已迁移的行数
         * @param totalRows 总行数
         * @param progress 进度百分比
         * @param tableName 当前正在处理的表名
         */
        void onProgress(int currentRows, int totalRows, double progress, String tableName);
    }

    private AbstractDataBaseHandler getInputDataBaseHandler(DbConfig dbConfig) {
        String dbHandlerName = dbConfig.getDataBaseHandlerName();
        AbstractDataBaseHandler databaseHandler = MrSpringContextHolder.getBean(dbHandlerName, AbstractDataBaseHandler.class);
        databaseHandler.setDbConfig(dbConfig);
        return databaseHandler;
    }

    private boolean isOracleDatabase(Connection conn) {
        try {
            return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
        } catch (SQLException e) {
            log.warn("无法获取数据库产品名称，假设不是Oracle数据库", e);
            return false;
        }
    }

    private boolean isOracle12cOrHigher(Connection conn) {
        try {
            String productVersion = conn.getMetaData().getDatabaseProductVersion();
            return productVersion.toLowerCase().contains("oracle 12c") || productVersion.toLowerCase().contains("oracle 18c") || productVersion.toLowerCase().contains("oracle 19c");
        } catch (SQLException e) {
            log.warn("无法获取Oracle版本信息，假设不是Oracle 12c+数据库", e);
            return false;
        }
    }

    private boolean isMySQLDatabase(Connection conn) {
        try {
            return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (SQLException e) {
            log.warn("无法获取数据库产品名称，假设不是MySQL数据库", e);
            return false;
        }
    }

    private boolean isPostgreSQLDatabase(Connection conn) {
        try {
            return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (SQLException e) {
            log.warn("无法获取数据库产品名称，假设不是PostgreSQL数据库", e);
            return false;
        }
    }

    private boolean isSQLServerDatabase(Connection conn) {
        try {
            return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("sql server");
        } catch (SQLException e) {
            log.warn("无法获取数据库产品名称，假设不是SQL Server数据库", e);
            return false;
        }
    }

    /**
     * 测试数据库连接状态
     * @param conn 数据库连接
     * @return 连接状态信息
     */
    public String testConnectionStatus(Connection conn) {
        if (conn == null) {
            return "连接为空";
        }
        
        try {
            StringBuilder status = new StringBuilder();
            status.append("数据库产品: ").append(conn.getMetaData().getDatabaseProductName()).append("\n");
            status.append("数据库版本: ").append(conn.getMetaData().getDatabaseProductVersion()).append("\n");
            status.append("连接是否关闭: ").append(conn.isClosed()).append("\n");
            status.append("只读模式: ").append(conn.isReadOnly()).append("\n");
            status.append("自动提交: ").append(conn.getAutoCommit()).append("\n");
            status.append("事务隔离级别: ").append(conn.getTransactionIsolation()).append("\n");
            status.append("网络超时: ").append(conn.getNetworkTimeout()).append("ms\n");
            
            // 测试简单查询
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(5); // 5秒超时
                try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    if (rs.next()) {
                        status.append("简单查询测试: 成功\n");
                    } else {
                        status.append("简单查询测试: 失败\n");
                    }
                }
            } catch (SQLException e) {
                status.append("简单查询测试失败: ").append(e.getMessage()).append("\n");
            }
            
            return status.toString();
        } catch (SQLException e) {
            return "获取连接状态失败: " + e.getMessage();
        }
    }
}
