package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.utils.JsonFormatUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.XmlFormatUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaneFormatController {
    @Autowired
    private GlobalProperties globalProps;
    @FXML
    private TextArea xmlInput;
    @FXML
    private Button formatBtn;
    @FXML
    private Button removeNewlineBtn;
    @FXML
    private Button removeSpaceBtn;
    @FXML
    private TextField replaceInput;
    @FXML
    private Button replaceBtn;

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        formatBtn.setOnAction(this::onFormat);
        removeNewlineBtn.setOnAction(this::onRemoveNewline);
        removeSpaceBtn.setOnAction(this::onRemoveSpace);
        replaceBtn.setOnAction(this::onReplace);
    }

    private void onFormat(ActionEvent event) {
        String input = xmlInput.getText();
        if (input == null || input.trim().isEmpty()) {
            showAlert("请输入内容！");
            return;
        }
        try {
            String formatted;
            if (isJson(input)) {
                formatted = JsonFormatUtil.formatJson(input);
            } else {
                formatted = XmlFormatUtil.formatXml(input);
            }
            xmlInput.setText(formatted);
        } catch (Exception e) {
            showAlert("格式化失败：" + e.getMessage());
        }
    }

    private boolean isJson(String text) {
        String t = text.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    private void onRemoveNewline(ActionEvent event) {
        String input = xmlInput.getText();
        if (input == null) return;
        xmlInput.setText(input.replaceAll("\r?\n", ""));
    }

    private void onRemoveSpace(ActionEvent event) {
        String input = xmlInput.getText();
        if (input == null) return;
        xmlInput.setText(input.replaceAll("\\s+", ""));
    }

    private void onReplace(ActionEvent event) {
        String input = xmlInput.getText();
        String replaceRule = replaceInput.getText();
        if (input == null || replaceRule == null || !replaceRule.contains("->")) {
            showAlert("请输入正确的替换内容，格式：原文->新文");
            return;
        }
        String[] parts = replaceRule.split("->", 2);
        String oldStr = parts[0];
        String newStr = parts[1];
        xmlInput.setText(input.replace(oldStr, newStr));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
} 