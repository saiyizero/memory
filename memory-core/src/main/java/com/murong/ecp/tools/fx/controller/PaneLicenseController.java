package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.infrastructure.license.LicenseManager;
import com.murong.ecp.tools.fx.infrastructure.license.LicenseValidator;
import com.murong.ecp.tools.fx.infrastructure.license.model.LicenseInfo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 许可证管理界面控制器
 */
@Component
public class PaneLicenseController {

    @FXML
    private VBox licensePane;
    
    @FXML
    private Label lblCompanyName;
    
    @FXML
    private Label lblProductName;
    
    @FXML
    private Label lblVersion;
    
    @FXML
    private Label lblExpireDate;
    
    @FXML
    private Label lblFeatures;
    
    @FXML
    private Label lblMinVersion;
    
    @FXML
    private Label lblMaxVersion;
    
    @FXML
    private Label lblLicenseStatus;
    
    @FXML
    private Button btnInstallLicense;
    
    @FXML
    private Button btnShowCurrentVersion;
    
    @FXML
    private Button btnRefreshLicense;
    
    @Autowired
    private LicenseManager licenseManager;
    
    @Autowired
    private LicenseValidator licenseValidator;
    
    @FXML
    public void initialize() {
        refreshLicenseInfo();
    }
    
    /**
     * 刷新许可证信息
     */
    @FXML
    public void refreshLicenseInfo() {
        try {
            LicenseInfo licenseInfo = licenseManager.getCurrentLicense();
            
            if (licenseInfo != null) {
                // 显示许可证信息
                lblCompanyName.setText(licenseInfo.getCompanyName());
                lblProductName.setText(licenseInfo.getProductName());
                lblVersion.setText(licenseInfo.getVersion());
                lblExpireDate.setText(licenseInfo.getExpireDate());
                lblFeatures.setText(licenseInfo.getFeatures());
                lblMinVersion.setText(licenseInfo.getMinVersion());
                lblMaxVersion.setText(licenseInfo.getMaxVersion());
                
                // 检查许可证状态
                if (licenseManager.isLicenseValid()) {
                    lblLicenseStatus.setText("有效");
                    lblLicenseStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    lblLicenseStatus.setText("无效");
                    lblLicenseStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
                
                btnInstallLicense.setText("重新安装许可证");
            } else {
                // 没有许可证
                lblCompanyName.setText("未安装");
                lblProductName.setText("未安装");
                lblVersion.setText("未安装");
                lblExpireDate.setText("未安装");
                lblFeatures.setText("未安装");
                lblMinVersion.setText("未安装");
                lblMaxVersion.setText("未安装");
                lblLicenseStatus.setText("未安装");
                lblLicenseStatus.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                
                btnInstallLicense.setText("安装许可证");
            }
            
        } catch (Exception e) {
            showError("刷新许可证信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 安装许可证
     */
    @FXML
    public void installLicense() {
        try {
            Stage stage = (Stage) licensePane.getScene().getWindow();
            
            // 显示文件选择对话框
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择许可证文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("许可证文件", "*.license", "*.txt", "*.*")
            );
            
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                // 读取文件内容
                String licenseContent = new String(Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath())));
                
                // 安装许可证
                if (licenseManager.installLicense(licenseContent)) {
                    showInfo("许可证安装成功");
                    refreshLicenseInfo();
                } else {
                    showError("许可证安装失败，请检查许可证文件是否正确");
                }
            }
            
        } catch (Exception e) {
            showError("安装许可证失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示当前版本
     */
    @FXML
    public void showCurrentVersion() {
        try {
            String currentVersion = licenseManager.getCurrentVersion();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("当前版本");
            alert.setHeaderText("应用当前版本");
            alert.setContentText("当前版本: " + currentVersion);
            alert.showAndWait();
        } catch (Exception e) {
            showError("获取当前版本失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示信息对话框
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("信息");
        alert.setHeaderText("操作成功");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 显示错误对话框
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText("操作失败");
        alert.setContentText(message);
        alert.showAndWait();
    }
} 