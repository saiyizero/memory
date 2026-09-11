package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * 导入DDL弹窗
 */
public class ImpddlDialogUtil {

    /**
     * 显示导入DDL弹窗
     * @param onConfirm 确认回调，参数为输入的DDL内容
     */
    public static void showImportDdlDialog(Consumer<String> onConfirm) {
        Platform.runLater(() -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("导入DDL语句");
            dialog.setHeaderText("请输入建表语句");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResizable(true);
            dialog.getDialogPane().setPrefSize(800, 600);
            dialog.getDialogPane().setMinSize(600, 400);
            dialog.getDialogPane().setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            // 创建文本区域
            TextArea textArea = new TextArea();
            textArea.setPromptText("在此处粘贴建表语句");
            textArea.setPrefWidth(780);
            textArea.setPrefHeight(500);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(textArea, Priority.ALWAYS);

            // 设置背景色和字体样式，参考SQL语法高亮组件
            textArea.setStyle(
                    "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-color: #1e1e1e;" +
                            "-fx-text-fill: #d4d4d4;" +
                            "-fx-control-inner-background: #1e1e1e;" +
                            "-fx-highlight-fill: #264f78;" +
                            "-fx-highlight-text-fill: #ffffff;" +
                            "-fx-caret-color: #ffffff;"
            );

            // 创建主容器
            VBox vbox = new VBox();
            vbox.setSpacing(8);
            vbox.getChildren().add(textArea);
            vbox.setPrefSize(780, 500);
            vbox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            dialog.getDialogPane().setContent(vbox);

            // 设置结果转换器
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return textArea.getText();
                }
                return null;
            });

            // 弹窗显示后调整大小
            dialog.setOnShown(evv -> {
                Window window = dialog.getDialogPane().getScene().getWindow();
                if (window != null) {
                    window.setWidth(800);
                    window.setHeight(600);
                }
            });

            // 显示对话框并处理结果
            dialog.showAndWait().ifPresent(result -> {
                if (result != null && !result.trim().isEmpty()) {
                    onConfirm.accept(result);
                } else if (result != null && result.trim().isEmpty()) {
                    ViewUtils.alertForFail("请输入DDL语句");
                }
            });
        });
    }
}
