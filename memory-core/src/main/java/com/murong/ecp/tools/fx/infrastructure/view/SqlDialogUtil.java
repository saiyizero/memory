package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.controller.MainController;
import com.murong.ecp.tools.fx.controller.PaneExeSqlController;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.MenuEnum;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.Modality;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * SQL显示、编辑弹窗
 */
public class SqlDialogUtil {

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
            logDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, ButtonType.APPLY);
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
            Button wrapBtn = new Button("自动换行");
            Button importExecuteBtn = new Button("导入执行");
            Button copyAllBtn = new Button("全部复制");
            

            
            // 创建左侧的导入执行按钮容器
            HBox leftBox = new HBox(5, importExecuteBtn, copyAllBtn);
            leftBox.setAlignment(Pos.CENTER_LEFT);
            
            // 创建右侧的搜索和功能按钮容器
            HBox rightBox = new HBox(6, searchField, upBtn, downBtn, wrapBtn);
            rightBox.setAlignment(Pos.CENTER_RIGHT);
            
            // 创建主搜索栏，左侧放导入执行按钮，右侧放其他元素
            HBox searchBox = new HBox();
            searchBox.setAlignment(Pos.CENTER);
            searchBox.getChildren().addAll(leftBox, rightBox);
            HBox.setHgrow(leftBox, Priority.NEVER);
            HBox.setHgrow(rightBox, Priority.ALWAYS);

            TextArea area = new TextArea(content);
            area.setEditable(true);
            area.setWrapText(false); // 初始不自动换行
            area.setPrefWidth(width - 20);
            area.setPrefHeight(height - 40);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(area, Priority.ALWAYS);

            // 设置背景色和字体样式，参考SQL语法高亮组件
            area.setStyle(
                    "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-color: #1e1e1e;" +
                            "-fx-text-fill: #d4d4d4;" +
                            "-fx-control-inner-background: #1e1e1e;" +
                            "-fx-highlight-fill: #264f78;" +
                            "-fx-highlight-text-fill: #ffffff;" +
                            "-fx-caret-color: #ffffff;"
            );

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
                boolean isJson = false, isXml = false, isSql = false;

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

                // 尝试格式化SQL
                if (!isJson && !isXml) {
                    // SQL格式化功能已移除
                    isSql = false;
                }

                if (!isJson && !isXml && !isSql) {
                    ViewUtils.alertForFail("选中内容不是合法的JSON、XML或SQL");
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

            VBox vbox = new VBox();
            vbox.setSpacing(8);
            vbox.getChildren().addAll(searchBox, area);
            vbox.setPrefSize(width - 20, height - 40);
            vbox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            logDialog.getDialogPane().setContent(vbox);

            // 设置保存按钮事件
            Button saveButton = (Button) logDialog.getDialogPane().lookupButton(ButtonType.APPLY);
            saveButton.setText("保存");
            saveButton.setOnAction(evt -> {
                String editedContent = area.getText();
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("保存文件");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("SQL文件", "*.sql"),
                        new FileChooser.ExtensionFilter("文本文件", "*.txt"),
                        new FileChooser.ExtensionFilter("所有文件", "*.*")
                );

                // 生成默认文件名：${schema}_ddl_yyyyMMdd.sql
                String schema = "";
                try {
                    ApplicationContext context = MrSpringContextHolder.getApplicationContext();
                    if (context != null) {
                        GlobalProperties globalProps = MrSpringContextHolder.getBean(GlobalProperties.class);
                        if (globalProps != null) {
                            schema = globalProps.getDDLSchema();
                        }
                    }
                } catch (Exception e) {
                    // 如果获取schema失败，使用默认值
                    schema = "default";
                }
                
                // 生成当前日期字符串 yyyyMMdd
                String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                
                // 构建默认文件名
                String defaultFileName = schema + "_ddl_" + currentDate + ".sql";
                fileChooser.setInitialFileName(defaultFileName);

                Window window = logDialog.getDialogPane().getScene() != null ? 
                    logDialog.getDialogPane().getScene().getWindow() : null;
                File file = fileChooser.showSaveDialog(window);
                if (file != null) {
                    try {
                        Files.write(file.toPath(), editedContent.getBytes());
                        ViewUtils.alertForSucess("文件已保存到: " + file.getAbsolutePath());
                    } catch (IOException e) {
                        ViewUtils.alertForFail("保存文件失败: " + e.getMessage());
                    }
                }
            });

            // 设置导入执行按钮事件
            importExecuteBtn.setOnAction(evt -> {
                String sqlContent = area.getText();
                if (sqlContent == null || sqlContent.trim().isEmpty()) {
                    ViewUtils.alertForFail("没有内容可以导入");
                    return;
                }
                
                try {
                    // 获取Spring应用上下文
                    ApplicationContext context = MrSpringContextHolder.getApplicationContext();
                    if (context == null) {
                        ViewUtils.alertForFail("无法获取应用上下文");
                        return;
                    }
                    
                    // 获取主控制器
                    MainController mainController = MrSpringContextHolder.getBean(MainController.class);
                    if (mainController == null) {
                        ViewUtils.alertForFail("无法获取主控制器");
                        return;
                    }
                    
                    // 切换到SQL执行标签页
                    mainController.showPage(MenuEnum.EXESQL.getKey(), "/fxml/pane_exesql.fxml");
                    
                    // 延迟执行，确保界面加载完成后再设置内容
                    Platform.runLater(() -> {
                        try {
                            // 获取执行SQL控制器
                            PaneExeSqlController exeSqlController = MrSpringContextHolder.getBean(PaneExeSqlController.class);
                            if (exeSqlController != null && exeSqlController.getSqlTextArea() != null) {
                                // 设置SQL内容
                                exeSqlController.getSqlTextArea().setText(sqlContent);
                                // 关闭当前弹框
                                logDialog.close();
                            } else {
                                ViewUtils.alertForFail("无法获取SQL执行控制器");
                            }
                        } catch (Exception e) {
                            ViewUtils.alertForFail("导入SQL失败: " + e.getMessage());
                        }
                    });
                    
                } catch (Exception e) {
                    ViewUtils.alertForFail("导入执行失败: " + e.getMessage());
                }
            });

            // 设置全部复制按钮事件
            copyAllBtn.setOnAction(evt -> {
                String copyContent = area.getText();
                if (copyContent == null || copyContent.trim().isEmpty()) {
                    ViewUtils.alertForFail("没有内容可以复制");
                    return;
                }
                
                try {
                    // 获取系统剪贴板
                    java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                    java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(copyContent);
                    clipboard.setContents(stringSelection, null);
                    
                    // 使用Tooltip显示复制成功提示
                    Tooltip tooltip = new Tooltip("复制成功");
                    tooltip.setAutoHide(true);
                    tooltip.setShowDelay(javafx.util.Duration.millis(100));
                    tooltip.setHideDelay(javafx.util.Duration.millis(1500));
                    
                    // 在按钮上方显示Tooltip
                    tooltip.show(copyAllBtn, 
                        copyAllBtn.localToScreen(copyAllBtn.getWidth() / 2, 0).getX(),
                        copyAllBtn.localToScreen(0, -30).getY());
                    
                    // 自动隐藏Tooltip
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                    delay.setOnFinished(e -> tooltip.hide());
                    delay.play();
                    
                } catch (Exception e) {
                    ViewUtils.alertForFail("复制失败: " + e.getMessage());
                }
            });

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

            // 弹窗显示后调整大小
            logDialog.setOnShown(evv -> {
                Window window = logDialog.getDialogPane().getScene().getWindow();
                if (window != null) {
                    window.setWidth(width + 100);
                    window.setHeight(height + 100);
                }
            });

            // 非阻塞显示
            logDialog.show();
        });
    }


}