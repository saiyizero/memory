package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.Modality;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 日志显示弹窗
 */
public class LogDialogUtil {
    
    /**
     * 显示日志内容弹窗
     * @param content 日志内容
     * @param title 弹窗标题
     * @param width 初始宽度
     * @param height 初始高度
     */
    public static void showLogDialog(String content, String title, double width, double height) {
        Platform.runLater(() -> {
            Dialog<Void> logDialog = new Dialog<>();
            logDialog.setTitle(title != null ? title : "日志内容");
            logDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            logDialog.setResizable(true);
            logDialog.getDialogPane().setPrefSize(width, height);
            logDialog.getDialogPane().setMinSize(600, 400);
            logDialog.getDialogPane().setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            // 使用非模态，弹出后不阻塞主界面
            logDialog.initModality(Modality.NONE);

            // 搜索功能UI
            TextField searchField = new TextField();
            searchField.setPromptText("搜索关键字");
            Button upBtn = new Button("↑");
            Button downBtn = new Button("↓");
            Button copyBtn = new Button("复制全部");
            Button saveBtn = new Button("保存");
            Button wrapBtn = new Button("自动换行");
            Button fontSizeUpBtn = new Button("+");
            Button fontSizeDownBtn = new Button("-");
            HBox searchBox = new HBox(6, searchField, upBtn, downBtn, copyBtn, saveBtn, wrapBtn, fontSizeUpBtn, fontSizeDownBtn);
            searchBox.setAlignment(Pos.CENTER_RIGHT);

            TextArea area = new TextArea(content);
            area.setEditable(true);
            area.setWrapText(false); // 初始不自动换行
            area.setPrefWidth(width - 20);
            area.setPrefHeight(height - 40);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(area, Priority.ALWAYS);
            
            // 字体大小调节相关变量
            final double[] currentFontSize = {14.0};
            final double minFontSize = 8.0;
            final double maxFontSize = 32.0;
            final double fontSizeStep = 2.0;
            
            // 更新字体大小的方法
            Runnable updateFontSize = () -> {
                String newStyle = 
                    "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
                    "-fx-font-size: " + currentFontSize[0] + "px;" +
                    "-fx-background-color: #1e1e1e;" +
                    "-fx-text-fill: #d4d4d4;" +
                    "-fx-control-inner-background: #1e1e1e;" +
                    "-fx-highlight-fill: #264f78;" +
                    "-fx-highlight-text-fill: #ffffff;" +
                    "-fx-caret-color: #ffffff;";
                area.setStyle(newStyle);
            };
            
            // 设置背景色和字体样式，参考SQL语法高亮组件
            String initialStyle = 
                "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
                "-fx-font-size: " + currentFontSize[0] + "px;" +
                "-fx-background-color: #1e1e1e;" +
                "-fx-text-fill: #d4d4d4;" +
                "-fx-control-inner-background: #1e1e1e;" +
                "-fx-highlight-fill: #264f78;" +
                "-fx-highlight-text-fill: #ffffff;" +
                "-fx-caret-color: #ffffff;";
            area.setStyle(initialStyle);

            // 右键菜单格式化
            ContextMenu contextMenu = new ContextMenu();
            MenuItem formatItem = new MenuItem("格式化");
            contextMenu.getItems().add(formatItem);
            area.setContextMenu(contextMenu);

            formatItem.setOnAction(evt -> {
                String selected = area.getSelectedText();
                if (selected == null || selected.isEmpty()) {
                    ViewUtils.alertForFail("请先选中需要格式化的内容");
                    return;
                }
                String formatted = null;
                boolean isJson = false, isXml = false;
                
                // 尝试格式化JSON
                try {
                    formatted = com.murong.ecp.tools.fx.infrastructure.utils.JsonFormatUtil.formatJson(selected);
                    isJson = true;
                } catch (Exception ignore) {}
                
                // 尝试格式化XML
                if (!isJson) {
                    try {
                        formatted = com.murong.ecp.tools.fx.infrastructure.utils.XmlFormatUtil.formatXml(selected);
                        isXml = true;
                    } catch (Exception ignore) {}
                }
                
                if (!isJson && !isXml) {
                    ViewUtils.alertForFail("选中内容不是合法的JSON或XML");
                    return;
                }
                
                // 替换选中内容为格式化后的内容
                int start = area.getSelection().getStart();
                int end = area.getSelection().getEnd();
                String before = area.getText(0, start);
                String after = area.getText(end, area.getLength());
                String newText = before + formatted + after;
                area.setText(newText);
                area.selectRange(start, start + formatted.length());
            });

            // 复制全部
            copyBtn.setOnAction(evt -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content1 = new ClipboardContent();
                content1.putString(area.getText());
                clipboard.setContent(content1);
                ViewUtils.alertForSucess("已复制全部内容");
            });

            // 自动换行切换
            wrapBtn.setOnAction(evt -> {
                boolean currentWrap = area.isWrapText();
                area.setWrapText(!currentWrap);
                if (!currentWrap) {
                    wrapBtn.setText("不自动换行");
                } else {
                    wrapBtn.setText("自动换行");
                }
            });

            // 字体大小调节
            fontSizeUpBtn.setOnAction(evt -> {
                if (currentFontSize[0] < maxFontSize) {
                    currentFontSize[0] += fontSizeStep;
                    updateFontSize.run();
                }
            });
            
            fontSizeDownBtn.setOnAction(evt -> {
                if (currentFontSize[0] > minFontSize) {
                    currentFontSize[0] -= fontSizeStep;
                    updateFontSize.run();
                }
            });



            // 保存编辑内容
            saveBtn.setOnAction(evt -> {
                String editedContent = area.getText();
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("保存文件");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("文本文件", "*.txt"),
                    new FileChooser.ExtensionFilter("所有文件", "*.*")
                );
                
                File file = fileChooser.showSaveDialog(logDialog.getDialogPane().getScene().getWindow());
                if (file != null) {
                    try {
                        Files.write(file.toPath(), editedContent.getBytes());
                        ViewUtils.alertForSucess("文件已保存到: " + file.getAbsolutePath());
                    } catch (IOException e) {
                        ViewUtils.alertForFail("保存文件失败: " + e.getMessage());
                    }
                }
            });

            VBox vbox = new VBox();
            vbox.setSpacing(8);
            vbox.getChildren().addAll(searchBox, area);
            vbox.setPrefSize(width - 20, height - 40);
            vbox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            logDialog.getDialogPane().setContent(vbox);

            // 搜索功能实现
            final int[] lastIndex = { -1 };
            upBtn.setOnAction(evt -> {
                String keyword = searchField.getText();
                if (keyword == null || keyword.isEmpty()) return;
                String text = area.getText();
                int caret = area.getCaretPosition();
                int idx = text.lastIndexOf(keyword, caret - keyword.length() - 1);
                if (idx == -1 && caret != text.length()) {
                    idx = text.lastIndexOf(keyword, text.length());
                }
                if (idx != -1) {
                    area.selectRange(idx, idx + keyword.length());
                    area.requestFocus();
                    lastIndex[0] = idx;
                }
            });
            downBtn.setOnAction(evt -> {
                String keyword = searchField.getText();
                if (keyword == null || keyword.isEmpty()) return;
                String text = area.getText();
                int caret = area.getSelection().getEnd();
                int idx = text.indexOf(keyword, caret);
                if (idx == -1 && caret != 0) {
                    idx = text.indexOf(keyword, 0);
                }
                if (idx != -1) {
                    area.selectRange(idx, idx + keyword.length());
                    area.requestFocus();
                    lastIndex[0] = idx;
                }
            });

            // 弹窗显示后调整大小并打印位置
            logDialog.setOnShown(evv -> {
                Window window = logDialog.getDialogPane().getScene() != null ?
                    logDialog.getDialogPane().getScene().getWindow() : null;
                if (window != null) {
                    window.setWidth(width + 100);
                    window.setHeight(height + 100);
                    System.out.println("弹出框位置: x=" + window.getX() + ", y=" + window.getY());
                }
            });

            // 非阻塞显示
            logDialog.show();
        });
    }
    

} 