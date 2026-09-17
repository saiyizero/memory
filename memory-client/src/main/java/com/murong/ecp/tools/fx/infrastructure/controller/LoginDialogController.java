package com.murong.ecp.tools.fx.infrastructure.controller;

import com.murong.ecp.tools.fx.domain.service.auth.LoginService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录窗口控制器
 */
@Component
public class LoginDialogController {

    @FXML
    private Label titleLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField realNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button registerButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private HBox realNameRow;

    @FXML
    private HBox emailRow;

    @FXML
    private HBox phoneRow;

    @FXML
    private HBox confirmPasswordRow;

    @FXML
    private HBox loginButtonRow;

    @FXML
    private HBox registerButtonRow;

    @FXML
    private Hyperlink switchModeLink;

    @Autowired
    private LoginService loginService;

    private Stage dialogStage;
    private boolean loginSuccess = false;
    private boolean registerMode = false;

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        this.loginSuccess = false;
        switchToLoginMode(false);

        passwordField.setOnAction(event -> {
            if (registerMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });
        confirmPasswordField.setOnAction(event -> handleRegister());

        Platform.runLater(() -> usernameField.requestFocus());
    }

    /**
     * 处理登录按钮点击
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        messageLabel.setText("");
        messageLabel.setStyle("");

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

        loginButton.setDisable(true);
        loginButton.setText("登录中...");

        try {
            CrResult crResult = loginService.validateLogin(username, password);
            if (crResult.isSucess()) {
                showMessage("登录成功！", false);
                loginSuccess = true;

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
            loginButton.setDisable(false);
            loginButton.setText("登录");
        }
    }

    /**
     * 处理注册按钮点击
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String realName = realNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        messageLabel.setText("");
        messageLabel.setStyle("");

        if (StringUtils.isBlank(username)) {
            showMessage("请输入用户名", true);
            usernameField.requestFocus();
            return;
        }
        if (StringUtils.isBlank(realName)) {
            showMessage("请输入真实姓名", true);
            realNameField.requestFocus();
            return;
        }
        if (StringUtils.isBlank(email)) {
            showMessage("请输入邮箱", true);
            emailField.requestFocus();
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showMessage("请输入正确的邮箱地址", true);
            emailField.requestFocus();
            return;
        }
        if (StringUtils.isBlank(phone)) {
            showMessage("请输入电话", true);
            phoneField.requestFocus();
            return;
        }
        if (StringUtils.isBlank(password)) {
            showMessage("请输入密码", true);
            passwordField.requestFocus();
            return;
        }
        if (StringUtils.isBlank(confirmPassword)) {
            showMessage("请再次输入密码", true);
            confirmPasswordField.requestFocus();
            return;
        }
        if (!StringUtils.equals(password, confirmPassword)) {
            showMessage("两次输入的密码不一致", true);
            confirmPasswordField.clear();
            confirmPasswordField.requestFocus();
            return;
        }

        registerButton.setDisable(true);
        registerButton.setText("注册中...");
        try {
            CrResult<String> crResult = loginService.register(username, password, realName, email, phone);
            if (crResult != null && crResult.isSucess()) {
                String registeredUsername = username.trim();
                switchToLoginMode(true);
                usernameField.setText(registeredUsername);
                showMessage(StringUtils.defaultIfBlank(crResult.getMsgInf(), "注册成功，请登录"), false);
                passwordField.requestFocus();
            } else {
                String msg = crResult == null ? "注册失败" : crResult.getMsgInf();
                showMessage(msg, true);
            }
        } catch (Exception e) {
            showMessage("注册失败: " + e.getMessage(), true);
            System.err.println("注册异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            registerButton.setDisable(false);
            registerButton.setText("注册");
        }
    }

    @FXML
    private void handleSwitchToRegister() {
        switchToRegisterMode();
    }

    @FXML
    private void handleSwitchToLogin() {
        switchToLoginMode(true);
    }

    /**
     * 处理取消按钮点击
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void switchToRegisterMode() {
        registerMode = true;
        titleLabel.setText("用户注册");
        if (dialogStage != null) {
            dialogStage.setTitle("用户注册");
        }
        setRowVisible(realNameRow, true);
        setRowVisible(emailRow, true);
        setRowVisible(phoneRow, true);
        setRowVisible(confirmPasswordRow, true);
        setRowVisible(loginButtonRow, false);
        setRowVisible(registerButtonRow, true);
        switchModeLink.setVisible(false);
        switchModeLink.setManaged(false);
        realNameField.clear();
        emailField.clear();
        phoneField.clear();
        confirmPasswordField.clear();
        passwordField.clear();
        messageLabel.setText("");
        messageLabel.setStyle("");
        resizeDialog();
        Platform.runLater(() -> {
            if (StringUtils.isBlank(usernameField.getText())) {
                usernameField.requestFocus();
            } else {
                realNameField.requestFocus();
            }
        });
    }

    private void switchToLoginMode(boolean clearSensitiveFields) {
        registerMode = false;
        titleLabel.setText("Memory 系统登录");
        if (dialogStage != null) {
            dialogStage.setTitle("用户登录");
        }
        setRowVisible(realNameRow, false);
        setRowVisible(emailRow, false);
        setRowVisible(phoneRow, false);
        setRowVisible(confirmPasswordRow, false);
        setRowVisible(loginButtonRow, true);
        setRowVisible(registerButtonRow, false);
        switchModeLink.setVisible(true);
        switchModeLink.setManaged(true);
        if (clearSensitiveFields) {
            passwordField.clear();
            confirmPasswordField.clear();
            realNameField.clear();
            emailField.clear();
            phoneField.clear();
        }
        messageLabel.setText("");
        messageLabel.setStyle("");
        resizeDialog();
    }

    private void setRowVisible(HBox row, boolean visible) {
        row.setVisible(visible);
        row.setManaged(visible);
    }

    private void resizeDialog() {
        if (dialogStage != null) {
            dialogStage.sizeToScene();
        }
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
