package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 通用选择对话框工具类
 * 支持下拉列表选择和回调函数
 */
public class SelectDialogUtil {
    
    /**
     * 显示选择对话框
     * 
     * @param title 对话框标题
     * @param options 下拉选项列表
     * @param defaultValue 默认选中的值
     * @param onConfirm 确认按钮回调函数，参数为选中的值
     * @param onCancel 取消按钮回调函数
     */
    public static void showSelectDialog(String title, 
                                      List<String> options, 
                                      String defaultValue,
                                      Consumer<String> onConfirm,
                                      Runnable onCancel) {
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        double boxWidth = 300;

        VBox content = new VBox(18);
        content.setStyle("-fx-padding: 24 32 18 32; -fx-background-color: #fff; -fx-border-radius: 10; -fx-background-radius: 10;");
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(boxWidth);

        Label label = new Label("请选择");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue != null ? defaultValue : (options.isEmpty() ? "" : options.get(0)));
        comboBox.setPrefWidth(boxWidth - 10);
        comboBox.setMinWidth(boxWidth - 10);
        comboBox.setMaxWidth(boxWidth - 10);
        comboBox.setStyle("-fx-font-size: 14px;");

        // 自定义按钮区
        HBox btnBox = new HBox(12);
        btnBox.setPrefWidth(boxWidth - 10);
        btnBox.setMinWidth(boxWidth - 10);
        btnBox.setMaxWidth(boxWidth - 10);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setStyle("-fx-padding: 0 0 0 0;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setMinWidth(80);
        cancelBtn.setPrefWidth(80);
        cancelBtn.setMaxWidth(80);
        cancelBtn.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #d9d9d9; -fx-border-width: 1px; -fx-cursor: hand;");
        
        // 取消按钮的鼠标悬停效果
        cancelBtn.setOnMouseEntered(e -> {
            cancelBtn.setStyle("-fx-background-color: #e6e6e6; -fx-text-fill: #333; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #bfbfbf; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMouseExited(e -> {
            cancelBtn.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #d9d9d9; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMousePressed(e -> {
            cancelBtn.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: #000; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #adadad; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMouseReleased(e -> {
            cancelBtn.setStyle("-fx-background-color: #e6e6e6; -fx-text-fill: #333; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #bfbfbf; -fx-border-width: 1px; -fx-cursor: hand;");
        });

        Button okBtn = new Button("确定");
        okBtn.setMinWidth(80);
        okBtn.setPrefWidth(80);
        okBtn.setMaxWidth(80);
        okBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        
        // 确定按钮的鼠标悬停效果
        okBtn.setOnMouseEntered(e -> {
            okBtn.setStyle("-fx-background-color: #ff7875; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMouseExited(e -> {
            okBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMousePressed(e -> {
            okBtn.setStyle("-fx-background-color: #d9363e; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMouseReleased(e -> {
            okBtn.setStyle("-fx-background-color: #ff7875; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnBox.getChildren().addAll(cancelBtn, spacer, okBtn);

        content.getChildren().addAll(label, comboBox, btnBox);

        dialog.getDialogPane().setContent(content);
        
        // 添加标准按钮类型以支持ESC键和X按钮
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        // 隐藏标准按钮，但保留其功能
        dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        dialog.getDialogPane().setMinWidth(boxWidth + 64);
        dialog.getDialogPane().setPrefWidth(boxWidth + 64);
        dialog.getDialogPane().setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.12),16,0,0,4); -fx-padding: 0;");

        // 设置默认结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return comboBox.getValue();
            }
            return null;
        });

        // 事件处理
        okBtn.setOnAction(e -> {
            String selectedValue = comboBox.getValue();
            dialog.setResult(selectedValue);
            dialog.close();
        });
        
        cancelBtn.setOnAction(e -> {
            dialog.setResult(null);
            dialog.close();
        });

        // 添加键盘事件支持
        dialog.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(null);
                dialog.close();
            } else if (event.getCode() == KeyCode.ENTER) {
                String selectedValue = comboBox.getValue();
                dialog.setResult(selectedValue);
                dialog.close();
            }
        });

        // 显示对话框并处理结果
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            // 确认操作
            if (onConfirm != null) {
                onConfirm.accept(result.get());
            }
        } else {
            // 取消操作
            if (onCancel != null) {
                onCancel.run();
            }
        }
    }
    
    /**
     * 显示带复选框的选择对话框（用于表结构对比）
     * 
     * @param title 对话框标题
     * @param envOptions 下拉选项列表
     * @param defaultValue 默认选中的值
     * @param onConfirm 确认按钮回调函数，参数为选中的值和复选框状态
     * @param onCancel 取消按钮回调函数
     */
    public static void showSelectDialogWithCheckboxes(String title, 
                                                    List<String> envOptions, 
                                                    String defaultValue,
                                                    Consumer<TableCompareOptions> onConfirm,
                                                    Runnable onCancel) {
        
        Dialog<TableCompareOptions> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        double boxWidth = 400;

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 24 32 18 32; -fx-background-color: #fff; -fx-border-radius: 10; -fx-background-radius: 10;");
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(boxWidth);

        Label label = new Label("请选择对比环境");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(envOptions);
        comboBox.setValue(defaultValue != null ? defaultValue : (envOptions.isEmpty() ? "" : envOptions.get(0)));
        comboBox.setPrefWidth(boxWidth - 10);
        comboBox.setMinWidth(boxWidth - 10);
        comboBox.setMaxWidth(boxWidth - 10);
        comboBox.setStyle("-fx-font-size: 14px;");

        // 复选框区域
        VBox checkboxBox = new VBox(12);
        checkboxBox.setPrefWidth(boxWidth - 10);
        checkboxBox.setMinWidth(boxWidth - 10);
        checkboxBox.setMaxWidth(boxWidth - 10);
        checkboxBox.setStyle("-fx-padding: 16 0 0 0;");

        CheckBox compareNullableCheckBox = new CheckBox("对比字段是否允许为空");
        compareNullableCheckBox.setSelected(false);
        compareNullableCheckBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        CheckBox compareDefaultValueCheckBox = new CheckBox("对比字段默认值");
        compareDefaultValueCheckBox.setSelected(false);
        compareDefaultValueCheckBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        CheckBox compareNonPrimaryIndexCheckBox = new CheckBox("对比非主键索引");
        compareNonPrimaryIndexCheckBox.setSelected(false);
        compareNonPrimaryIndexCheckBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        CheckBox compareCommentCheckBox = new CheckBox("对比注释说明");
        compareCommentCheckBox.setSelected(false);
        compareCommentCheckBox.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        checkboxBox.getChildren().addAll(compareNullableCheckBox, compareDefaultValueCheckBox, compareNonPrimaryIndexCheckBox, compareCommentCheckBox);

        // 自定义按钮区
        HBox btnBox = new HBox(12);
        btnBox.setPrefWidth(boxWidth - 10);
        btnBox.setMinWidth(boxWidth - 10);
        btnBox.setMaxWidth(boxWidth - 10);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setStyle("-fx-padding: 0 0 0 0;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setMinWidth(80);
        cancelBtn.setPrefWidth(80);
        cancelBtn.setMaxWidth(80);
        cancelBtn.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #d9d9d9; -fx-border-width: 1px; -fx-cursor: hand;");
        
        // 取消按钮的鼠标悬停效果
        cancelBtn.setOnMouseEntered(e -> {
            cancelBtn.setStyle("-fx-background-color: #e6e6e6; -fx-text-fill: #333; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #bfbfbf; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMouseExited(e -> {
            cancelBtn.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #d9d9d9; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMousePressed(e -> {
            cancelBtn.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: #000; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #adadad; -fx-border-width: 1px; -fx-cursor: hand;");
        });
        cancelBtn.setOnMouseReleased(e -> {
            cancelBtn.setStyle("-fx-background-color: #e6e6e6; -fx-text-fill: #333; -fx-background-radius: 6; -fx-font-size: 14px; -fx-border-color: #bfbfbf; -fx-border-width: 1px; -fx-cursor: hand;");
        });

        Button okBtn = new Button("确定");
        okBtn.setMinWidth(80);
        okBtn.setPrefWidth(80);
        okBtn.setMaxWidth(80);
        okBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        
        // 确定按钮的鼠标悬停效果
        okBtn.setOnMouseEntered(e -> {
            okBtn.setStyle("-fx-background-color: #ff7875; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMouseExited(e -> {
            okBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMousePressed(e -> {
            okBtn.setStyle("-fx-background-color: #d9363e; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });
        okBtn.setOnMouseReleased(e -> {
            okBtn.setStyle("-fx-background-color: #ff7875; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px; -fx-cursor: hand;");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnBox.getChildren().addAll(cancelBtn, spacer, okBtn);

        content.getChildren().addAll(label, comboBox, checkboxBox, btnBox);

        dialog.getDialogPane().setContent(content);
        
        // 添加标准按钮类型以支持ESC键和X按钮
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        // 隐藏标准按钮，但保留其功能
        dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        dialog.getDialogPane().setMinWidth(boxWidth + 64);
        dialog.getDialogPane().setPrefWidth(boxWidth + 64);
        dialog.getDialogPane().setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.12),16,0,0,4); -fx-padding: 0;");

        // 设置默认结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                TableCompareOptions compareOptions = new TableCompareOptions();
                compareOptions.setCompareEnv(comboBox.getValue());
                compareOptions.setCompareNullable(compareNullableCheckBox.isSelected());
                compareOptions.setCompareDefaultValue(compareDefaultValueCheckBox.isSelected());
                compareOptions.setCompareNonPrimaryIndex(compareNonPrimaryIndexCheckBox.isSelected());
                compareOptions.setCompareComment(compareCommentCheckBox.isSelected());
                return compareOptions;
            }
            return null;
        });

        // 事件处理
        okBtn.setOnAction(e -> {
            TableCompareOptions compareOptions = new TableCompareOptions();
            compareOptions.setCompareEnv(comboBox.getValue());
            compareOptions.setCompareNullable(compareNullableCheckBox.isSelected());
            compareOptions.setCompareDefaultValue(compareDefaultValueCheckBox.isSelected());
            compareOptions.setCompareNonPrimaryIndex(compareNonPrimaryIndexCheckBox.isSelected());
            compareOptions.setCompareComment(compareCommentCheckBox.isSelected());
            dialog.setResult(compareOptions);
            dialog.close();
        });
        
        cancelBtn.setOnAction(e -> {
            dialog.setResult(null);
            dialog.close();
        });

        // 添加键盘事件支持
        dialog.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(null);
                dialog.close();
            } else if (event.getCode() == KeyCode.ENTER) {
                TableCompareOptions compareOptions = new TableCompareOptions();
                compareOptions.setCompareEnv(comboBox.getValue());
                compareOptions.setCompareNullable(compareNullableCheckBox.isSelected());
                compareOptions.setCompareDefaultValue(compareDefaultValueCheckBox.isSelected());
                compareOptions.setCompareNonPrimaryIndex(compareNonPrimaryIndexCheckBox.isSelected());
                compareOptions.setCompareComment(compareCommentCheckBox.isSelected());
                dialog.setResult(compareOptions);
                dialog.close();
            }
        });

        // 显示对话框并处理结果
        Optional<TableCompareOptions> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null) {
            // 确认操作
            if (onConfirm != null) {
                onConfirm.accept(result.get());
            }
        } else {
            // 取消操作
            if (onCancel != null) {
                onCancel.run();
            }
        }
    }
    
    /**
     * 显示选择对话框（简化版本，只需要确认回调）
     * 
     * @param title 对话框标题
     * @param options 下拉选项列表
     * @param defaultValue 默认选中的值
     * @param onConfirm 确认按钮回调函数，参数为选中的值
     */
    public static void showSelectDialog(String title, 
                                      List<String> options, 
                                      String defaultValue,
                                      Consumer<String> onConfirm) {
        showSelectDialog(title, options, defaultValue, onConfirm, null);
    }
    
    /**
     * 显示选择对话框（最简化版本）
     * 
     * @param title 对话框标题
     * @param options 下拉选项列表
     * @param onConfirm 确认按钮回调函数，参数为选中的值
     */
    public static void showSelectDialog(String title, 
                                      List<String> options,
                                      Consumer<String> onConfirm) {
        showSelectDialog(title, options, null, onConfirm, null);
    }
    
    /**
     * 表结构对比选项类
     */
    public static class TableCompareOptions {
        private String compareEnv;
        private boolean compareNullable;
        private boolean compareDefaultValue;
        private boolean compareNonPrimaryIndex;
        private boolean compareComment;
        
        public String getCompareEnv() {
            return compareEnv;
        }
        
        public void setCompareEnv(String compareEnv) {
            this.compareEnv = compareEnv;
        }
        
        public boolean isCompareNullable() {
            return compareNullable;
        }
        
        public void setCompareNullable(boolean compareNullable) {
            this.compareNullable = compareNullable;
        }
        
        public boolean isCompareDefaultValue() {
            return compareDefaultValue;
        }
        
        public void setCompareDefaultValue(boolean compareDefaultValue) {
            this.compareDefaultValue = compareDefaultValue;
        }
        
        public boolean isCompareNonPrimaryIndex() {
            return compareNonPrimaryIndex;
        }
        
        public void setCompareNonPrimaryIndex(boolean compareNonPrimaryIndex) {
            this.compareNonPrimaryIndex = compareNonPrimaryIndex;
        }
        
        public boolean isCompareComment() {
            return compareComment;
        }
        
        public void setCompareComment(boolean compareComment) {
            this.compareComment = compareComment;
        }
    }
}
