package com.murong.ecp.tools.fx.infrastructure.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * 数据库配置窗口控制器
 */
@Component
public class DatabaseConfigDialogController {

    @FXML
    private TextField dbDriverNameField;

    @FXML
    private TextField dbUrlField;

    @FXML
    private TextField dbUsernameField;

    @FXML
    private PasswordField dbPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button testConnectionButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @Autowired(required = false)
    private LocalSettingDao localSettingDao;
    
    @Autowired(required = false)
    private GlobalProperties globalProperties;

    private Stage dialogStage;
    private boolean configSuccess = false;

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // 设置焦点
        Platform.runLater(() -> dbDriverNameField.requestFocus());
        
        // 加载现有配置
        loadExistingConfig();
    }

    /**
     * 加载现有配置
     */
    private void loadExistingConfig() {
        try {
            if (localSettingDao != null) {
                // 查询状态为Y的记录
                List<LocalSettingPO> resultList = localSettingDao.queryActiveSettings();
                
                if (resultList != null && !resultList.isEmpty()) {
                    LocalSettingPO firstRecord = resultList.get(0);
                    dbDriverNameField.setText(firstRecord.getDbDriverName());
                    dbUrlField.setText(firstRecord.getDbUrl());
                    dbUsernameField.setText(firstRecord.getDbUsrName());
                    dbPasswordField.setText(firstRecord.getDbPassWord());
                    return;
                }
            }
            
            // 尝试从本地配置文件加载
            if (loadConfigFromFile()) {
                return;
            }
            
            // 设置默认值
            dbDriverNameField.setText("org.postgresql.Driver");
            dbUrlField.setText("jdbc:postgresql://localhost:5432/memory_db");
            dbUsernameField.setText("postgres");
        } catch (Exception e) {
            System.err.println("加载现有配置失败: " + e.getMessage());
            // 设置默认值
            dbDriverNameField.setText("org.postgresql.Driver");
            dbUrlField.setText("jdbc:postgresql://localhost:5432/memory_db");
            dbUsernameField.setText("postgres");
        }
    }

    /**
     * 从本地配置文件加载配置
     */
    private boolean loadConfigFromFile() {
        try {
            String userHome = System.getProperty("user.home");
            Path configFile = Paths.get(userHome, ".memory", "database.properties");
            
            if (Files.exists(configFile)) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                    props.load(fis);
                    
                    String driver = props.getProperty("db.driver");
                    String url = props.getProperty("db.url");
                    String username = props.getProperty("db.username");
                    String password = props.getProperty("db.password");
                    
                    if (driver != null && url != null && username != null) {
                        dbDriverNameField.setText(driver);
                        dbUrlField.setText(url);
                        dbUsernameField.setText(username);
                        if (password != null) {
                            dbPasswordField.setText(password);
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("从配置文件加载配置失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 处理测试连接按钮点击
     */
    @FXML
    private void handleTestConnection() {
        String driverName = dbDriverNameField.getText();
        String url = dbUrlField.getText();
        String username = dbUsernameField.getText();
        String password = dbPasswordField.getText();

        // 清空之前的消息
        messageLabel.setText("");
        messageLabel.setStyle("");

        // 验证输入
        if (driverName == null || driverName.trim().isEmpty()) {
            showMessage("请输入数据库驱动名称", true);
            dbDriverNameField.requestFocus();
            return;
        }

        if (url == null || url.trim().isEmpty()) {
            showMessage("请输入数据库连接URL", true);
            dbUrlField.requestFocus();
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            showMessage("请输入数据库用户名", true);
            dbUsernameField.requestFocus();
            return;
        }

        // 禁用测试按钮，防止重复点击
        testConnectionButton.setDisable(true);
        testConnectionButton.setText("测试中...");

        try {
            // 测试数据库连接
            testDatabaseConnection(driverName, url, username, password);
            showMessage("数据库连接测试成功！", false);
        } catch (Exception e) {
            showMessage("数据库连接测试失败: " + e.getMessage(), true);
            System.err.println("数据库连接测试异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 恢复测试按钮状态
            testConnectionButton.setDisable(false);
            testConnectionButton.setText("测试连接");
        }
    }

    /**
     * 测试数据库连接
     */
    private void testDatabaseConnection(String driverName, String url, String username, String password) throws Exception {
        Connection connection = null;
        try {
            // 加载驱动
            Class.forName(driverName);
            
            // 创建连接
            connection = DriverManager.getConnection(url, username, password);
            
            // 测试查询
            connection.createStatement().executeQuery("SELECT 1");
            
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // 忽略关闭连接时的异常
                }
            }
        }
    }

    /**
     * 处理保存按钮点击
     */
    @FXML
    private void handleSave() {
        String driverName = dbDriverNameField.getText();
        String url = dbUrlField.getText();
        String username = dbUsernameField.getText();
        String password = dbPasswordField.getText();

        // 清空之前的消息
        messageLabel.setText("");
        messageLabel.setStyle("");

        // 验证输入
        if (driverName == null || driverName.trim().isEmpty()) {
            showMessage("请输入数据库驱动名称", true);
            dbDriverNameField.requestFocus();
            return;
        }

        if (url == null || url.trim().isEmpty()) {
            showMessage("请输入数据库连接URL", true);
            dbUrlField.requestFocus();
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            showMessage("请输入数据库用户名", true);
            dbUsernameField.requestFocus();
            return;
        }

        // 禁用保存按钮，防止重复点击
        saveButton.setDisable(true);
        saveButton.setText("保存中...");

        try {
            // 先测试连接
            testDatabaseConnection(driverName, url, username, password);
            
            // 保存配置
            saveDatabaseConfig(driverName, url, username, password);
            
            showMessage("数据库配置保存成功！", false);
            configSuccess = true;
            
            // 延迟关闭窗口，让用户看到成功消息
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                dialogStage.close();
            });
            
        } catch (Exception e) {
            showMessage("保存配置失败: " + e.getMessage(), true);
            System.err.println("保存配置异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 恢复保存按钮状态
            saveButton.setDisable(false);
            saveButton.setText("保存");
        }
    }

    /**
     * 保存数据库配置
     */
    private void saveDatabaseConfig(String driverName, String url, String username, String password) throws Exception {
        if (localSettingDao == null) {
            // 如果没有Spring容器，保存到本地配置文件
            saveConfigToFile(driverName, url, username, password);
            return;
        }
        
        // 获取表单数据
        LocalSettingPO savePO = new LocalSettingPO();
        savePO.setDbDriverName(driverName.trim());
        savePO.setDbUrl(url.trim());
        savePO.setDbUsrName(username.trim());
        savePO.setDbPassWord(password.trim());
        savePO.setStatus(FlgEnum.YES.getValue());
        
        // 设置更新信息
        if (globalProperties != null && globalProperties.getOperator() != null) {
            savePO.setUpdateBy(globalProperties.getOperator().getUsername());
        } else {
            savePO.setUpdateBy("system");
        }
        savePO.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 查询现有记录
        List<LocalSettingPO> existingRecords = localSettingDao.queryActiveSettings();
        
        if (existingRecords != null && !existingRecords.isEmpty()) {
            // 更新现有记录
            LocalSettingPO wherePO = new LocalSettingPO();
            wherePO.setStatus(FlgEnum.YES.getValue());
            localSettingDao.updateByOne(savePO, wherePO);
        } else {
            // 新增记录
            localSettingDao.save(savePO);
        }
    }

    /**
     * 保存配置到本地文件
     */
    private void saveConfigToFile(String driverName, String url, String username, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("db.driver", driverName.trim());
        props.setProperty("db.url", url.trim());
        props.setProperty("db.username", username.trim());
        props.setProperty("db.password", password.trim());
        props.setProperty("db.status", "Y");
        props.setProperty("update.time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 保存到用户目录下的配置文件
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".memory");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("database.properties");
        
        try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
            props.store(fos, "Database Configuration");
        }
        
        System.out.println("数据库配置已保存到: " + configFile);
    }

    /**
     * 处理取消按钮点击
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * 显示消息
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: red;");
        } else {
            messageLabel.setStyle("-fx-text-fill: green;");
        }
    }

    /**
     * 获取配置是否成功
     */
    public boolean isConfigSuccess() {
        return configSuccess;
    }
}
