package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * 删除对话框工具类
 * 参考系统删除对话框样式
 */
public class DeleteDialogUtil {
    
    /**
     * 显示删除确认对话框
     * @param title 对话框标题
     * @param message 删除消息
     * @param itemName 要删除的项目名称
     * @param owner 父窗口
     * @param checkBoxOptions 自定义复选框选项列表
     * @return 删除确认结果
     */
    public static DeleteResult showDeleteDialog(String title, String message, String itemName, Stage owner, List<CheckBoxOption> checkBoxOptions) {
        Dialog<DeleteResult> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(owner);
        dialog.setResizable(false);
        
        // 设置对话框样式
        dialog.getDialogPane().setMinWidth(350);
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().setMinHeight(180);
        dialog.getDialogPane().setPrefHeight(220);
        
        // 创建主内容区域
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(18, 20, 18, 20));
        contentBox.setStyle("-fx-background-color: white;");
        
        // 主问题文本
        Label questionLabel = new Label(message);
        questionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        questionLabel.setWrapText(true);
        
        // 复选框区域
        VBox checkboxBox = new VBox(6);
        checkboxBox.setPadding(new Insets(8, 0, 8, 0));
        
        // 动态创建复选框
        List<CheckBox> checkBoxes = new java.util.ArrayList<>();
        if (checkBoxOptions != null && !checkBoxOptions.isEmpty()) {
            for (CheckBoxOption option : checkBoxOptions) {
                CheckBox checkBox = new CheckBox(option.getText());
                checkBox.setSelected(option.isDefaultSelected());
                checkBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #333333;");
                checkBoxes.add(checkBox);
                checkboxBox.getChildren().add(checkBox);
            }
        }
        

        
        // 组装内容
        contentBox.getChildren().addAll(questionLabel, checkboxBox);
        
        // 设置对话框内容
        dialog.getDialogPane().setContent(contentBox);
        
        // 设置按钮
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, confirmButtonType);
        
        // 设置按钮样式
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #007AFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 14;");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-background-color: white; -fx-text-fill: #333333; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-background-radius: 5; -fx-padding: 6 14;");
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // 收集复选框状态
                List<Boolean> checkBoxStates = new java.util.ArrayList<>();
                for (int i = 0; i < checkBoxes.size(); i++) {
                    checkBoxStates.add(checkBoxes.get(i).isSelected());
                }
                return new DeleteResult(true, checkBoxStates, checkBoxOptions);
            }
            return new DeleteResult(false, null, null);
        });
        
        // 显示对话框
        Optional<DeleteResult> result = dialog.showAndWait();
        return result.orElse(new DeleteResult(false, null, null));
    }
    
    /**
     * 复选框选项类
     */
    public static class CheckBoxOption {
        private final String text;
        private final boolean defaultSelected;
        private final String action;
        
        public CheckBoxOption(String text, boolean defaultSelected, String action) {
            this.text = text;
            this.defaultSelected = defaultSelected;
            this.action = action;
        }
        
        public String getText() {
            return text;
        }
        
        public boolean isDefaultSelected() {
            return defaultSelected;
        }
        
        public String getAction() {
            return action;
        }
    }
    
    /**
     * 删除结果类
     */
    public static class DeleteResult {
        private final boolean confirmed;
        private final List<Boolean> checkBoxStates;
        private final List<CheckBoxOption> checkBoxOptions;
        
        public DeleteResult(boolean confirmed, List<Boolean> checkBoxStates, List<CheckBoxOption> checkBoxOptions) {
            this.confirmed = confirmed;
            this.checkBoxStates = checkBoxStates;
            this.checkBoxOptions = checkBoxOptions;
        }
        
        public boolean isConfirmed() {
            return confirmed;
        }
        
        public List<Boolean> getCheckBoxStates() {
            return checkBoxStates;
        }
        
        public List<CheckBoxOption> getCheckBoxOptions() {
            return checkBoxOptions;
        }
        
        /**
         * 获取指定索引的复选框状态
         */
        public boolean getCheckBoxState(int index) {
            if (checkBoxStates != null && index >= 0 && index < checkBoxStates.size()) {
                return checkBoxStates.get(index);
            }
            return false;
        }
        
        /**
         * 获取指定操作的复选框状态
         */
        public boolean getCheckBoxStateByAction(String action) {
            if (checkBoxOptions != null && checkBoxStates != null) {
                for (int i = 0; i < checkBoxOptions.size(); i++) {
                    if (action.equals(checkBoxOptions.get(i).getAction())) {
                        return getCheckBoxState(i);
                    }
                }
            }
            return false;
        }
        
        // 向后兼容的方法
        public boolean isSecureDelete() {
            return getCheckBoxStateByAction("synch_code");
        }
    }
}
