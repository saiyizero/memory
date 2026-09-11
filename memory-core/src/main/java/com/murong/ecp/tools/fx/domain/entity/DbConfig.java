package com.murong.ecp.tools.fx.domain.entity;

import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import lombok.Data;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Data
public class DbConfig {
    Connection conn;
    String driver;
    String url;
    String username;
    String password;
    String schema;

    public DbConfig(DbConnectionPO dbConnPO) {
        this.driver = dbConnPO.getDriver();
        this.url = dbConnPO.getJdbcUrl();
        this.username = dbConnPO.getUsername();
        this.password = dbConnPO.getPassword();
        this.schema = dbConnPO.getSchemaNm();
    }
    public DbConfig(String driver, String url, String username, String password,String schema) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
        this.schema = schema;
    }

    @SneakyThrows
    public Connection getEnsureConnection() {
        if (conn == null || conn.isClosed()) {
            Class.forName(this.driver);
            this.conn = DriverManager.getConnection(this.url, this.username, this.password);
        }
        return this.conn;
    }

    @SneakyThrows
    public void beginTransaction() {
        Connection connection = getEnsureConnection();
        connection.setAutoCommit(false);
    }

    @SneakyThrows
    public void commitTransaction() {
        if (conn != null && !conn.isClosed()) {
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    @SneakyThrows
    public void rollbackTransaction() {
        if (conn != null && !conn.isClosed()) {
            conn.rollback();
            conn.setAutoCommit(true);
        }
    }

    @SneakyThrows
    public Integer executeSql(String sql) {
        Integer count = 0;
        String[] sqlStatements = splitSqlStatements(sql);
        try (Statement stmt = getEnsureConnection().createStatement()) {
            for (String singleSql : sqlStatements) {
                if (singleSql.trim().isEmpty()) continue;
                System.out.println("Executing SQL: " + singleSql);
                count = count+stmt.executeUpdate(singleSql);
            }
        }catch (Exception e) {
            throw e;
        }
        return count;
    }

    @SneakyThrows
    public Integer executeSqlBatch(List<String> sqlList) {
        Integer totalCount = 0;
        try (Statement stmt = getEnsureConnection().createStatement()) {
            for (String sql : sqlList) {
                if (sql.trim().isEmpty()) continue;
                System.out.println("Executing SQL: " + sql);
                totalCount += stmt.executeUpdate(sql);
            }
        } catch (Exception e) {
            throw e;
        }
        return totalCount;
    }

    @SneakyThrows
    public String getDataBaseHandlerName(){
        if (driver.contains(".mudb.")) {
            return "muDbHandler";
        }
        DatabaseMetaData meta = getEnsureConnection().getMetaData();
        String dbName = meta.getDatabaseProductName();
        return dbName+"Handler";
    }

    // 工具方法：安全拆分SQL脚本，避免字符串和注释中的分号
    private String[] splitSqlStatements(String sqlScript) {
        // 简单实现：仅按分号拆分，忽略分号在字符串/注释内的情况
        // 如需更严谨可用正则或第三方库
        return sqlScript.split(";\\s*(?=([^']*'[^']*')*[^']*$)");
    }

    @SneakyThrows
    public List<java.util.Map<String, Object>> selectSql(String sql) {
        List<java.util.Map<String, Object>> resultList = new ArrayList<>();
        try (Statement stmt = getEnsureConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        }
        return resultList;
    }
}