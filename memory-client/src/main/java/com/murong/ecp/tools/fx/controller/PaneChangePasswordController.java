package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.auth.LoginService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaneChangePasswordController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private GlobalProperties globalProperties;

    @FXML
    private Label currentUserLabel;
    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    public void initialize() {
        refreshCurrentUser();
    }

    @FXML
    public void savePassword() {
        try {
            refreshCurrentUser();
            if (globalProperties.getOperator() == null
                    || StringUtils.isBlank(globalProperties.getOperator().getUsername())) {
                ViewUtils.alertForFail("未登录，无法修改密码");
                return;
            }

            String oldPassword = oldPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            if (StringUtils.isBlank(oldPassword)) {
                ViewUtils.alertForFail("请输入原密码");
                oldPasswordField.requestFocus();
                return;
            }
            if (StringUtils.isBlank(newPassword)) {
                ViewUtils.alertForFail("请输入新密码");
                newPasswordField.requestFocus();
                return;
            }
            if (StringUtils.isBlank(confirmPassword)) {
                ViewUtils.alertForFail("请再次输入新密码");
                confirmPasswordField.requestFocus();
                return;
            }
            if (!StringUtils.equals(newPassword, confirmPassword)) {
                ViewUtils.alertForFail("两次输入的新密码不一致");
                confirmPasswordField.requestFocus();
                return;
            }
            if (StringUtils.equals(oldPassword, newPassword)) {
                ViewUtils.alertForFail("新密码不能与原密码相同");
                newPasswordField.requestFocus();
                return;
            }

            CrResult<String> result = loginService.changeOwnPassword(oldPassword, newPassword);
            if (result != null && result.isSucess()) {
                resetForm();
                ViewUtils.alertForSucess(StringUtils.defaultIfBlank(result.getMsgInf(), "密码修改成功"));
            } else {
                String message = result == null ? "密码修改失败" : result.getMsgInf();
                ViewUtils.alertForFail(StringUtils.defaultIfBlank(message, "密码修改失败"));
            }
        } catch (Exception e) {
            ViewUtils.alertForFail("密码修改失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void resetForm() {
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void refreshCurrentUser() {
        if (globalProperties != null && globalProperties.getOperator() != null
                && StringUtils.isNotBlank(globalProperties.getOperator().getUsername())) {
            String username = globalProperties.getOperator().getUsername();
            String realName = globalProperties.getOperator().getRealname();
            if (StringUtils.isNotBlank(realName)) {
                currentUserLabel.setText(username + "（" + realName + "）");
            } else {
                currentUserLabel.setText(username);
            }
        } else {
            currentUserLabel.setText("未登录");
        }
    }
}
