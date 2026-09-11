package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.database.ExecuteSqlService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.SqlSyntaxHighlighter;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


@Component
public class PaneExeSqlController implements Initializable {
    @Autowired
    ExecuteSqlService execSqlService;
    @Autowired
    private GlobalProperties globalProps;
    private SqlSyntaxHighlighter sqlTextArea;

    @FXML
    private TableView<Map<String, Object>> resultTable;
    @FXML
    private ComboBox<String> envComboBox;
    @FXML
    private Button executeButton;
    @FXML
    private Button formatButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button selectButton;
    @FXML
    private SplitPane splitPane;

    // 调用服务方法进行登记
    @FXML
    void handleRegisterAction(ActionEvent event) {
        String sql = sqlTextArea.getText();
        String envName = envComboBox.getValue();
        CrResult<Object> result = execSqlService.registerSql(sql, envName);
        if (result.isSucess()) {
            ViewUtils.alertForSucess(result.getMsgInf());
        } else {
            ViewUtils.alertForFail(result.getMsgInf());
        }
    }

    @FXML
    void handleExecuteAction(ActionEvent event) {
        String sql;
        String selectedText = sqlTextArea.getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            // 如果有选中文本，则执行选中的SQL
            sql = selectedText.trim();
        } else {
            // 如果没有选中文本，则执行全部SQL
            sql = sqlTextArea.getText();
        }
        // 新增：执行SQL时弹出进度条
        Platform.runLater(() -> {
            Dialog<Void> progressDialog = new Dialog<>();
            progressDialog.setTitle("正在执行SQL...");
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(400);
            progressBar.setProgress(-1); // 不确定进度
            String progressText = (selectedText != null && !selectedText.trim().isEmpty()) ? 
                "正在执行选中的SQL..." : "正在执行全部SQL...";
            Label progressLabel = new Label(progressText);
            VBox vbox = new VBox(16, progressLabel, progressBar);
            vbox.setPrefWidth(420);
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            progressDialog.getDialogPane().setContent(vbox);
            progressDialog.setResizable(false);
            progressDialog.show();

            new Thread(() -> {
                try {
                    CrResult<Object> crResult = execSqlService.executeSql(sql, envComboBox.getValue());
                    Platform.runLater(() -> {
                        progressDialog.close();
                        if (!crResult.isSucess()){
                            ViewUtils.alertForFail(crResult.getMsgInf());
                            return;
                        }
                        if(crResult.getData() instanceof Integer) {
                            resultTable.getColumns().clear();
                            resultTable.getItems().clear();
                            resultTable.setVisible(false);
                            resultTable.setPrefHeight(50); // 自动收缩到最小
                            splitPane.setDividerPositions(0.9); // 输入框占90%，表格占10%
                            ViewUtils.alertForSucess("执行成功，执行成功数："+crResult.getData());
                        }else {
                            List<Map<String, Object>> resultMap=(List<Map<String, Object>>)crResult.getData();
                            resultTable.getColumns().clear();
                            resultTable.getItems().clear();
                            if(resultMap != null && !resultMap.isEmpty()) {
                                Map<String, Object> firstRow = resultMap.get(0);
                                for(String col : firstRow.keySet()) {
                                    TableColumn<Map<String, Object>, Object> column = new TableColumn<>(col);
                                    column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().get(col)));
                                    column.setSortable(false); // 禁用排序
                                    
                                    // 表头双击复制功能将在表格初始化后统一设置
                                    
                                    // 设置单元格双击复制
                                    column.setCellFactory(columnParam -> new TableCell<Map<String, Object>, Object>() {
                                        @Override
                                        protected void updateItem(Object item, boolean empty) {
                                            super.updateItem(item, empty);
                                            if (empty || item == null) {
                                                setText(null);
                                            } else {
                                                setText(item.toString());
                                            }
                                        }
                                        
                                        {
                                            setOnMouseClicked(cellEvent -> {
                                                if (cellEvent.getClickCount() == 2) {
                                                    String cellValue = getText();
                                                    if (cellValue != null) {
                                                        copyToClipboard(cellValue, cellEvent);
                                                    }
                                                }
                                            });
                                        }
                                    });
                                    
                                    resultTable.getColumns().add(column);
                                }
                                resultTable.setItems(FXCollections.observableArrayList(resultMap));
                                resultTable.setVisible(true);
                                resultTable.setPrefHeight(500); // 表格自动展开到500像素
                                splitPane.setDividerPositions(0.3); // 输入框占30%，表格占70%
                                
                                // 设置表头双击复制功能
                                setupHeaderDoubleClickCopy();
                            } else {
                                resultTable.setVisible(false);
                                resultTable.setPrefHeight(50); // 自动收缩
                                splitPane.setDividerPositions(0.9); // 输入框占90%，表格占10%
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        progressDialog.close();
                        ViewUtils.alertForFail("SQL执行异常: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    @FXML
    void handleFormatAction(ActionEvent event) {
        String sql = sqlTextArea.getText();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        sqlTextArea.setText(formatSql(sql));
    }

    @FXML
    void handleSelectAction(ActionEvent event) {
        String template = "select * from table_name";
        String currentText = sqlTextArea.getText();
        if (currentText.isEmpty()) {
            sqlTextArea.setText(template);
        } else {
            // 如果文本框已有内容，在末尾添加换行和模板
            sqlTextArea.setText(currentText + "\n" + template);
        }
        // 设置焦点到文本框
        sqlTextArea.requestFocus();
        // 将光标移到末尾
        sqlTextArea.positionCaret(sqlTextArea.getText().length());
    }

    /**
     * PL/SQL Developer风格SQL格式化：
     * - FROM/JOIN等关键字与表名同一行
     * - WHERE/AND等条件对齐
     */
    private String formatSql(String sql) {
        if (sql == null) return "";
        sql = sql.replaceAll("\\s+", " ").trim();
        // 主要关键字大写并换行
        String[] keywords = {"SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "JOIN", "ON", "HAVING", "UNION"};
        for (String kw : keywords) {
            sql = sql.replaceAll("(?i)\\s*" + kw.replace(" ", "\\s+") + "\\s*", "\n" + kw + " ");
        }
        // 合并FROM/JOIN等关键字与表名为一行
        sql = sql.replaceAll("(?i)\n(FROM|LEFT JOIN|RIGHT JOIN|INNER JOIN|OUTER JOIN|JOIN|ON|UNION) ", "\n$1 ");
        sql = sql.replaceAll("(?i)(FROM|LEFT JOIN|RIGHT JOIN|INNER JOIN|OUTER JOIN|JOIN|ON|UNION) ([^\n]+)", "$1 $2");
        // AND/OR前换行并对齐
        sql = sql.replaceAll("(?i)\\s+AND\\s+", "\n  AND ");
        sql = sql.replaceAll("(?i)\\s+OR\\s+", "\n  OR ");
        // 逗号后换行并缩进（仅在SELECT、WHERE、GROUP BY、ORDER BY、HAVING、ON等子句内）
        String[] lines = sql.split("\\n");
        StringBuilder sb = new StringBuilder();
        boolean indent = false;
        for (String line : lines) {
            String trim = line.trim();
            if (trim.isEmpty()) continue;
            String upper = trim.toUpperCase();
            if (upper.matches("SELECT|WHERE|GROUP BY|ORDER BY|HAVING|ON")) {
                sb.append(upper).append("\n");
                indent = true;
            } else if (upper.matches("FROM.*|LEFT JOIN.*|RIGHT JOIN.*|INNER JOIN.*|OUTER JOIN.*|JOIN.*|UNION.*")) {
                sb.append(line.trim()).append("\n");
                indent = false;
            } else {
                String content = indent ? line.replaceAll(",", ",\n    ") : line;
                if (indent) {
                    // 每行缩进4空格
                    String[] contentLines = content.split("\\n");
                    for (String cl : contentLines) {
                        sb.append("    ").append(cl.trim()).append("\n");
                    }
                } else {
                    sb.append(content.trim()).append("\n");
                }
            }
        }
        return sb.toString().replaceAll("(\n)+", "\n").trim();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 初始化环境下拉框，实际环境可从服务获取
        envComboBox.setItems(FXCollections.observableArrayList("dev", "sit", "uat", "poc"));
        envComboBox.getSelectionModel().selectFirst();
        
        // 创建SQL语法高亮组件
        sqlTextArea = new SqlSyntaxHighlighter();
        sqlTextArea.setPromptText("请输入SQL语句...");
        sqlTextArea.setPrefRowCount(40);
        sqlTextArea.setPrefHeight(400);  // 初始高度设置为400像素
        sqlTextArea.setMinHeight(100);   // 允许用户调整到很小的高度
        // 设置背景色和字体样式，参考LogDialogUtil中的TextArea样式
        sqlTextArea.setStyle(
            "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 14px;" +
            "-fx-background-color: #1e1e1e;" +
            "-fx-text-fill: #d4d4d4;" +
            "-fx-control-inner-background: #1e1e1e;" +
            "-fx-highlight-fill: #264f78;" +
            "-fx-highlight-text-fill: #ffffff;" +
            "-fx-caret-color: #ffffff;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-border-color: #3c3c3c;" +
            "-fx-border-width: 1px;" +
            "-fx-padding: 12;"
        );
        
        // 将组件添加到SplitPane中
        splitPane.getItems().set(0, sqlTextArea);
        
        // 设置初始分割位置：输入框占90%，表格占10%
        splitPane.setDividerPositions(0.9);
        
        // 其余初始化逻辑
        resultTable.setEditable(true);
        resultTable.setVisible(false);

        formatButton.setOnAction(this::handleFormatAction);
        clearButton.setOnAction(e -> sqlTextArea.clear());
        
        // 设置select按钮的鼠标悬停效果
        setupSelectButtonHoverEffects();
        
        // 设置执行按钮的鼠标悬停和点击效果
        setupExecuteButtonEffects();
        
        // 使用Platform.runLater确保UI完全加载后再添加登记按钮
        Platform.runLater(() -> {
            try {
                if (formatButton.getParent() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) formatButton.getParent();
                    
                    // 在格式化按钮左侧添加登记按钮
                    createRegisterButtonInHBox(parent);
                }
            } catch (Exception e) {
                System.err.println("添加登记按钮时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // 使用Platform.runLater确保UI完全加载后再添加其他SQL标签按钮
        Platform.runLater(() -> {
            try {
                if (formatButton.getParent() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) formatButton.getParent();
                    
                    // 添加其他SQL标签按钮
                    createOtherSqlTagButtonsInHBox(parent);
                }
            } catch (Exception e) {
                System.err.println("添加SQL标签按钮时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
        

    }


    
    /**
     * 设置select按钮的鼠标悬停效果
     */
    private void setupSelectButtonHoverEffects() {
        // 添加鼠标悬停效果
        selectButton.setOnMouseEntered(e -> {
            selectButton.setStyle(
                "-fx-background-color: #c82333;" +  // 深红色
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 4px 8px;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 70px;" +
                "-fx-max-width: 100px;"
            );
        });
        
        selectButton.setOnMouseExited(e -> {
            selectButton.setStyle(
                "-fx-background-color: #dc3545;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 4px 8px;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 70px;" +
                "-fx-max-width: 100px;"
            );
        });
    }
    
    /**
     * 设置执行按钮的鼠标悬停和点击效果
     */
    private void setupExecuteButtonEffects() {
        // 鼠标悬停效果 - 图标颜色变深
        executeButton.setOnMouseEntered(e -> {
            executeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;"
            );
            // 通过CSS选择器改变SVG颜色
            executeButton.lookup("SVGPath").setStyle("-fx-fill: #047857;");
        });
        
        // 鼠标离开效果 - 恢复原始颜色
        executeButton.setOnMouseExited(e -> {
            executeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;"
            );
            executeButton.lookup("SVGPath").setStyle("-fx-fill: #059669;");
        });
        
        // 鼠标按下效果 - 图标颜色更深
        executeButton.setOnMousePressed(e -> {
            executeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;"
            );
            executeButton.lookup("SVGPath").setStyle("-fx-fill: #065f46;");
        });
        
        // 鼠标释放效果 - 恢复到悬停颜色
        executeButton.setOnMouseReleased(e -> {
            executeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;"
            );
            executeButton.lookup("SVGPath").setStyle("-fx-fill: #047857;");
        });
    }
    
    /**
     * 在HBox中创建登记按钮
     */
    private void createRegisterButtonInHBox(HBox parent) {
        System.out.println("在HBox中创建登记按钮...");
        
        // 创建登记按钮
        Button registerButton = new Button("登记");
        
        // 设置按钮样式 - 蓝色圆角按钮样式
        registerButton.setStyle(
            "-fx-background-color: #007bff;" +  // 蓝色背景
            "-fx-text-fill: white;" +           // 白色文字
            "-fx-font-size: 11px;" +            // 字体大小
            "-fx-font-weight: bold;" +          // 粗体
            "-fx-background-radius: 12px;" +    // 圆角
            "-fx-border-radius: 12px;" +        // 边框圆角
            "-fx-padding: 4px 8px;" +           // 内边距
            "-fx-cursor: hand;" +               // 手型光标
            "-fx-min-width: 70px;" +            // 最小宽度
            "-fx-max-width: 100px;"             // 最大宽度
        );
        
        // 添加鼠标悬停效果
        registerButton.setOnMouseEntered(e -> {
            registerButton.setStyle(
                "-fx-background-color: #0056b3;" +  // 深蓝色
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 4px 8px;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 70px;" +
                "-fx-max-width: 100px;"
            );
        });
        
        registerButton.setOnMouseExited(e -> {
            registerButton.setStyle(
                "-fx-background-color: #007bff;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 4px 8px;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 70px;" +
                "-fx-max-width: 100px;"
            );
        });
        
        // 点击事件 - 登记SQL到数据库
        registerButton.setOnAction(this::handleRegisterAction);
        
        // 将登记按钮插入到格式化按钮之前
        int formatButtonIndex = parent.getChildren().indexOf(formatButton);
        if (formatButtonIndex >= 0) {
            parent.getChildren().add(formatButtonIndex, registerButton);
        } else {
            parent.getChildren().add(registerButton);
        }
        
        System.out.println("登记按钮已添加到HBox中");
    }
    
    /**
     * 在HBox中创建其他SQL标签按钮（除了select）
     */
    private void createOtherSqlTagButtonsInHBox(HBox parent) {
        System.out.println("在HBox中创建其他SQL标签按钮...");
        
        // 创建其他标签按钮（除了select）
        String[] tagLabels = {"modify", "add column"};
        String[] tagTemplates = {
                "alter table table_name add column column_name data_type default ' ' not null",
                "alter table table_name add column column_name data_type default ' ' not null"
        };
        
        for (int i = 0; i < tagLabels.length; i++) {
            Button tagButton = new Button(tagLabels[i]);
            final String template = tagTemplates[i];
            
            // 设置按钮样式 - 按照截图的红色圆角按钮样式
            tagButton.setStyle(
                "-fx-background-color: #dc3545;" +  // 红色背景
                "-fx-text-fill: white;" +           // 白色文字
                "-fx-font-size: 11px;" +            // 字体大小
                "-fx-font-weight: bold;" +          // 粗体
                "-fx-background-radius: 12px;" +    // 圆角
                "-fx-border-radius: 12px;" +        // 边框圆角
                "-fx-padding: 4px 8px;" +           // 内边距
                "-fx-cursor: hand;" +               // 手型光标
                "-fx-min-width: 70px;" +            // 最小宽度
                "-fx-max-width: 100px;"             // 最大宽度
            );
            
            // 添加鼠标悬停效果
            tagButton.setOnMouseEntered(e -> {
                tagButton.setStyle(
                    "-fx-background-color: #c82333;" +  // 深红色
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 12px;" +
                    "-fx-border-radius: 12px;" +
                    "-fx-padding: 4px 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-min-width: 70px;" +
                    "-fx-max-width: 100px;"
                );
            });
            
            tagButton.setOnMouseExited(e -> {
                tagButton.setStyle(
                    "-fx-background-color: #dc3545;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 12px;" +
                    "-fx-border-radius: 12px;" +
                    "-fx-padding: 4px 8px;" +
                    "-fx-cursor: hand;" +
                    "-fx-min-width: 70px;" +
                    "-fx-max-width: 100px;"
                );
            });
            
            // 点击事件 - 插入SQL模板到文本框
            tagButton.setOnAction(e -> {
                String currentText = sqlTextArea.getText();
                if (currentText.isEmpty()) {
                    sqlTextArea.setText(template);
                } else {
                    // 如果文本框已有内容，在末尾添加换行和模板
                    sqlTextArea.setText(currentText + "\n" + template);
                }
                // 设置焦点到文本框
                sqlTextArea.requestFocus();
                // 将光标移到末尾
                sqlTextArea.positionCaret(sqlTextArea.getText().length());
            });
            
            parent.getChildren().add(tagButton);
        }
        
        System.out.println("其他SQL标签按钮已添加到HBox中，按钮数量: " + tagLabels.length);
    }
    
    /**
     * 复制文本到剪贴板
     * @param text 要复制的文本
     * @param event 鼠标事件，用于显示Tooltip位置
     */
    private void copyToClipboard(String text, javafx.scene.input.MouseEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        
        // 使用Tooltip提示
        Tooltip tp = new Tooltip("已复制: " + text);
        tp.show(resultTable, event.getScreenX(), event.getScreenY());
        
        // 2秒后自动隐藏Tooltip
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> tp.hide());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 设置表头双击复制功能（最兼容方案，直接为每列表头设置Label并监听双击）
     */
    private void setupHeaderDoubleClickCopy() {
        Platform.runLater(() -> {
            for (TableColumn<?, ?> column : resultTable.getColumns()) {
                String colText = column.getText();
                Label headerLabel = new Label(colText);
                headerLabel.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
                headerLabel.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        copyToClipboard(colText, event);
                    }
                });
                column.setGraphic(headerLabel);
                // 清除原始文本，避免重复显示
                column.setText("");
            }
        });
    }
    
    /**
     * 获取SQL文本区域
     * @return SQL文本区域
     */
    public SqlSyntaxHighlighter getSqlTextArea() {
        return sqlTextArea;
    }
    
    /**
     * 获取环境下拉框
     * @return 环境下拉框
     */
    public ComboBox<String> getEnvComboBox() {
        return envComboBox;
    }
} 