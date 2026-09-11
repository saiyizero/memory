package com.murong.ecp.tools.fx.infrastructure.license;

import com.murong.ecp.tools.fx.infrastructure.license.model.LicenseValidationResult;
import com.murong.ecp.tools.fx.infrastructure.view.NativeSplashScreen;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 许可证验证器
 * 负责在应用启动时验证许可证
 */
@Component
public class LicenseValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(LicenseValidator.class);
    
    @Autowired
    private LicenseManager licenseManager;
    
    @Autowired
    private DevelopmentLicenseBypass developmentBypass;
    
    @Value("${license.enabled:true}")
    private boolean licenseEnabled;
    
    /**
     * 验证许可证
     * 如果验证失败，会显示对话框让用户输入许可证
     */
    public boolean validateLicense(Stage primaryStage) {
        try {
            logger.info("=== 开始许可证验证 ===");
            logger.info("许可证验证启用状态: {}", licenseEnabled);
            logger.info("开发模式状态: {}", developmentBypass.isDevelopmentMode());
            
            // 检查许可证验证是否启用
            if (!licenseEnabled) {
                logger.warn("许可证验证已禁用，跳过验证");
                return true;
            }
            
            // 检查是否为开发模式
            if (developmentBypass.isDevelopmentMode()) {
                logger.info("开发模式：跳过许可证验证");
                return true;
            }
            
            logger.info("生产模式：执行许可证验证");
            
            // 1. 验证许可证
            LicenseValidationResult result = licenseManager.validateLicense();
            
            if (result.isValid()) {
                logger.info("许可证验证成功: {}", result.getLicenseInfo());
                return true;
            }
            
            // 2. 许可证无效，显示错误对话框
            logger.warn("许可证验证失败: {}", result.getMessage());
            
            // 3. 在显示许可证输入对话框之前，确保启动动画被隐藏
            // 这样可以避免启动动画和许可证验证弹出框同时显示
            hideSplashScreen();
            
            // 4. 显示许可证输入对话框
            return showLicenseDialog(primaryStage, result.getMessage());
            
        } catch (Exception e) {
            logger.error("许可证验证异常", e);
            // 异常情况下也要确保启动动画被隐藏
            hideSplashScreen();
            return showLicenseDialog(primaryStage, "许可证验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 显示许可证输入对话框
     */
    private boolean showLicenseDialog(Stage primaryStage, String errorMessage) {
        try {
            // 确保启动动画被隐藏
            hideSplashScreen();
            
            // 显示错误信息
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("许可证验证失败");
            alert.setHeaderText("许可证验证失败");
            alert.setContentText(errorMessage + "\n\n请选择许可证文件或输入许可证内容。");
            
            // 添加按钮
            ButtonType fileButton = new ButtonType("选择许可证文件");
            ButtonType inputButton = new ButtonType("输入许可证内容");
            ButtonType exitButton = new ButtonType("退出");
            
            alert.getButtonTypes().setAll(fileButton, inputButton, exitButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == fileButton) {
                    return selectLicenseFile(primaryStage);
                } else if (result.get() == inputButton) {
                    return inputLicenseContent(primaryStage);
                } else {
                    // 用户选择退出
                    Platform.exit();
                    return false;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("显示许可证对话框失败", e);
            Platform.exit();
            return false;
        }
    }
    
    /**
     * 选择许可证文件
     */
    private boolean selectLicenseFile(Stage primaryStage) {
        try {
            // 确保启动动画被隐藏
            hideSplashScreen();
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择许可证文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("许可证文件", "*.license", "*.txt", "*.*")
            );
            
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                // 读取文件内容
                String licenseContent = new String(Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath())));
                
                // 安装许可证
                if (licenseManager.installLicense(licenseContent)) {
                    showSuccessDialog("许可证安装成功");
                    return true;
                } else {
                    showErrorDialog("许可证安装失败，请检查许可证文件是否正确");
                    return showLicenseDialog(primaryStage, "许可证安装失败");
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("选择许可证文件失败", e);
            showErrorDialog("读取许可证文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 输入许可证内容
     */
    private boolean inputLicenseContent(Stage primaryStage) {
        try {
            // 确保启动动画被隐藏
            hideSplashScreen();
            
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("输入许可证");
            dialog.setHeaderText("请输入许可证内容");
            dialog.setContentText("许可证内容:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String licenseContent = result.get().trim();
                
                // 安装许可证
                if (licenseManager.installLicense(licenseContent)) {
                    showSuccessDialog("许可证安装成功");
                    return true;
                } else {
                    showErrorDialog("许可证安装失败，请检查许可证内容是否正确");
                    return showLicenseDialog(primaryStage, "许可证安装失败");
                }
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("输入许可证内容失败", e);
            showErrorDialog("处理许可证内容失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 显示成功对话框
     */
    private void showSuccessDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText("操作成功");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("操作失败");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 隐藏启动动画
     */
    private void hideSplashScreen() {
        try {
            NativeSplashScreen.hide();
            logger.info("启动动画已隐藏");
        } catch (Exception e) {
            logger.warn("隐藏启动动画失败: {}", e.getMessage());
        }
    }

} 