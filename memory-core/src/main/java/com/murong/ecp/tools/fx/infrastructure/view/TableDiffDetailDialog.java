package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import com.murong.ecp.tools.fx.infrastructure.utils.SqlDiffUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.SqlDiffUtil.SqlDiffResult;
import com.murong.ecp.tools.fx.infrastructure.utils.SqlDiffUtil.DiffLine;
import com.murong.ecp.tools.fx.infrastructure.utils.SqlDiffUtil.DiffType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;


import java.net.URL;
import java.util.*;

@Component
public class TableDiffDetailDialog {

    /**
     * 显示差异详情
     */
    public static void showDiffDetail(TableDiffPO diff) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("表结构差异详情");
        dialog.setHeaderText("表名: " + diff.getTableNameSnake() + " | 环境: " + diff.getCompareEnv());
        
        // 设置对话框大小
        dialog.setResizable(true);
        
        // 创建主容器
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(1000);
        content.setPrefHeight(550);
        
        // 创建标题
        Label titleLabel = new Label("表结构对比");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // 创建对比区域
        HBox compareBox = createCompareBox(diff);
        compareBox.setPrefHeight(400);
        
        // 创建按钮区域
        HBox buttonBox = createButtonBox(diff);
        
        content.getChildren().addAll(titleLabel, compareBox, buttonBox);
        dialog.getDialogPane().setContent(content);
        
