/*
 * MIT License
 *
 * Copyright (c) 2021 LeeWyatt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.domain.service.database.SqliteBackupService;
import com.murong.ecp.tools.fx.enums.BackupSetTypEnum;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class PaneKnowledgeController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button importButton;

    @FXML
    private Button exportButton;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField importFilePathField;

    @FXML
    private ComboBox<BackupSetTypEnum> exportTypeComboBox;

    @FXML
    private VBox mainContainer;

    @Autowired
    private SqliteBackupService sqliteBackupService;

    @Value("${memory.local.datasource.file}")
    private String localDbFile;

    @FXML
    void initialize() {
        // 初始化界面
        setupUI();
    }

    /**
     * 设置界面初始状态
     */
    private void setupUI() {
        // 设置状态标签初始文本
        statusLabel.setText("知识库管理 - 支持文件导入和数据库备份");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        
        // 设置按钮样式
        importButton.setStyle("-fx-background-color: #1890ff; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 16;");
        exportButton.setStyle("-fx-background-color: #52c41a; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 16;");
        
        // 设置文件路径输入框
        importFilePathField.setEditable(false);
        importFilePathField.setPromptText("请选择要导入的文件...");
        importFilePathField.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #d9d9d9; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        // 设置备份类型下拉框
        exportTypeComboBox.getItems().addAll(BackupSetTypEnum.values());
        exportTypeComboBox.setValue(BackupSetTypEnum.ALL);
        exportTypeComboBox.setStyle("-fx-background-color: white; -fx-border-color: #d9d9d9; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        // 设置下拉框显示文本
        exportTypeComboBox.setCellFactory(param -> new ListCell<BackupSetTypEnum>() {
            @Override
            protected void updateItem(BackupSetTypEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDesc());
                }
            }
        });
        
        exportTypeComboBox.setButtonCell(new ListCell<BackupSetTypEnum>() {
            @Override
            protected void updateItem(BackupSetTypEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDesc());
                }
            }
        });
    }

    /**
     * 导入文件
     */
    @FXML
    void importFile(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择要导入的SQL备份文件");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQL备份文件", "*.sql"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
            );
            
            // 设置初始目录为备份文件目录
            String backupDir = System.getProperty("user.home") + "/我的文档/草稿箱/meo备份数据";
            File backupDirectory = new File(backupDir);
            if (backupDirectory.exists() && backupDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(backupDirectory);
            } else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            }
            
            File selectedFile = fileChooser.showOpenDialog(importButton.getScene().getWindow());
            if (selectedFile == null) {
                return; // 用户取消选择
            }
            
            // 验证文件
            if (!selectedFile.exists()) {
                ViewUtils.alertForFail("选择的文件不存在！");
                return;
            }
            
            if (!selectedFile.canRead()) {
                ViewUtils.alertForFail("无法读取选择的文件，请检查文件权限！");
                return;
            }
            
            // 显示导入的文件路径
            importFilePathField.setText(selectedFile.getAbsolutePath());
            
            // 检查文件是否为SQL备份文件
            if (selectedFile.getName().toLowerCase().endsWith(".sql")) {
                // 读取文件前几行来验证是否为我们的备份文件
                try {
                    String firstLine = Files.lines(selectedFile.toPath()).findFirst().orElse("");
                    if (firstLine.contains("数据库表数据备份文件")) {
                        statusLabel.setText("SQL备份文件已选择: " + selectedFile.getName());
                        statusLabel.setStyle("-fx-text-fill: #52c41a; -fx-font-size: 14px;");
                        
                        System.out.println("选择备份文件: " + selectedFile.getAbsolutePath());
                        System.out.println("文件大小: " + formatFileSize(selectedFile.length()));
                        
                        // 询问用户是否立即导入
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                        alert.setTitle("确认导入");
                        alert.setHeaderText("SQL备份文件选择成功！");
                        alert.setContentText("文件名: " + selectedFile.getName() + 
                            "\n文件大小: " + formatFileSize(selectedFile.length()) + 
                            "\n\n是否立即导入此备份文件？\n注意：导入将覆盖现有数据！");
                        
                        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                            // 执行导入逻辑
                            importSqlBackupFile(selectedFile);
                        }
                        
                    } else {
                        statusLabel.setText("警告: 选择的文件可能不是有效的备份文件");
                        statusLabel.setStyle("-fx-text-fill: #faad14; -fx-font-size: 14px;");
                        ViewUtils.alertForFail("警告：选择的文件可能不是有效的数据库备份文件！\n请确保选择的是通过本系统导出的SQL备份文件。");
                    }
                } catch (IOException e) {
                    statusLabel.setText("文件读取失败: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
                    ViewUtils.alertForFail("文件读取失败: " + e.getMessage());
                }
            } else {
                statusLabel.setText("请选择SQL备份文件 (.sql)");
                statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
                ViewUtils.alertForFail("请选择SQL备份文件 (.sql)！\n当前选择的是: " + selectedFile.getName());
            }
            
        } catch (Exception e) {
            ViewUtils.alertForFail("导入文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 导出文件 - 备份数据库表
     */
    @FXML
    void exportFile(ActionEvent event) {
        try {
            // 选择备份保存目录
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("选择备份文件保存目录");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            
            File selectedDirectory = directoryChooser.showDialog(exportButton.getScene().getWindow());
            if (selectedDirectory == null) {
                return; // 用户取消选择
            }
            
            // 检查sqlite3命令是否可用
            if (!isSqlite3Available()) {
                ViewUtils.alertForFail("系统中未找到sqlite3命令，请先安装SQLite！");
                return;
            }
            
            // 获取当前数据库路径
            String currentDbPath = getCurrentDatabasePath();
            if (currentDbPath == null || currentDbPath.trim().isEmpty()) {
                ViewUtils.alertForFail("无法获取当前数据库路径！");
                return;
            }
            
                        // 获取选中的备份类型
            BackupSetTypEnum selectedBackupType = exportTypeComboBox.getValue();
            if (selectedBackupType == null) {
                ViewUtils.alertForFail("请选择备份类型！");
                return;
            }
            
            // 从枚举获取要备份的表列表
            List<String> tablesToBackup = selectedBackupType.getList();
            if (tablesToBackup == null || tablesToBackup.isEmpty()) {
                ViewUtils.alertForFail("选中的备份类型没有配置要备份的表！");
                return;
            }
            
            // 更新状态
            statusLabel.setText("正在备份数据库表...");
            statusLabel.setStyle("-fx-text-fill: #1890ff; -fx-font-size: 14px;");
            
            // 调用SqliteBackupService进行合并备份
            boolean success = backupAllTablesToSingleFile(currentDbPath, tablesToBackup, selectedDirectory.getAbsolutePath());
            int successCount = success ? tablesToBackup.size() : 0;
            
            if (successCount > 0) {
                // 更新状态
                statusLabel.setText("成功备份 " + successCount + " 个表");
                statusLabel.setStyle("-fx-text-fill: #52c41a; -fx-font-size: 14px;");
                
                ViewUtils.alertForSucess("数据库表备份成功！\n" +
                    "备份类型: " + selectedBackupType.getDesc() + "\n" +
                    "备份目录: " + selectedDirectory.getAbsolutePath() + "\n" +
                    "成功备份表数量: " + successCount + "\n" +
                    "备份的表: " + String.join(", ", tablesToBackup));
            } else {
                ViewUtils.alertForFail("备份失败，没有成功备份任何表！");
                statusLabel.setText("备份失败");
                statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
            }
            
        } catch (Exception e) {
            ViewUtils.alertForFail("导出文件失败: " + e.getMessage());
            statusLabel.setText("备份失败: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
            e.printStackTrace();
        }
    }

    /**
     * 备份所有表到单个文件（只包含数据，不包含建表语句）
     */
    private boolean backupAllTablesToSingleFile(String dbPath, List<String> tableNames, String backupDir) {
        try {
            String backupName = "all_tables_data_backup_" + System.currentTimeMillis() + ".sql";
            String outputPath = backupDir + File.separator + backupName;
            
            System.out.println("[DEBUG] 开始合并备份所有表数据");
            System.out.println("[DEBUG] 数据库路径: " + dbPath);
            System.out.println("[DEBUG] 输出路径: " + outputPath);
            System.out.println("[DEBUG] 要备份的表: " + String.join(", ", tableNames));
            
            // 创建输出文件
            File outputFile = new File(outputPath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                // 写入文件头注释
                writer.write("-- 数据库表数据备份文件\n");
                writer.write("-- 生成时间: " + new java.util.Date() + "\n");
                writer.write("-- 包含的表: " + String.join(", ", tableNames) + "\n");
                writer.write("-- 注意: 此文件只包含数据，不包含建表语句\n\n");
                
                // 为每个表导出数据
                for (String tableName : tableNames) {
                    System.out.println("[DEBUG] 正在备份表: " + tableName);
                    
                    // 获取表的字段列表
                    List<String> columns = getTableColumns(dbPath, tableName);
                    if (columns.isEmpty()) {
                        System.out.println("[DEBUG] 表 " + tableName + " 没有字段或不存在，跳过");
                        continue;
                    }
                    
                    // 写入表数据开始标记
                    writer.write("-- ==========================================\n");
                    writer.write("-- 表: " + tableName + "\n");
                    writer.write("-- 字段: " + String.join(", ", columns) + "\n");
                    writer.write("-- ==========================================\n\n");
                    
                    // 写入清空表数据的SQL
                    writer.write("-- 清空表数据\n");
                    writer.write("DELETE FROM " + tableName + ";\n\n");
                    
                    // 导出表数据
                    boolean tableSuccess = exportTableDataOnly(dbPath, tableName, writer);
                    if (tableSuccess) {
                        System.out.println("[DEBUG] 表 " + tableName + " 数据导出成功");
                    } else {
                        System.out.println("[DEBUG] 表 " + tableName + " 数据导出失败");
                    }
                    
                    writer.write("\n");
                }
                
                // 写入文件尾注释
                writer.write("-- 备份完成\n");
            }
            
            // 检查输出文件
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.println("[DEBUG] 合并备份文件创建成功: " + outputPath + ", 大小: " + outputFile.length() + " bytes");
                return true;
            } else {
                System.out.println("[DEBUG] 合并备份文件创建失败或为空: " + outputPath);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[DEBUG] 合并备份时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取表的字段列表
     */
    private List<String> getTableColumns(String dbPath, String tableName) {
        List<String> columns = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sqlite3", dbPath, "PRAGMA table_info(" + tableName + ");");
            
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
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
            System.err.println("[DEBUG] 获取表字段时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        return columns;
    }

    /**
     * 导出表数据（只包含INSERT语句，不包含建表语句）
     */
    private boolean exportTableDataOnly(String dbPath, String tableName, BufferedWriter writer) {
        try {
            // 获取表的字段列表
            List<String> columns = getTableColumns(dbPath, tableName);
            if (columns.isEmpty()) {
                System.err.println("[DEBUG] 无法获取表 " + tableName + " 的字段列表");
                return false;
            }
            
            // 使用list模式而不是csv模式，这样可以更好地处理包含特殊字符的JSON数据
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sqlite3", dbPath, ".mode list", "SELECT * FROM " + tableName + ";");
            
            Process process = processBuilder.start();
            
            // 读取输出并转换为带字段名的INSERT语句
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        // 使用|作为分隔符解析list模式的输出
                        String[] values = line.split("\\|", -1); // -1表示保留空字段
                        
                        if (values.length == columns.size()) {
                            // 构建带字段名的INSERT语句
                            StringBuilder insertSql = new StringBuilder();
                            insertSql.append("INSERT INTO ").append(tableName).append(" (");
                            insertSql.append(String.join(", ", columns));
                            insertSql.append(") VALUES (");
                            
                            // 添加值，处理NULL和特殊字符
                            for (int i = 0; i < values.length; i++) {
                                if (i > 0) insertSql.append(", ");
                                
                                String value = values[i];
                                if (value == null || value.equals("")) {
                                    insertSql.append("NULL");
                                } else {
                                    // 转义单引号并添加引号，保持JSON格式完整
                                    String escapedValue = value.replace("'", "''");
                                    insertSql.append("'").append(escapedValue).append("'");
                                }
                            }
                            insertSql.append(");");
                            
                            writer.write(insertSql.toString() + "\n");
                        } else {
                            System.err.println("[WARN] 字段数量不匹配，期望: " + columns.size() + "，实际: " + values.length);
                        }
                    }
                }
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
            
        } catch (Exception e) {
            System.err.println("[DEBUG] 导出表数据时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 检查sqlite3命令是否可用
     */
    private boolean isSqlite3Available() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sqlite3", "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("[DEBUG] sqlite3命令检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前数据库路径
     */
    private String getCurrentDatabasePath() {
        try {
            if (localDbFile == null || localDbFile.isBlank()) {
                System.out.println("[DEBUG] 未配置本地数据库路径");
                return null;
            }
            File dbFile = new File(localDbFile);
            System.out.println("[DEBUG] 检查数据库路径: " + dbFile.getAbsolutePath());
            if (dbFile.exists()) {
                System.out.println("[DEBUG] 数据库文件存在: " + dbFile.getAbsolutePath());
                return dbFile.getAbsolutePath();
            }
            System.out.println("[DEBUG] 数据库文件不存在: " + dbFile.getAbsolutePath());
            return null;
        } catch (Exception e) {
            System.err.println("[DEBUG] 获取数据库路径时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 导入SQL备份文件
     */
    private void importSqlBackupFile(File backupFile) {
        try {
            statusLabel.setText("正在导入备份文件...");
            statusLabel.setStyle("-fx-text-fill: #1890ff; -fx-font-size: 14px;");
            
            // 获取当前数据库路径
            String currentDbPath = getCurrentDatabasePath();
            if (currentDbPath == null) {
                ViewUtils.alertForFail("无法获取当前数据库路径！");
                return;
            }
            
            System.out.println("[DEBUG] 开始导入备份文件: " + backupFile.getAbsolutePath());
            System.out.println("[DEBUG] 目标数据库: " + currentDbPath);
            
            // 验证备份文件格式
            if (!validateBackupFile(backupFile)) {
                ViewUtils.alertForFail("备份文件格式验证失败！\n请确保文件是有效的SQL备份文件。");
                return;
            }
            
            // 调用SqliteBackupService进行导入
            boolean importSuccess = sqliteBackupService.importFromSqlScript(currentDbPath, backupFile.getAbsolutePath());
            
            if (importSuccess) {
                statusLabel.setText("备份文件导入成功！");
                statusLabel.setStyle("-fx-text-fill: #52c41a; -fx-font-size: 14px;");
                
                ViewUtils.alertForSucess("备份文件导入成功！\n" +
                    "文件名: " + backupFile.getName() + "\n" +
                    "导入时间: " + new java.util.Date() + "\n" +
                    "数据库: " + currentDbPath);
                
                System.out.println("[DEBUG] 备份文件导入成功");
                
            } else {
                statusLabel.setText("备份文件导入失败！");
                statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
                
                ViewUtils.alertForFail("备份文件导入失败！\n请检查文件格式和数据库连接。");
                
                System.out.println("[DEBUG] 备份文件导入失败");
            }
            
        } catch (Exception e) {
            statusLabel.setText("导入过程中发生错误！");
            statusLabel.setStyle("-fx-text-fill: #ff4d4f; -fx-font-size: 14px;");
            
            ViewUtils.alertForFail("导入过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证备份文件格式
     */
    private boolean validateBackupFile(File backupFile) {
        try {
            // 读取文件前几行进行验证
            try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                String firstLine = reader.readLine();
                if (firstLine == null || !firstLine.contains("数据库表数据备份文件")) {
                    System.err.println("[ERROR] 文件不是有效的备份文件: " + firstLine);
                    return false;
                }
                
                // 检查是否包含必要的SQL语句
                StringBuilder content = new StringBuilder();
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 50) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                
                String contentStr = content.toString();
                if (!contentStr.contains("DELETE FROM") || !contentStr.contains("INSERT INTO")) {
                    System.err.println("[ERROR] 文件不包含必要的SQL语句");
                    return false;
                }
                
                System.out.println("[DEBUG] 备份文件格式验证通过");
                return true;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 验证备份文件时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 