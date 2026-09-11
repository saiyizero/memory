package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.view.ProgressDialog;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.enums.JavaTypeEnum;
import com.murong.ecp.tools.fx.enums.DatabaseTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BaseDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BaseDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.FilteredEditingCell;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;

import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Excel处理相关导入
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import javafx.stage.FileChooser;

@Component
public class PaneBaseDictController {
    @Autowired
    private BaseDictDao baseDictDao;
    @Autowired
    private GlobalProperties globalProperties;

    @FXML
    private TextField searchField;
    @FXML
    private Button queryButton;
    @FXML
    private Button addButton;
    @FXML
    private Button importButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button saveChangesButton;
    @FXML
    private Label totalCountLabel;
    @FXML
    private TableView<BaseDictPO> baseDictTableView;
    @FXML
    private TableColumn<BaseDictPO, String> nameSnakeColumn;
    @FXML
    private TableColumn<BaseDictPO, String> typeColumn;
    @FXML
    private TableColumn<BaseDictPO, Number> lengthColumn;
    @FXML
    private TableColumn<BaseDictPO, String> dbTypColumn;
    @FXML
    private TableColumn<BaseDictPO, String> defaultValueColumn;
    @FXML
    private TableColumn<BaseDictPO, String> commentCnColumn;

    @FXML
    private TableColumn<BaseDictPO, String> statusColumn;
    @FXML
    private TableColumn<BaseDictPO, Void> actionColumn;

