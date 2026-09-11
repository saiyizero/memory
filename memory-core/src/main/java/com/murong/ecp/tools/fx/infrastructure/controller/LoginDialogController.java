package com.murong.ecp.tools.fx.infrastructure.controller;

import com.murong.ecp.tools.fx.domain.service.auth.LoginService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录窗口控制器
 */
@Component
public class LoginDialogController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button cancelButton;

    @Autowired
    private LoginService loginService;

    private Stage dialogStage;
    private boolean loginSuccess = false;

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // 设置回车键登录
        passwordField.setOnAction(event -> handleLogin());
        
        // 设置焦点
        Platform.runLater(() -> usernameField.requestFocus());
    }

    /**
     * 处理登录按钮点击
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 清空之前的消息
        messageLabel.setText("");
        messageLabel.setStyle("");

        // 验证输入
        if (username == null || username.trim().isEmpty()) {
            showMessage("请输入用户名", true);
            usernameField.requestFocus();
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            showMessage("请输入密码", true);
            passwordField.requestFocus();
            return;
        }

        // 禁用登录按钮，防止重复点击
        loginButton.setDisable(true);
        loginButton.setText("登录中...");

        try {
            // 验证登录
            CrResult crResult = loginService.validateLogin(username, password);
            if (crResult.isSucess()) {
                showMessage("登录成功！", false);
                loginSuccess = true;
                
                // 延迟关闭窗口，让用户看到成功消息
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    dialogStage.close();
                });
            } else {
                showMessage(crResult.getMsgInf(), true);
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (Exception e) {
            showMessage("登录验证失败: " + e.getMessage(), true);
            System.err.println("登录验证异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 恢复登录按钮状态
            loginButton.setDisable(false);
            loginButton.setText("登录");
        }
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
            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            messageLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    /**
     * 获取登录是否成功
     */
    public boolean isLoginSuccess() {
        return loginSuccess;
    }
}
