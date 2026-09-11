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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

@Component
public class DatabaseConfigDialogController {

    @FXML
    private TextField dbUrlField;

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

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        Platform.runLater(() -> dbUrlField.requestFocus());
        loadExistingConfig();
    }

    private void loadExistingConfig() {
        try {
            if (localSettingDao != null) {
                List<LocalSettingPO> resultList = localSettingDao.queryActiveSettings();
                if (resultList != null && !resultList.isEmpty()) {
                    LocalSettingPO firstRecord = resultList.get(0);
                    if (firstRecord.getDbUrl() != null && firstRecord.getDbUrl().startsWith("http")) {
                        dbUrlField.setText(firstRecord.getDbUrl());
                        return;
                    }
                }
            }
            if (loadConfigFromFile()) {
                return;
            }
            dbUrlField.setText("http://localhost:8080");
        } catch (Exception e) {
            System.err.println("加载现有配置失败: " + e.getMessage());
            dbUrlField.setText("http://localhost:8080");
        }
    }

    private boolean loadConfigFromFile() {
        try {
            String userHome = System.getProperty("user.home");
            Path configFile = Paths.get(userHome, ".memory", "database.properties");
            if (Files.exists(configFile)) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                    props.load(fis);
                    String url = props.getProperty("memory.service.url", props.getProperty("db.url"));
                    if (url != null && url.startsWith("http")) {
                        dbUrlField.setText(url);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("从配置文件加载配置失败: " + e.getMessage());
        }
        return false;
    }

    @FXML
    private void handleTestConnection() {
        String url = dbUrlField.getText();
        messageLabel.setText("");
        messageLabel.setStyle("");
        if (url == null || url.trim().isEmpty()) {
            showMessage("请输入 memory-service 地址", true);
            dbUrlField.requestFocus();
            return;
        }
        testConnectionButton.setDisable(true);
        testConnectionButton.setText("测试中...");
        try {
            testServiceConnection(url.trim());
            showMessage("服务连接测试成功！", false);
        } catch (Exception e) {
            showMessage("服务连接测试失败: " + e.getMessage(), true);
        } finally {
            testConnectionButton.setDisable(false);
            testConnectionButton.setText("测试连接");
        }
    }

    private void testServiceConnection(String url) throws Exception {
        String healthUrl = url.endsWith("/") ? url + "api/health" : url + "/api/health";
        HttpURLConnection connection = (HttpURLConnection) URI.create(healthUrl).toURL().openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code);
        }
    }

    @FXML
    private void handleSave() {
        String url = dbUrlField.getText();
        messageLabel.setText("");
        messageLabel.setStyle("");
        if (url == null || url.trim().isEmpty()) {
            showMessage("请输入 memory-service 地址", true);
            dbUrlField.requestFocus();
            return;
        }
        saveButton.setDisable(true);
        saveButton.setText("保存中...");
        try {
            testServiceConnection(url.trim());
            saveDatabaseConfig(url.trim());
            showMessage("服务地址保存成功！", false);
            configSuccess = true;
            Platform.runLater(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                dialogStage.close();
            });
        } catch (Exception e) {
            showMessage("保存配置失败: " + e.getMessage(), true);
        } finally {
            saveButton.setDisable(false);
            saveButton.setText("保存配置");
        }
    }

    private void saveDatabaseConfig(String url) throws Exception {
        saveConfigToFile(url);
        if (localSettingDao == null) {
            return;
        }
        LocalSettingPO savePO = new LocalSettingPO();
        savePO.setDbUrl(url);
        savePO.setStatus(FlgEnum.YES.getValue());
        if (globalProperties != null && globalProperties.getOperator() != null) {
            savePO.setUpdateBy(globalProperties.getOperator().getUsername());
        } else {
            savePO.setUpdateBy("system");
        }
        savePO.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        List<LocalSettingPO> existingRecords = localSettingDao.queryActiveSettings();
        if (existingRecords != null && !existingRecords.isEmpty()) {
            LocalSettingPO wherePO = new LocalSettingPO();
            wherePO.setStatus(FlgEnum.YES.getValue());
            localSettingDao.updateByOne(savePO, wherePO);
        } else {
            localSettingDao.save(savePO);
        }
    }

    private void saveConfigToFile(String url) throws Exception {
        Properties props = new Properties();
        props.setProperty("memory.service.url", url);
        props.setProperty("db.url", url);
        props.setProperty("db.status", "Y");
        props.setProperty("update.time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Path configDir = Paths.get(System.getProperty("user.home"), ".memory");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("database.properties");
        try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
            props.store(fos, "Memory Service Configuration");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    public boolean isConfigSuccess() {
        return configSuccess;
    }
}