        // 加载CSS样式
        try {
            String cssPath = "/css/sql-diff-highlighter.css";
            URL cssUrl = TableDiffDetailDialog.class.getResource(cssPath);
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("成功加载CSS: " + cssUrl.toExternalForm());
            } else {
                // 尝试其他路径
                String[] alternativePaths = {"css/sql-diff-highlighter.css", "../css/sql-diff-highlighter.css"};
                for (String path : alternativePaths) {
                    cssUrl = TableDiffDetailDialog.class.getResource(path);
                    if (cssUrl != null) {
                        dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
                        System.out.println("成功加载CSS: " + cssUrl.toExternalForm());
                        break;
                    }
                }
                
                if (cssUrl == null) {
                    System.err.println("无法找到CSS文件，尝试的路径:");
                    System.err.println("  /css/sql-diff-highlighter.css");
                    System.err.println("  css/sql-diff-highlighter.css");
                    System.err.println("  ../css/sql-diff-highlighter.css");
                }
            }
        } catch (Exception e) {
            System.err.println("加载CSS失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 添加CSS类
        dialog.getDialogPane().getStyleClass().add("diff-dialog");
        
        // 设置按钮
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // 显示对话框
        dialog.showAndWait();
        
        // 设置窗口尺寸（在显示后设置）
        dialog.setOnShown(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setWidth(1050);
            stage.setHeight(600);
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.setMaxWidth(1200);
            stage.setMaxHeight(800);
        });
    }

    /**
     * 创建对比区域
     */
    private static HBox createCompareBox(TableDiffPO diff) {
        HBox compareBox = new HBox(10);
        compareBox.setAlignment(Pos.CENTER);
        
        // 获取本地和远程DDL
        String localDDL = getLocalTableDDL(diff);
        String remoteDDL = getRemoteTableDDL(diff);
        
        // 创建本地表结构显示区域
        VBox localBox = createTableBox("本地表结构", localDDL, true);
        
        // 创建分隔线
        VBox separator = createSeparator();
        
        // 创建远程表结构显示区域
        VBox remoteBox = createTableBox("远程表结构", remoteDDL, false);
        
        compareBox.getChildren().addAll(localBox, separator, remoteBox);
        
        // 设置同步滚动
        ScrollPane localScrollPane = (ScrollPane) localBox.getChildren().get(1);
        ScrollPane remoteScrollPane = (ScrollPane) remoteBox.getChildren().get(1);
        setupSyncScroll(localScrollPane, remoteScrollPane);
        
        // 应用差异高亮
        applyDiffHighlighting(localScrollPane, remoteScrollPane, localDDL, remoteDDL);
        
        return compareBox;
    }

    /**
     * 创建表结构显示区域
     */
    private static VBox createTableBox(String title, String ddl, boolean isLocal) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(480);
        box.setPrefHeight(380);
        
        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPrefWidth(480);
        
        // 创建TextFlow并设置DDL内容
        TextFlow textFlow = createStyledTextFlow();
        
        // 清理DDL格式，移除开头和结尾的空行
        String cleanedDdl = cleanDdlFormat(ddl);
        
        System.out.println("=== 设置到TextFlow的DDL ===");
        System.out.println("DDL长度: " + cleanedDdl.length());
        System.out.println("DDL前50个字符: '" + (cleanedDdl.length() > 50 ? cleanedDdl.substring(0, 50) : cleanedDdl) + "'");
        
        // 设置DDL内容到TextFlow
        Text text = new Text(cleanedDdl);
        text.setStyle("-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-line-spacing: 0;");
        textFlow.getChildren().add(text);
        
        // 强制TextFlow重新布局
        textFlow.requestLayout();
        
        // 滚动面板
        ScrollPane scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefWidth(480);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 0;");
        scrollPane.getStyleClass().add("virtualized-scroll-pane");
        
        // 确保滚动条从顶部开始
        scrollPane.setVvalue(0);
        scrollPane.setHvalue(0);
        
        box.getChildren().addAll(titleLabel, scrollPane);
        return box;
    }

    /**
     * 创建分隔线
     */
    private static VBox createSeparator() {
        VBox separator = new VBox();
        separator.setPrefWidth(2);
        separator.setPrefHeight(380);
        separator.setStyle("-fx-background-color: #95a5a6; -fx-border-color: #7f8c8d; -fx-border-width: 1;");
        return separator;
    }

    /**
     * 创建带样式的TextFlow
     */
    private static TextFlow createStyledTextFlow() {
        TextFlow textFlow = new TextFlow();
        textFlow.setStyle(
            "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 12px;" +
            "-fx-background-color: #f8f9fa;" +
            "-fx-text-fill: #2c3e50;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 0;" +
            "-fx-line-spacing: 0;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;"
        );
        // 添加CSS类来覆盖全局样式
        textFlow.getStyleClass().add("ddl-text-flow");
        return textFlow;
    }

    /**
     * 设置同步滚动
     */
    private static void setupSyncScroll(ScrollPane left, ScrollPane right) {
        // 垂直滚动同步
        left.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (Math.abs(newValue.doubleValue() - right.getVvalue()) > 0.01) {
                right.setVvalue(newValue.doubleValue());
            }
        });
        
        right.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (Math.abs(newValue.doubleValue() - left.getVvalue()) > 0.01) {
                left.setVvalue(newValue.doubleValue());
            }
        });
        
        // 水平滚动同步
        left.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (Math.abs(newValue.doubleValue() - right.getHvalue()) > 0.01) {
                right.setHvalue(newValue.doubleValue());
            }
        });
        
        right.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (Math.abs(newValue.doubleValue() - left.getHvalue()) > 0.01) {
                left.setHvalue(newValue.doubleValue());
            }
        });
    }

    /**
     * 应用差异高亮
     */
    private static void applyDiffHighlighting(ScrollPane localScrollPane, ScrollPane remoteScrollPane, String localDDL, String remoteDDL) {
        // 使用专业的SQL比对工具
        SqlDiffResult diffResult = SqlDiffUtil.compareSqlDdl(localDDL, remoteDDL);
        
        // 应用差异高亮到本地文本区域
        applyDiffToTextFlow((TextFlow) localScrollPane.getContent(), diffResult.getLocalLines());
        
        // 应用差异高亮到远程文本区域
        applyDiffToTextFlow((TextFlow) remoteScrollPane.getContent(), diffResult.getRemoteLines());
        
        // 打印差异统计信息
        Map<String, Integer> stats = SqlDiffUtil.getDiffStatistics(diffResult);
        System.out.println("差异统计: " + stats);
        
        // 打印详细差异
        for (SqlDiffUtil.DiffDetail detail : diffResult.getDifferences()) {
            System.out.println("差异: " + detail.getDescription());
        }
    }

    /**
     * 应用差异到TextFlow
     */
    private static void applyDiffToTextFlow(TextFlow textFlow, List<DiffLine> lines) {
        // 清空现有内容
        textFlow.getChildren().clear();
        
        for (int i = 0; i < lines.size(); i++) {
            DiffLine line = lines.get(i);
            String lineText = line.getContent();
            if (lineText.isEmpty()) {
                lineText = " ";
            }
            
            // 创建HBox容器来支持背景色
            HBox lineBox = new HBox();
            lineBox.setStyle(getLineBoxStyle(line.getType()));
            lineBox.setAlignment(Pos.CENTER_LEFT);
            
            // 创建Text对象
            Text text = new Text(lineText);
            text.setStyle(getTextStyle(line.getType()));
            
            // 将Text添加到HBox
            lineBox.getChildren().add(text);
            
            // 将HBox添加到TextFlow
            textFlow.getChildren().add(lineBox);
            
            // 除了最后一行，都添加换行
            if (i < lines.size() - 1) {
                Text newline = new Text("\n");
                textFlow.getChildren().add(newline);
            }
        }
        
        // 添加调试信息
        System.out.println("应用样式到TextFlow，行数: " + lines.size());
    }

    /**
     * 获取行容器样式 - 设置背景色
     */
    private static String getLineBoxStyle(DiffType type) {
        switch (type) {
            case ADDED:
                return "-fx-background-color: #d4edda; -fx-padding: 0 4; -fx-min-height: 16;";
            case DELETED:
                return "-fx-background-color: #f8d7da; -fx-padding: 0 4; -fx-min-height: 16;";
            case MODIFIED:
                return "-fx-background-color: #fff3cd; -fx-padding: 0 4; -fx-min-height: 16;";
            case UNCHANGED:
            default:
                return "-fx-background-color: transparent; -fx-padding: 0 4; -fx-min-height: 16;";
        }
    }

    /**
     * 获取文本样式 - 只设置文字颜色
     */
    private static String getTextStyle(DiffType type) {
        switch (type) {
            case ADDED:
                return "-fx-fill: #000000; -fx-font-weight: bold;";
            case DELETED:
                return "-fx-fill: #000000; -fx-font-weight: bold;";
            case MODIFIED:
                return "-fx-fill: #000000; -fx-font-weight: bold;";
            case UNCHANGED:
            default:
                return "-fx-fill: #2c3e50;";
        }
    }



    /**
     * 创建按钮区域
     */
    private static HBox createButtonBox(TableDiffPO diff) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        Button resolveBtn = new Button("标记为已解决");
        resolveBtn.getStyleClass().add("diff-resolve-btn");
        resolveBtn.setOnAction(e -> {
            // TODO: 调用更新状态的方法
        });

        Button ignoreBtn = new Button("标记为已忽略");
        ignoreBtn.getStyleClass().add("diff-ignore-btn");
        ignoreBtn.setOnAction(e -> {
            // TODO: 调用更新状态的方法
        });

        buttonBox.getChildren().addAll(resolveBtn, ignoreBtn);
        return buttonBox;
    }

    /**
     * 获取本地表DDL
     */
    private static String getLocalTableDDL(TableDiffPO diff) {
        try {
            String localValue = diff.getLocalValue();
            if (localValue != null && !localValue.trim().isEmpty()) {
                // 直接返回存储的DDL内容
                return localValue;
            }
            
            return "-- 无法获取本地表结构\n-- 表名: " + diff.getTableNameSnake();
            
        } catch (Exception e) {
            return "-- 无法获取本地表结构: " + e.getMessage() + "\n-- 表名: " + diff.getTableNameSnake();
        }
    }

    /**
     * 获取远程表DDL
     */
    private static String getRemoteTableDDL(TableDiffPO diff) {
        try {
            String remoteValue = diff.getRemoteValue();
            if (remoteValue != null && !remoteValue.trim().isEmpty()) {
                // 直接返回存储的DDL内容
                return remoteValue;
            }
            
            return "-- 表不存在或无法连接\n-- 表名: " + diff.getTableNameSnake() + "\n-- 环境: " + diff.getCompareEnv();
            
        } catch (Exception e) {
            return "-- 无法获取远程表结构: " + e.getMessage() + "\n-- 表名: " + diff.getTableNameSnake() + "\n-- 环境: " + diff.getCompareEnv();
        }
    }

    /**
     * 清理DDL格式，移除开头和结尾的空行
     */
    private static String cleanDdlFormat(String ddl) {
        if (ddl == null || ddl.isEmpty()) {
            return ddl;
        }
        
        System.out.println("=== TableDiffDetailDialog 清理DDL ===");
        System.out.println("原始DDL长度: " + ddl.length());
        System.out.println("原始DDL前30个字符: '" + (ddl.length() > 30 ? ddl.substring(0, 30) : ddl) + "'");
        
        // 移除开头和结尾的空白字符
        String trimmed = ddl.trim();
        
        // 按行分割并重新构建
        String[] lines = trimmed.split("\n");
        StringBuilder cleaned = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }
            
            // 添加非空行
            if (cleaned.length() > 0) {
                cleaned.append("\n");
            }
            cleaned.append(line);
        }
        
        // 确保没有前导或尾随的换行符
        String result = cleaned.toString().trim();
        
        System.out.println("清理后DDL长度: " + result.length());
        System.out.println("清理后DDL前30个字符: '" + (result.length() > 30 ? result.substring(0, 30) : result) + "'");
        System.out.println("=== TableDiffDetailDialog 清理完成 ===");
        
        return result;
    }

} 