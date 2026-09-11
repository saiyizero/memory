package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * 提醒对话框工具类
 * 提供各种确认、警告、信息等弹出框功能
 * 参考DeleteDialogUtil样式设计
 */
public class ReminderDialogUtil {
    
    /**
     * 显示确认删除对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param onConfirm 确认删除时的回调函数
     */
    public static void showConfirmDeleteDialog(String title, String headerText, String contentText, Runnable onConfirm) {
        showConfirmDeleteDialog(title, headerText, contentText, onConfirm, null);
    }
    
    /**
     * 显示确认删除对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param onConfirm 确认删除时的回调函数
     * @param onCancel 取消删除时的回调函数
     */
    public static void showConfirmDeleteDialog(String title, String headerText, String contentText, Runnable onConfirm, Runnable onCancel) {
        showConfirmDialog(title, headerText, contentText, onConfirm, onCancel, null);
    }
    
    /**
     * 显示确认删除对话框（默认文本）
     * @param onConfirm 确认删除时的回调函数
     */
    public static void showConfirmDeleteDialog(Runnable onConfirm) {
        showConfirmDeleteDialog("确认删除", "确认删除这条记录吗？", "删除后将无法恢复", onConfirm);
    }
    
    /**
     * 显示确认对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param onConfirm 确认时的回调函数
     * @param onCancel 取消时的回调函数
     */
    public static void showConfirmDialog(String title, String headerText, String contentText, Runnable onConfirm, Runnable onCancel) {
        showConfirmDialog(title, headerText, contentText, onConfirm, onCancel, null);
    }
    
    /**
     * 显示带复选框的确认对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param onConfirm 确认时的回调函数
     * @param onCancel 取消时的回调函数
     * @param checkBoxOptions 复选框选项列表
     */
    public static void showConfirmDialog(String title, String headerText, String contentText, 
                                       Runnable onConfirm, Runnable onCancel, List<CheckBoxOption> checkBoxOptions) {
        showConfirmDialogWithResult(title, headerText, contentText, checkBoxOptions, 
            dialogResult -> {
                if (dialogResult.isConfirmed()) {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                } else {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                }
            });
    }
    
    /**
     * 显示带复选框的确认对话框（返回结果）
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param checkBoxOptions 复选框选项列表
     * @param onResult 结果处理回调函数
     */
    public static void showConfirmDialogWithResult(String title, String headerText, String contentText, 
                                                 List<CheckBoxOption> checkBoxOptions, 
                                                 java.util.function.Consumer<DialogResult> onResult) {
        Platform.runLater(() -> {
            Dialog<DialogResult> dialog = new Dialog<>();
            dialog.setTitle(title != null ? title : "确认");
            dialog.setResizable(false);
            
            // 设置对话框样式
            dialog.getDialogPane().setMinWidth(350);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setMinHeight(180);
            dialog.getDialogPane().setPrefHeight(220);
            
            // 设置模态和置顶
            dialog.initModality(Modality.APPLICATION_MODAL);
            setDialogOwner(dialog);
            
            // 创建主内容区域
            VBox contentBox = new VBox(12);
            contentBox.setPadding(new Insets(18, 20, 18, 20));
            contentBox.setStyle("-fx-background-color: white;");
            
            // 主问题文本
            Label questionLabel = new Label(contentText != null ? contentText : headerText);
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
                    return new DialogResult(true, checkBoxStates, checkBoxOptions);
                }
                return new DialogResult(false, null, null);
            });
            
