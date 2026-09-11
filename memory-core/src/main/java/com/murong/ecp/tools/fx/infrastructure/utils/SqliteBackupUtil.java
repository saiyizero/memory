package com.murong.ecp.tools.fx.infrastructure.utils;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite备份工具类
 */
@Component
public class SqliteBackupUtil {

    /**
     * 使用SQL脚本导入数据
     * @param dbPath 数据库路径
     * @param sqlPath SQL脚本路径
     * @return 是否成功
     */
    public boolean importFromSqlScript(String dbPath, String sqlPath) {
        try {
            System.out.println("[DEBUG] 开始导入SQL脚本: " + sqlPath);
            System.out.println("[DEBUG] 目标数据库: " + dbPath);
            
            // 验证SQL文件是否存在
            File sqlFile = new File(sqlPath);
            if (!sqlFile.exists()) {
                System.err.println("[ERROR] SQL文件不存在: " + sqlPath);
                return false;
            }
            
            // 逐行读取SQL文件并执行
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
                String line;
                int lineNumber = 0;
                int successCount = 0;
                int errorCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    
                    // 跳过注释行和空行
                    if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                        continue;
                    }
                    
                    // 执行SQL语句
                    if (line.startsWith("DELETE FROM") || line.startsWith("INSERT INTO")) {
                        boolean success = executeSingleSql(dbPath, line);
                        if (success) {
                            successCount++;
                        } else {
                            errorCount++;
                            System.err.println("[ERROR] 第" + lineNumber + "行SQL执行失败: " + line.substring(0, Math.min(100, line.length())) + "...");
                        }
                    }
                }
                
                System.out.println("[DEBUG] SQL脚本导入完成，成功: " + successCount + "，失败: " + errorCount);
                return errorCount == 0;
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR] 导入SQL脚本时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 执行单个SQL语句
     */
    private boolean executeSingleSql(String dbPath, String sql) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sqlite3", dbPath, sql);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        System.out.println("[DEBUG] sqlite3输出: " + line);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
            
        } catch (Exception e) {
            System.err.println("[ERROR] 执行SQL语句时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取表的字段列表
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @return 字段列表
     */
    public List<String> getTableColumns(String dbPath, String tableName) {
        List<String> columns = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sqlite3", dbPath,
                "PRAGMA table_info(" + tableName + ");");
            
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        columns.add(parts[1].trim()); // 字段名在第二列
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return columns;
    }
} 