    private ObservableList<BaseDictPO> dictList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProperties)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 设置表格为可编辑
        baseDictTableView.setEditable(true);
        
        // 设置表格列绑定
        nameSnakeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNameSnake()));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        lengthColumn.setCellValueFactory(cellData -> {
            Integer len = cellData.getValue().getLength();
            return new javafx.beans.property.SimpleIntegerProperty(len != null ? len : 0);
        });
        dbTypColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDbTyp()));
        defaultValueColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefaultValue()));
        commentCnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCommentCn()));
        statusColumn.setCellValueFactory(cellData -> {
            String statusCode = cellData.getValue().getStatus();
            if (statusCode != null) {
                DataStatusEnum statusEnum = DataStatusEnum.getByCode(statusCode);
                return new SimpleStringProperty(statusEnum != null ? statusEnum.getDesc() : statusCode);
            }
            return new SimpleStringProperty("");
        });

        // 设置操作列
        actionColumn.setCellFactory(createActionCellFactory());
        
        // 设置可编辑列的单元格工厂和编辑事件
        setupEditableColumns();
        


        baseDictTableView.setItems(dictList);
        
        // 查询按钮事件
        if (queryButton != null) {
            queryButton.setOnAction(event -> doQuery());
        }
        
        // 新增按钮事件
        if (addButton != null) {
            addButton.setOnAction(event -> addBaseDict());
        }
        
        // 导入按钮事件
        if (importButton != null) {
            importButton.setOnAction(event -> importFromExcel());
        }
        
        // 导出按钮事件
        if (exportButton != null) {
            exportButton.setOnAction(event -> exportToExcel());
        }
        
        // 保存更改按钮事件
        if (saveChangesButton != null) {
            saveChangesButton.setOnAction(event -> saveChanges());
            // 初始状态设置为禁用
            saveChangesButton.setDisable(true);
            saveChangesButton.setText("保存更改");
        }
        
        // 搜索框回车键事件
        if (searchField != null) {
            searchField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    doQuery();
                }
            });
        }
        
        // 初始化加载全部（延迟执行，确保所有组件都已初始化）
        javafx.application.Platform.runLater(() -> doQuery());
        
        // 设置字段名列左对齐并添加左边距
        nameSnakeColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 10;");
        
        // 设置其他列居中对齐
        typeColumn.setStyle("-fx-alignment: center;");
        lengthColumn.setStyle("-fx-alignment: center;");
        dbTypColumn.setStyle("-fx-alignment: center;");
        defaultValueColumn.setStyle("-fx-alignment: center;");
        statusColumn.setStyle("-fx-alignment: center;");
        actionColumn.setStyle("-fx-alignment: center;");
        
        // 中文注释列设置为左对齐并添加左边距
        commentCnColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
        
        // 通过设置表格样式确保对齐生效
        baseDictTableView.setStyle("-fx-alignment: center;");
    }

    private Callback<TableColumn<BaseDictPO, Void>, TableCell<BaseDictPO, Void>> createActionCellFactory() {
        return new Callback<TableColumn<BaseDictPO, Void>, TableCell<BaseDictPO, Void>>() {
            @Override
            public TableCell<BaseDictPO, Void> call(TableColumn<BaseDictPO, Void> param) {
                return new TableCell<BaseDictPO, Void>() {
                    private final Button saveBtn = new Button();
                    private final Button delBtn = new Button();
                    private final HBox buttonBox = new HBox(4, saveBtn, delBtn);
                    
                    {
                        // 设置按钮容器居中对齐
                        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                        
                        // 设置保存按钮样式
                        javafx.scene.shape.SVGPath saveIcon = new javafx.scene.shape.SVGPath();
                        saveIcon.setContent("M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.11 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z");
                        saveIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                        saveIcon.setScaleX(0.7);
                        saveIcon.setScaleY(0.7);
                        saveBtn.setGraphic(saveIcon);
                        saveBtn.setTooltip(new Tooltip("保存"));
                        saveBtn.setMinWidth(28);
                        saveBtn.setPrefWidth(28);
                        saveBtn.setMaxWidth(28);
                        saveBtn.setMinHeight(28);
                        saveBtn.setPrefHeight(28);
                        saveBtn.setMaxHeight(28);
                        saveBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                        // 添加鼠标悬停效果
                        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: transparent;"));
                        saveBtn.setOnMousePressed(e -> saveBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                        saveBtn.setOnMouseReleased(e -> saveBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        saveBtn.setOnAction(e -> {
                            BaseDictPO item = getTableView().getItems().get(getIndex());
                            saveSingleRecord(item);
                        });
                        
                        // 设置删除按钮样式
                        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                        trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                        trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                        trashIcon.setScaleX(0.7);
                        trashIcon.setScaleY(0.7);
                        delBtn.setGraphic(trashIcon);
                        delBtn.setTooltip(new Tooltip("删除"));
                        delBtn.setMinWidth(28);
                        delBtn.setPrefWidth(28);
                        delBtn.setMaxWidth(28);
                        delBtn.setMinHeight(28);
                        delBtn.setPrefHeight(28);
                        delBtn.setMaxHeight(28);
                        delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                        // 添加鼠标悬停效果
                        delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent;"));
                        delBtn.setOnMousePressed(e -> delBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                        delBtn.setOnMouseReleased(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        delBtn.setOnAction(e -> {
                            BaseDictPO item = getTableView().getItems().get(getIndex());
                            deleteBaseDict(item);
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonBox);
                        }
                        setStyle("-fx-alignment: center;");
                    }
                };
            }
        };
    }

    /**
     * 新增基础字典
     */
    private void addBaseDict() {
        // 使用Stage而不是Dialog，这样可以更好地控制窗口行为
        Stage dialogStage = new Stage();
        dialogStage.setTitle("新增基础字典");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(380);
        dialogStage.setWidth(550);
        dialogStage.setHeight(420);
        
        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(20, 15, 15, 15));
        
        // 设置网格样式
        grid.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        
        // 设置列约束，确保最小宽度
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(80);
        labelColumn.setPrefWidth(100);
        labelColumn.setMaxWidth(120);
        
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(320);
        inputColumn.setPrefWidth(380);
        inputColumn.setMaxWidth(Double.MAX_VALUE);
        
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);
        
        // 创建标签样式
        String labelStyle = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #495057; -fx-padding: 6 0;";
        
        // 创建输入字段
        TextField nameSnakeField = new TextField();
        nameSnakeField.setPromptText("字段名（下划线命名）");
        nameSnakeField.setMaxWidth(Double.MAX_VALUE);
        nameSnakeField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        // 创建Java类型下拉框
        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll(JavaTypeEnum.getAllTypeNames());
        typeComboBox.setEditable(true);
        typeComboBox.setPromptText("选择或输入Java类型");
        typeComboBox.setMaxWidth(Double.MAX_VALUE);
        typeComboBox.setMinHeight(32);
        typeComboBox.setPrefHeight(32);
        typeComboBox.setMaxHeight(32);
        typeComboBox.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 0; -fx-font-size: 13px;");
        
        TextField lengthField = new TextField();
        lengthField.setPromptText("字段长度");
        lengthField.setMaxWidth(Double.MAX_VALUE);
        lengthField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        // 创建数据库类型下拉框
        ComboBox<String> dbTypComboBox = new ComboBox<>();
        dbTypComboBox.getItems().addAll(DatabaseTypeEnum.getAllTypeNames());
        dbTypComboBox.setEditable(true);
        dbTypComboBox.setPromptText("选择或输入数据库类型");
        dbTypComboBox.setMaxWidth(Double.MAX_VALUE);
        dbTypComboBox.setMinHeight(32);
        dbTypComboBox.setPrefHeight(32);
        dbTypComboBox.setMaxHeight(32);
        dbTypComboBox.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 0; -fx-font-size: 13px;");
        
        TextField defaultValueField = new TextField();
        defaultValueField.setPromptText("默认值");
        defaultValueField.setMaxWidth(Double.MAX_VALUE);
        defaultValueField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        TextField commentCnField = new TextField();
        commentCnField.setPromptText("中文注释");
        commentCnField.setMaxWidth(Double.MAX_VALUE);
        commentCnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        // 添加到网格
        Label nameSnakeLabel = new Label("字段名:");
        nameSnakeLabel.setStyle(labelStyle);
        grid.add(nameSnakeLabel, 0, 0);
        grid.add(nameSnakeField, 1, 0);
        
        Label typeLabel = new Label("Java类型:");
        typeLabel.setStyle(labelStyle);
        grid.add(typeLabel, 0, 1);
        grid.add(typeComboBox, 1, 1);
        
        Label lengthLabel = new Label("字段长度:");
        lengthLabel.setStyle(labelStyle);
        grid.add(lengthLabel, 0, 2);
        grid.add(lengthField, 1, 2);
        
        Label dbTypLabel = new Label("数据库类型:");
        dbTypLabel.setStyle(labelStyle);
        grid.add(dbTypLabel, 0, 3);
        grid.add(dbTypComboBox, 1, 3);
        
        Label defaultValueLabel = new Label("默认值:");
        defaultValueLabel.setStyle(labelStyle);
        grid.add(defaultValueLabel, 0, 4);
        grid.add(defaultValueField, 1, 4);
        
        Label commentCnLabel = new Label("中文注释:");
        commentCnLabel.setStyle(labelStyle);
        grid.add(commentCnLabel, 0, 5);
        grid.add(commentCnField, 1, 5);
        
        // 添加提示标签
        Label tipLabel = new Label("");
        tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc3545; -fx-padding: 5 0;");
        tipLabel.setWrapText(true);
        tipLabel.setMaxWidth(Double.MAX_VALUE);
        grid.add(tipLabel, 1, 6); // 在第6行显示提示
        
        // 添加按钮区域
        HBox buttonBox = new HBox(10); // 10px间距
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        // 设置列的增长策略，让输入框列占满剩余空间
        GridPane.setHgrow(nameSnakeField, Priority.ALWAYS);
        GridPane.setHgrow(typeComboBox, Priority.ALWAYS);
        GridPane.setHgrow(lengthField, Priority.ALWAYS);
        GridPane.setHgrow(dbTypComboBox, Priority.ALWAYS);
        GridPane.setHgrow(defaultValueField, Priority.ALWAYS);
        GridPane.setHgrow(commentCnField, Priority.ALWAYS);
        
        // 创建场景并设置内容
        Scene scene = new Scene(grid);
        dialogStage.setScene(scene);
        
        // 创建自定义保存按钮
        Button saveButton = new Button("保存");
        saveButton.setDefaultButton(true);
        saveButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-font-size: 13px; -fx-cursor: hand;");
        
        // 创建取消按钮
        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-font-size: 13px; -fx-cursor: hand;");
        
        // 设置保存按钮的点击事件
        saveButton.setOnAction(event -> {
            // 验证必填字段
            String nameSnake = nameSnakeField.getText().trim();
            String type = typeComboBox.getValue() != null ? typeComboBox.getValue().trim() : "";
            
            // 检查必填字段
            if (nameSnake.isEmpty()) {
                tipLabel.setText("字段名不能为空");
                return;
            }
            if (type.isEmpty()) {
                tipLabel.setText("Java类型不能为空");
                return;
            }
            
            // 清除之前的错误提示
            tipLabel.setText("");
            
            // 创建新的基础字典对象
            BaseDictPO newBaseDict = new BaseDictPO();
            newBaseDict.setNameSnake(nameSnake);
            newBaseDict.setType(type);
            
            // 处理长度字段
            String lengthText = lengthField.getText().trim();
            if (!lengthText.isEmpty()) {
                try {
                    int length = Integer.parseInt(lengthText);
                    if (length <= 0) {
                        tipLabel.setText("字段长度必须大于0");
                        return;
                    }
                    newBaseDict.setLength(length);
                } catch (NumberFormatException e) {
                    tipLabel.setText("字段长度必须是有效的数字");
                    return;
                }
            } else {
                newBaseDict.setLength(null);
            }
            
            newBaseDict.setDbTyp(dbTypComboBox.getValue() != null ? dbTypComboBox.getValue().trim() : "");
            newBaseDict.setDefaultValue(defaultValueField.getText().trim());
            newBaseDict.setCommentCn(commentCnField.getText().trim());
            
            // 设置状态为待审核
            newBaseDict.setStatus(DataStatusEnum.PENDING.getCode());
            
            try {
                // 保存到数据库
                baseDictDao.save(newBaseDict);
                
                // 刷新列表
                doQuery();
                
                // 关闭对话框
                dialogStage.close();

            } catch (Exception e) {
                e.printStackTrace();
                tipLabel.setText("添加基础字典时发生错误: " + e.getMessage());
            }
        });
        
        // 设置取消按钮的点击事件
        cancelButton.setOnAction(event -> {
            dialogStage.close();
        });
        
        // 将按钮添加到按钮区域
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        grid.add(buttonBox, 1, 7); // 在第7行显示按钮区域
        
        // 显示对话框
        dialogStage.showAndWait();
    }
    
    /**
     * 保存单条记录
     */
    private void saveSingleRecord(BaseDictPO baseDict) {
        try {
            // 检查是否已存在
            BaseDictPO baseDictPO = new BaseDictPO();
            baseDictPO.setNameSnake(baseDict.getNameSnake());
            BaseDictPO existing = baseDictDao.queryOne(baseDictPO);
            if (existing != null) {
                // 更新现有记录
                baseDictDao.updateByOne(baseDict, createWhereCondition(baseDict));
                ViewUtils.alertForSucess("记录更新成功！");
            } else {
                // 插入新记录
                baseDictDao.save(baseDict);
                ViewUtils.alertForSucess("记录保存成功！");
            }
            
            // 刷新列表
            doQuery();
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("保存记录时发生错误: " + e.getMessage());
        }
    }

    private void deleteBaseDict(BaseDictPO baseDict) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除基础字典 '" + baseDict.getNameSnake() + "' 吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // 从数据库删除
                    baseDictDao.delete(baseDict);
                    
                    // 从列表中移除
                    dictList.remove(baseDict);
                    baseDictTableView.refresh();
                    
                    // 更新总数统计
                    if (totalCountLabel != null) {
                        totalCountLabel.setText("总数: " + dictList.size());
                    }
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("删除成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("基础字典已删除");
                    successAlert.showAndWait();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("删除失败");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("删除基础字典时发生错误: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }



    /**
     * 设置可编辑列的单元格工厂和编辑事件
     */
    private void setupEditableColumns() {
        StringConverter<String> converter = new DefaultStringConverter();
        
        // 字段名列 - 可编辑
        nameSnakeColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        nameSnakeColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            item.setNameSnake(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // Java类型列 - 可编辑，支持下拉选择
        ObservableList<String> typeOptions = FXCollections.observableArrayList(JavaTypeEnum.getAllTypeNames());
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(typeOptions));
        typeColumn.setEditable(true);
        typeColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            item.setType(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 长度列 - 可编辑，需要特殊处理因为列类型是Number
        lengthColumn.setCellFactory(col -> new TableCell<BaseDictPO, Number>() {
            private final TextField textField = new TextField();
            
            {
                textField.setMaxWidth(Double.MAX_VALUE);
                textField.setStyle("-fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: transparent; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 13px; -fx-alignment: center;");
                textField.setPromptText("如: 18,2");
                
                textField.setOnAction(e -> {
                    commitEdit();
                });
                
                textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        commitEdit();
                    }
                });
            }
            
            private void commitEdit() {
                BaseDictPO item = getTableView().getItems().get(getIndex());
                if (item != null) {
                    try {
                        String text = textField.getText().trim();
                        if (!text.isEmpty()) {
                            // 支持小数格式，如：18,2
                            if (text.contains(",")) {
                                String[] parts = text.split(",");
                                if (parts.length == 2) {
                                    int length = Integer.parseInt(parts[0].trim());
                                    int decimal = Integer.parseInt(parts[1].trim());
                                    if (length > 0 && decimal >= 0) {
                                        // 将长度和小数位数组合存储，格式：长度*1000 + 小数位数
                                        item.setLength(length * 1000 + decimal);
                                    }
                                }
                            } else {
                                // 普通整数格式
                                int length = Integer.parseInt(text);
                                if (length > 0) {
                                    item.setLength(length);
                                }
                            }
                        } else {
                            item.setLength(null);
                        }
                        // 标记数据已修改
                        markDataModified();
                    } catch (NumberFormatException ex) {
                        // 长度格式错误，保持原值
                    }
                }
                getTableView().refresh();
            }
            
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (isEditing()) {
                        setText(null);
                        setGraphic(textField);
                        if (item != null) {
                            // 显示格式化的长度值
                            textField.setText(formatLengthForDisplay(item.intValue()));
                        }
                    } else {
                        setText(item != null ? formatLengthForDisplay(item.intValue()) : "");
                        setGraphic(null);
                    }
                }
                // 设置文字居中
                setStyle("-fx-alignment: center;");
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                if (getItem() != null) {
                    textField.setText(formatLengthForDisplay(getItem().intValue()));
                }
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
                textField.selectAll();
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() != null ? formatLengthForDisplay(getItem().intValue()) : "");
                setGraphic(null);
            }
        });
        
        // 数据库类型列 - 可编辑，支持下拉选择
        ObservableList<String> dbTypeOptions = FXCollections.observableArrayList(DatabaseTypeEnum.getAllTypeNames());
        dbTypColumn.setCellFactory(ComboBoxTableCell.forTableColumn(dbTypeOptions));
        dbTypColumn.setEditable(true);
        dbTypColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            item.setDbTyp(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 默认值列 - 可编辑
        defaultValueColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        defaultValueColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            item.setDefaultValue(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 中文注释列 - 可编辑
        commentCnColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        commentCnColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            item.setCommentCn(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 状态列 - 使用下拉选择
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            DataStatusEnum.PENDING.getDesc(),
            DataStatusEnum.REVIEW.getDesc(),
            DataStatusEnum.COMPLETED.getDesc()
        );
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(statusOptions));
        statusColumn.setEditable(true);
        statusColumn.setOnEditCommit(event -> {
            BaseDictPO item = event.getRowValue();
            String selectedStatusDesc = event.getNewValue();
            if (selectedStatusDesc != null) {
                for (DataStatusEnum statusEnum : DataStatusEnum.values()) {
                    if (statusEnum.getDesc().equals(selectedStatusDesc)) {
                        item.setStatus(statusEnum.getCode());
                        break;
                    }
                }
            }
            // 标记数据已修改
            markDataModified();
        });
    }
    

    
    private void doQuery() {
        String searchText = searchField != null ? searchField.getText() : "";
        
        // 使用BaseDictDao的searchByName方法进行搜索
        List<BaseDictPO> list = baseDictDao.searchByName(searchText);
        
        dictList.setAll(list);
        baseDictTableView.refresh();
        
        if (totalCountLabel != null) {
            totalCountLabel.setText("总数: " + list.size());
        }
    }
    


    /**
     * 导入Excel文件
     */
    private void importFromExcel() {
        try {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择要导入的Excel文件");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
            );
            
            // 获取当前窗口
            Stage stage = (Stage) importButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            
            if (selectedFile != null) {
                // 显示进度对话框
                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.setDialogTitle("导入进度");
                progressDialog.setDialogHeaderText("正在导入Excel文件...");
                progressDialog.setProgressText("请稍候...");
                progressDialog.setProgressValue(ProgressIndicator.INDETERMINATE_PROGRESS);
                
                // 在新线程中执行导入操作
                javafx.concurrent.Task<Void> importTask = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        importExcelData(selectedFile);
                        return null;
                    }
                };
                
                importTask.setOnSucceeded(event -> {
                    // 使用多重关闭策略确保进度对话框被关闭
                    progressDialog.forceCloseImmediate();
                    
                    // 延迟再次尝试关闭，确保对话框被完全关闭
                    progressDialog.delayedClose(100);
                    
                    // 延迟显示成功消息，确保进度对话框已关闭
                    javafx.animation.PauseTransition delay = 
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    delay.setOnFinished(e -> {
                        // 最后检查一次，确保进度对话框已关闭
                        if (progressDialog.isShowing()) {
                            progressDialog.forceCloseImmediate();
                        }
                        doQuery(); // 刷新数据
                        // 使用Platform.runLater确保在正确的时机显示提示
                        javafx.application.Platform.runLater(() -> {
                            ViewUtils.alertForSucess("Excel导入成功！");
                        });
                    });
                    delay.play();
                });
                
                importTask.setOnFailed(event -> {
                    // 使用多重关闭策略确保进度对话框被关闭
                    progressDialog.forceCloseImmediate();
                    
                    // 延迟再次尝试关闭，确保对话框被完全关闭
                    progressDialog.delayedClose(100);
                    
                    // 延迟显示失败消息，确保进度对话框已关闭
                    javafx.animation.PauseTransition delay = 
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    delay.setOnFinished(e -> {
                        // 最后检查一次，确保进度对话框已关闭
                        if (progressDialog.isShowing()) {
                            progressDialog.forceCloseImmediate();
                        }
                        // 使用Platform.runLater确保在正确的时机显示提示
                        javafx.application.Platform.runLater(() -> {
                            ViewUtils.alertForFail("Excel导入失败: " + importTask.getException().getMessage());
                        });
                    });
                    delay.play();
                });
                
                importTask.setOnCancelled(event -> {
                    // 使用多重关闭策略确保进度对话框被关闭
                    progressDialog.forceCloseImmediate();
                    progressDialog.delayedClose(100);
                });
                
                // 获取当前窗口作为父窗口
                Stage currentStage = (Stage) importButton.getScene().getWindow();
                progressDialog.showProgressDialog(currentStage);
                new Thread(importTask).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("导入Excel时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行Excel数据导入
     */
    private void importExcelData(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = createWorkbook(file, fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            List<BaseDictPO> importedData = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            // 从第二行开始读取数据（第一行是标题）
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                try {
                    BaseDictPO baseDict = parseRowToBaseDict(row);
                    if (baseDict != null) {
                        importedData.add(baseDict);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("解析第" + (rowIndex + 1) + "行数据时出错: " + e.getMessage());
                }
            }
            
            // 批量保存到数据库
            if (!importedData.isEmpty()) {
                for (BaseDictPO baseDict : importedData) {
                    try {
                        // 检查是否已存在
                        BaseDictPO existing = baseDictDao.queryOne(baseDict);
                        if (existing != null) {
                            // 更新现有记录
                            baseDictDao.updateByOne(baseDict, createWhereCondition(baseDict));
                        } else {
                            // 插入新记录
                            baseDictDao.save(baseDict);
                        }
                    } catch (Exception e) {
                        System.err.println("保存数据时出错: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("导入完成: 成功 " + successCount + " 条，失败 " + errorCount + " 条");
            
        } catch (Exception e) {
            throw new IOException("读取Excel文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据文件类型创建Workbook
     */
    private Workbook createWorkbook(File file, FileInputStream fis) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("不支持的文件格式");
        }
    }
    
    /**
     * 解析行数据为BaseDictPO对象
     */
    private BaseDictPO parseRowToBaseDict(org.apache.poi.ss.usermodel.Row row) {
        BaseDictPO baseDict = new BaseDictPO();
        
        try {
            // 设置状态为待审核
            baseDict.setStatus(DataStatusEnum.PENDING.getCode());
            
            // 读取各列数据
            baseDict.setNameSnake(getCellValueAsString(row.getCell(0))); // 字段名
            baseDict.setType(getCellValueAsString(row.getCell(1))); // Java类型
            baseDict.setLength(getCellValueAsInteger(row.getCell(2))); // 长度
            baseDict.setDbTyp(getCellValueAsString(row.getCell(3))); // 数据库类型
            baseDict.setDefaultValue(getCellValueAsString(row.getCell(4))); // 默认值
            baseDict.setCommentCn(getCellValueAsString(row.getCell(5))); // 中文注释

            
            // 验证必填字段
            if (baseDict.getNameSnake() == null || baseDict.getNameSnake().trim().isEmpty() ||
                baseDict.getType() == null || baseDict.getType().trim().isEmpty()) {
                return null; // 跳过无效数据
            }
            
            return baseDict;
            
        } catch (Exception e) {
            System.err.println("解析行数据时出错: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
    
    /**
     * 获取单元格的整数值
     */
    private Integer getCellValueAsInteger(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String strValue = cell.getStringCellValue().trim();
                    return strValue.isEmpty() ? null : Integer.parseInt(strValue);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 创建更新条件
     */
    private BaseDictPO createWhereCondition(BaseDictPO baseDict) {
        BaseDictPO wherePO = new BaseDictPO();
        wherePO.setNameSnake(baseDict.getNameSnake());
        return wherePO;
    }

    /**
     * 导出Excel文件
     */
    private void exportToExcel() {
        try {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择导出Excel文件保存位置");
            fileChooser.setInitialFileName("基础字典_" + System.currentTimeMillis() + ".xlsx");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
            );
            
            // 获取当前窗口
            Stage stage = (Stage) exportButton.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);
            
            if (selectedFile != null) {
                // 显示进度对话框
                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.setDialogTitle("导出进度");
                progressDialog.setDialogHeaderText("正在导出Excel文件...");
                progressDialog.setProgressText("请稍候...");
                progressDialog.setProgressValue(ProgressIndicator.INDETERMINATE_PROGRESS);
                
                // 在新线程中执行导出操作
                javafx.concurrent.Task<Void> exportTask = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        exportExcelData(selectedFile);
                        return null;
                    }
                };
                
                exportTask.setOnSucceeded(event -> {
                    // 使用多重关闭策略确保进度对话框被关闭
                    progressDialog.forceCloseImmediate();
                    
                    // 延迟再次尝试关闭，确保对话框被完全关闭
                    progressDialog.delayedClose(100);
                    
                    // 延迟显示成功消息，确保进度对话框已关闭
                    javafx.animation.PauseTransition delay = 
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    delay.setOnFinished(e -> {
                        // 最后检查一次，确保进度对话框已关闭
                        if (progressDialog.isShowing()) {
                            progressDialog.forceCloseImmediate();
                        }
                        // 使用Platform.runLater确保在正确的时机显示提示
                        javafx.application.Platform.runLater(() -> {
                            ViewUtils.alertForSucess("Excel导出成功！文件保存在: " + selectedFile.getAbsolutePath());
                        });
                    });
                    delay.play();
                });
                
                exportTask.setOnFailed(event -> {
                    // 使用多重关闭策略确保进度对话框被关闭
                    progressDialog.forceCloseImmediate();
                    
                    // 延迟再次尝试关闭，确保对话框被完全关闭
                    progressDialog.delayedClose(100);
                    
                    // 延迟显示失败消息，确保进度对话框已关闭
                    javafx.animation.PauseTransition delay = 
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    delay.setOnFinished(e -> {
                        // 最后检查一次，确保进度对话框已关闭
                        if (progressDialog.isShowing()) {
                            progressDialog.forceCloseImmediate();
                        }
                        // 使用Platform.runLater确保在正确的时机显示提示
                        javafx.application.Platform.runLater(() -> {
                            ViewUtils.alertForFail("Excel导出失败: " + exportTask.getException().getMessage());
                        });
                    });
                    delay.play();
                });
                
                exportTask.setOnCancelled(event -> {
                    // 使用安全关闭方法
                    progressDialog.safeClose();
                });
                
                // 获取当前窗口作为父窗口
                Stage currentStage = (Stage) exportButton.getScene().getWindow();
                progressDialog.showProgressDialog(currentStage);
                new Thread(exportTask).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("导出Excel时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行Excel数据导出
     */
    private void exportExcelData(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("基础字典");
            
            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建标题行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                "字段名", "Java类型", "长度", "数据库类型", "默认值", "中文注释", "状态"
            };
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256); // 设置列宽
            }
            
            // 导出当前数据
            List<BaseDictPO> dataToExport = new ArrayList<>(dictList);
            
            // 如果没有数据，尝试从数据库查询
            if (dataToExport.isEmpty()) {
                dataToExport = baseDictDao.searchByName("");
            }
            
            // 填充数据行
            int rowIndex = 1;
            for (BaseDictPO baseDict : dataToExport) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowIndex++);
                
                dataRow.createCell(0).setCellValue(baseDict.getNameSnake() != null ? baseDict.getNameSnake() : "");
                dataRow.createCell(1).setCellValue(baseDict.getType() != null ? baseDict.getType() : "");
                dataRow.createCell(2).setCellValue(baseDict.getLength() != null ? baseDict.getLength() : 0);
                dataRow.createCell(3).setCellValue(baseDict.getDbTyp() != null ? baseDict.getDbTyp() : "");
                dataRow.createCell(4).setCellValue(baseDict.getDefaultValue() != null ? baseDict.getDefaultValue() : "");
                dataRow.createCell(5).setCellValue(baseDict.getCommentCn() != null ? baseDict.getCommentCn() : "");
                
                // 状态列
                String statusDesc = "";
                if (baseDict.getStatus() != null) {
                    DataStatusEnum statusEnum = DataStatusEnum.getByCode(baseDict.getStatus());
                    statusDesc = statusEnum != null ? statusEnum.getDesc() : baseDict.getStatus();
                }
                dataRow.createCell(6).setCellValue(statusDesc);
            }
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            
        } catch (Exception e) {
            throw new IOException("创建Excel文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建标题行样式
     */
    private org.apache.poi.ss.usermodel.CellStyle createHeaderStyle(Workbook workbook) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }
    
    /**
     * 格式化长度显示，支持小数格式
     * @param lengthValue 长度值
     * @return 格式化后的字符串
     */
    private String formatLengthForDisplay(int lengthValue) {
        if (lengthValue >= 1000) {
            // 如果值大于等于1000，说明包含小数位数信息
            int length = lengthValue / 1000;
            int decimal = lengthValue % 1000;
            if (decimal > 0) {
                return length + "," + decimal;
            } else {
                return String.valueOf(length);
            }
        } else {
            return String.valueOf(lengthValue);
        }
    }
    
    /**
     * 标记数据已修改
     */
    private void markDataModified() {
        // 启用保存按钮，提示用户有未保存的更改
        if (saveChangesButton != null) {
            saveChangesButton.setDisable(false);
            saveChangesButton.setText("保存更改 *");
        }
    }
    
    /**
     * 保存表格中的更改
     */
    private void saveChanges() {
        try {
            int successCount = 0;
            int errorCount = 0;
            
            for (BaseDictPO baseDict : dictList) {
                try {
                    // 检查是否已存在
                    BaseDictPO existing = baseDictDao.queryOne(baseDict);
                    if (existing != null) {
                        // 更新现有记录
                        baseDictDao.updateByOne(baseDict, createWhereCondition(baseDict));
                    } else {
                        // 插入新记录
                        baseDictDao.save(baseDict);
                    }
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("保存基础字典 '" + baseDict.getNameSnake() + "' 时出错: " + e.getMessage());
                }
            }
            
            // 刷新列表
            doQuery();
            
            // 重置保存按钮状态
            if (saveChangesButton != null) {
                saveChangesButton.setDisable(true);
                saveChangesButton.setText("保存更改");
            }
            
            // 显示保存结果
            if (errorCount == 0) {
                ViewUtils.alertForSucess("保存成功！共保存 " + successCount + " 条记录。");
            } else {
                ViewUtils.alertForSucess("保存完成！成功 " + successCount + " 条，失败 " + errorCount + " 条。");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("保存时发生错误: " + e.getMessage());
        }
    }
}