            // 显示对话框并处理结果
            Optional<DialogResult> result = dialog.showAndWait();
            result.ifPresent(onResult);
        });
    }
    
    /**
     * 显示信息对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     */
    public static void showInfoDialog(String title, String headerText, String contentText) {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(title != null ? title : "提示");
            dialog.setResizable(false);
            
            // 设置对话框样式
            dialog.getDialogPane().setMinWidth(350);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setMinHeight(150);
            dialog.getDialogPane().setPrefHeight(180);
            
            // 设置模态和置顶
            dialog.initModality(Modality.APPLICATION_MODAL);
            setDialogOwner(dialog);
            
            // 创建主内容区域
            VBox contentBox = new VBox(12);
            contentBox.setPadding(new Insets(18, 20, 18, 20));
            contentBox.setStyle("-fx-background-color: white;");
            
            // 主问题文本
            Label questionLabel = new Label(contentText != null ? contentText : headerText);
            questionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333333;");
            questionLabel.setWrapText(true);
            
            // 组装内容
            contentBox.getChildren().addAll(questionLabel);
            
            // 设置对话框内容
            dialog.getDialogPane().setContent(contentBox);
            
            // 设置按钮
            ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(okButtonType);
            
            // 设置按钮样式
            Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
            okButton.setStyle("-fx-background-color: #007AFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 14;");
            
            dialog.showAndWait();
        });
    }
    
    /**
     * 显示警告对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     */
    public static void showWarningDialog(String title, String headerText, String contentText) {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(title != null ? title : "警告");
            dialog.setResizable(false);
            
            // 设置对话框样式
            dialog.getDialogPane().setMinWidth(350);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setMinHeight(150);
            dialog.getDialogPane().setPrefHeight(180);
            
            // 设置模态和置顶
            dialog.initModality(Modality.APPLICATION_MODAL);
            setDialogOwner(dialog);
            
            // 创建主内容区域
            VBox contentBox = new VBox(12);
            contentBox.setPadding(new Insets(18, 20, 18, 20));
            contentBox.setStyle("-fx-background-color: white;");
            
            // 主问题文本
            Label questionLabel = new Label(contentText != null ? contentText : headerText);
            questionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FF6B35;");
            questionLabel.setWrapText(true);
            
            // 组装内容
            contentBox.getChildren().addAll(questionLabel);
            
            // 设置对话框内容
            dialog.getDialogPane().setContent(contentBox);
            
            // 设置按钮
            ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(okButtonType);
            
            // 设置按钮样式
            Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
            okButton.setStyle("-fx-background-color: #FF6B35; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 14;");
            
            dialog.showAndWait();
        });
    }
    
    /**
     * 显示错误对话框
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     */
    public static void showErrorDialog(String title, String headerText, String contentText) {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(title != null ? title : "错误");
            dialog.setResizable(false);
            
            // 设置对话框样式
            dialog.getDialogPane().setMinWidth(350);
            dialog.getDialogPane().setPrefWidth(400);
            dialog.getDialogPane().setMinHeight(150);
            dialog.getDialogPane().setPrefHeight(180);
            
            // 设置模态和置顶
            dialog.initModality(Modality.APPLICATION_MODAL);
            setDialogOwner(dialog);
            
            // 创建主内容区域
            VBox contentBox = new VBox(12);
            contentBox.setPadding(new Insets(18, 20, 18, 20));
            contentBox.setStyle("-fx-background-color: white;");
            
            // 主问题文本
            Label questionLabel = new Label(contentText != null ? contentText : headerText);
            questionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FF3B30;");
            questionLabel.setWrapText(true);
            
            // 组装内容
            contentBox.getChildren().addAll(questionLabel);
            
            // 设置对话框内容
            dialog.getDialogPane().setContent(contentBox);
            
            // 设置按钮
            ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(okButtonType);
            
            // 设置按钮样式
            Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
            okButton.setStyle("-fx-background-color: #FF3B30; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 6 14;");
            
            dialog.showAndWait();
        });
    }
    
    /**
     * 设置Dialog的Owner窗口和置顶属性
     * @param dialog Dialog对象
     */
    private static void setDialogOwner(Dialog<?> dialog) {
        // 查找主窗口Stage作为owner
        Window owner = null;
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                owner = window;
                break;
            }
        }
        if (owner != null) {
            dialog.initOwner(owner);
        }
        // 弹窗始终置顶
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.setAlwaysOnTop(true);
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
     * 对话框结果类
     */
    public static class DialogResult {
        private final boolean confirmed;
        private final List<Boolean> checkBoxStates;
        private final List<CheckBoxOption> checkBoxOptions;
        
        public DialogResult(boolean confirmed, List<Boolean> checkBoxStates, List<CheckBoxOption> checkBoxOptions) {
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
    }
}
