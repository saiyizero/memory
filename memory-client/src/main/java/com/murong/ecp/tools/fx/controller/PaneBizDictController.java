package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.view.EnumSelectDialog;
import com.murong.ecp.tools.fx.domain.view.ProgressDialog;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.enums.JavaTypeEnum;
import com.murong.ecp.tools.fx.enums.DatabaseTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.BaseDictRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.BatchWriteResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.BizDictRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.EnumDictRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BaseDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.FilteredEditingCell;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PaneBizDictController {
    @Autowired
    private BizDictRpcService bizDictRpcService;
    @Autowired
    private EnumDictRpcService enumDictRpcService;
    @Autowired
    private BaseDictRpcService baseDictRpcService;
    @Autowired
    private GlobalProperties globalProperties;
    @Autowired
    private EnumSelectDialog enumSelectDialog;

    @FXML
    private ComboBox<String> appNameComboBox;
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
    private TableView<BizDictPO> bizDictTableView;
    @FXML
    private TableColumn<BizDictPO, String> appNameColumn;
    @FXML
    private TableColumn<BizDictPO, String> nameCamelColumn;
    @FXML
    private TableColumn<BizDictPO, String> nameSnakeColumn;
    @FXML
    private TableColumn<BizDictPO, Void> dependencyColumn;
    @FXML
    private TableColumn<BizDictPO, String> typeColumn;
    @FXML
    private TableColumn<BizDictPO, String> dbTypColumn;
    @FXML
    private TableColumn<BizDictPO, String> statusColumn;
    @FXML
    private TableColumn<BizDictPO, String> enumColumn;
    @FXML
    private TableColumn<BizDictPO, String> enumRefColumn;
    @FXML
    private TableColumn<BizDictPO, String> defaultValueColumn;
    @FXML
    private TableColumn<BizDictPO, String> commentCnColumn;
    @FXML
    private TableColumn<BizDictPO, String> commentEnColumn;
    @FXML
    private TableColumn<BizDictPO, Number> lengthColumn;
    @FXML
    private TableColumn<BizDictPO, Void> actionColumn;

    private ObservableList<BizDictPO> dictList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProperties)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 设置表格为可编辑
        bizDictTableView.setEditable(true);
        
        // 设置表格列绑定
        appNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAppName()));
        nameCamelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNameCamel()));
        nameSnakeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNameSnake()));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        dbTypColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDbTyp()));
        statusColumn.setCellValueFactory(cellData -> {
            String statusCode = cellData.getValue().getStatus();
            if (statusCode != null) {
                DataStatusEnum statusEnum = DataStatusEnum.getByCode(statusCode);
                return new SimpleStringProperty(statusEnum != null ? statusEnum.getDesc() : statusCode);
            }
            return new SimpleStringProperty("");
        });
        // enumColumn（引用列）的显示逻辑：优先显示enumNme，其次显示baseRef
        enumColumn.setCellValueFactory(cellData -> {
            BizDictPO item = cellData.getValue();

            String displayValue;
            if (item.getEnumNme() != null && !item.getEnumNme().trim().isEmpty()) {
                // 如果enumNme有值，显示enumNme
                displayValue = item.getEnumNme();
                System.out.println("选择显示enumNme: " + displayValue);
            } else if (item.getBaseRef() != null && !item.getBaseRef().trim().isEmpty()) {
                // 如果enumNme为空但baseRef有值，显示baseRef
                displayValue = item.getBaseRef();
                System.out.println("选择显示baseRef: " + displayValue);
            } else {
                // 如果两者都为空，显示空字符串
                displayValue = "";
            }

            return new SimpleStringProperty(displayValue);
        });
        // enumRefColumn是隐藏列，使用简单的绑定
        enumRefColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEnumRef()));
        defaultValueColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDefaultValue()));
        commentCnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCommentCn()));
        commentEnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCommentEn()));
        lengthColumn.setCellValueFactory(cellData -> {
            Integer len = cellData.getValue().getLength();
            return new javafx.beans.property.SimpleIntegerProperty(len != null ? len : 0);
        });

        // 中文描述和英文描述列的单元格工厂会在setupColumnCellFactories中统一设置

        // 设置依赖列
        dependencyColumn.setCellFactory(createDependencyCellFactory());
        
        // 设置操作列
        actionColumn.setCellFactory(createActionCellFactory());
        
        // 设置枚举列双击事件
        enumColumn.setCellFactory(col -> new TableCell<BizDictPO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                // 设置引用列为左对齐并添加10像素左边距，与PaneBaseDictController的字段名列格式保持一致
                setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 10;");
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
            }
        });
        
        // 为所有列设置单元格工厂（确保显示正确）
        setupColumnCellFactories();
        
        // 为表格添加双击事件处理
        bizDictTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                System.out.println("检测到双击事件");
                BizDictPO selectedItem = bizDictTableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    System.out.println("选中项目: " + selectedItem.getNameCamel());
                    // 获取点击的列对象
                    TableColumn<BizDictPO, ?> clickedColumn = null;
                    
                    // 使用表格的焦点模型获取点击的列
                    TablePosition<BizDictPO, ?> pos = bizDictTableView.getFocusModel().getFocusedCell();
                    if (pos != null) {
                        // 获取所有可见列的列表
                        List<TableColumn<BizDictPO, ?>> visibleColumns = new ArrayList<>();
                        for (TableColumn<BizDictPO, ?> column : bizDictTableView.getColumns()) {
                            if (column.isVisible()) {
                                visibleColumns.add(column);
                            }
                        }
                        
                        // 根据列索引获取对应的可见列
                        int columnIndex = pos.getColumn();
                        if (columnIndex >= 0 && columnIndex < visibleColumns.size()) {
                            clickedColumn = visibleColumns.get(columnIndex);
                        }
                    }
                    
                    if (clickedColumn != null) {
                        System.out.println("点击的列: " + clickedColumn.getText());
                        
                        // 根据具体的列来处理双击事件
                        if (clickedColumn == enumColumn) {
                            System.out.println("处理枚举列双击");
                            // 枚举列：根据是否有值决定显示详情还是选择编辑框
                            String enumValue = selectedItem.getEnumNme();
                            if (enumValue != null && !enumValue.trim().isEmpty()) {
                                showEnumDetails(selectedItem);
                            } else {
                                showEnumSelectionDialog(selectedItem);
                            }
                        } else if (clickedColumn == actionColumn) {
                            System.out.println("操作列双击，不处理");
                            // 操作列：不处理双击
                        }
                    } else {
                        System.out.println("无法确定点击的列");
                    }
                } else {
                    System.out.println("没有选中项目");
                }
            }
        });

        bizDictTableView.setItems(dictList);
        
        // 初始化微服务下拉框
        initializeAppNameComboBox();
        
        // 查询按钮事件
        if (queryButton != null) {
            queryButton.setOnAction(event -> doQuery());
        }
        
        // 新增按钮事件
        if (addButton != null) {
            addButton.setOnAction(event -> addBizDict());
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
        
        // 设置所有列居中对齐
        appNameColumn.setStyle("-fx-alignment: center;");
        // 业务字段列和描述列设置为左对齐并添加左边距
        nameCamelColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
        nameSnakeColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
        commentCnColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
        commentEnColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
        dependencyColumn.setStyle("-fx-alignment: center;");
        typeColumn.setStyle("-fx-alignment: center;");
        statusColumn.setStyle("-fx-alignment: center;");
        // 引用列设置为左对齐并添加10像素左边距，与PaneBaseDictController的字段名列格式保持一致
        enumColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 10;");
        defaultValueColumn.setStyle("-fx-alignment: center;");
        lengthColumn.setStyle("-fx-alignment: center;");
        dbTypColumn.setStyle("-fx-alignment: center;");
        actionColumn.setStyle("-fx-alignment: center;");
        
        // 设置归属系统字段左右上下都居中
        appNameColumn.setStyle("-fx-alignment: center; -fx-text-alignment: center;");
        
        // 中文描述、英文描述列已在上面设置为左对齐
        
        // 通过设置表格样式确保对齐生效
        bizDictTableView.setStyle("-fx-alignment: center;");
        
        // 设置可编辑列的单元格工厂和编辑事件
        setupEditableColumns();
    }

    private Callback<TableColumn<BizDictPO, Void>, TableCell<BizDictPO, Void>> createDependencyCellFactory() {
        return new Callback<TableColumn<BizDictPO, Void>, TableCell<BizDictPO, Void>>() {
            @Override
            public TableCell<BizDictPO, Void> call(TableColumn<BizDictPO, Void> param) {
                return new TableCell<BizDictPO, Void>() {
                    private final Button refBtn = new Button();
                    private final HBox hbox = new HBox(8, refBtn);
                    {
                        // 设置引用按钮样式
                        javafx.scene.shape.SVGPath refIcon = new javafx.scene.shape.SVGPath();
                        refIcon.setContent("M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14");
                        refIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                        refIcon.setScaleX(0.7);
                        refIcon.setScaleY(0.7);
                        refBtn.setGraphic(refIcon);
                        refBtn.setTooltip(new Tooltip("引用基础字典"));
                        refBtn.setMinWidth(28);
                        refBtn.setPrefWidth(28);
                        refBtn.setMaxWidth(28);
                        refBtn.setMinHeight(28);
                        refBtn.setPrefHeight(28);
                        refBtn.setMaxHeight(28);
                        refBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                        // 添加鼠标悬停效果
                        refBtn.setOnMouseEntered(e -> refBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        refBtn.setOnMouseExited(e -> refBtn.setStyle("-fx-background-color: transparent;"));
                        refBtn.setOnMousePressed(e -> refBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                        refBtn.setOnMouseReleased(e -> refBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        refBtn.setOnAction(e -> {
                            BizDictPO item = getTableView().getItems().get(getIndex());
                            showBaseDictReferenceDialog(item);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                        setStyle("-fx-alignment: center;");
                    }
                };
            }
        };
    }

    private Callback<TableColumn<BizDictPO, Void>, TableCell<BizDictPO, Void>> createActionCellFactory() {
        return new Callback<TableColumn<BizDictPO, Void>, TableCell<BizDictPO, Void>>() {
            @Override
            public TableCell<BizDictPO, Void> call(TableColumn<BizDictPO, Void> param) {
                return new TableCell<BizDictPO, Void>() {
                    private final Button saveBtn = new Button();
                    private final Button delBtn = new Button();
                    private final HBox hbox = new HBox(8, saveBtn, delBtn);
                    {
                        // 设置按钮容器居中对齐
                        hbox.setAlignment(javafx.geometry.Pos.CENTER);
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
                            BizDictPO item = getTableView().getItems().get(getIndex());
                            saveSingleRecord(item);
                        });
                        
                        // 设置删除按钮样式 - 使用与PaneTableMngController相同的SVG图标样式
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
                            BizDictPO item = getTableView().getItems().get(getIndex());
                            deleteBizDict(item);
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                        setStyle("-fx-alignment: center;");
                    }
                };
            }
        };
    }

    /**
     * 新增业务字典
     */
    private void addBizDict() {
        // 使用Stage而不是Dialog，这样可以更好地控制窗口行为
        Stage dialogStage = new Stage();
        dialogStage.setTitle("新增业务字典");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(450);
        dialogStage.setWidth(550);
        dialogStage.setHeight(500);
        
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
        TextField nameCamelField = new TextField();
        nameCamelField.setPromptText("业务字段名（驼峰命名）");
        nameCamelField.setMaxWidth(Double.MAX_VALUE);
        nameCamelField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        TextField nameSnakeField = new TextField();
        nameSnakeField.setPromptText("数据库字段名（下划线命名）");
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
        
        TextField commentCnField = new TextField();
        commentCnField.setPromptText("中文描述");
        commentCnField.setMaxWidth(Double.MAX_VALUE);
        commentCnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        TextField commentEnField = new TextField();
        commentEnField.setPromptText("英文描述");
        commentEnField.setMaxWidth(Double.MAX_VALUE);
        commentEnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        TextField defaultValueField = new TextField();
        defaultValueField.setPromptText("默认值");
        defaultValueField.setMaxWidth(Double.MAX_VALUE);
        defaultValueField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        
        // 添加到网格
        Label nameCamelLabel = new Label("业务字段名:");
        nameCamelLabel.setStyle(labelStyle);
        grid.add(nameCamelLabel, 0, 0);
        grid.add(nameCamelField, 1, 0);
        
        Label nameSnakeLabel = new Label("数据库字段名:");
        nameSnakeLabel.setStyle(labelStyle);
        grid.add(nameSnakeLabel, 0, 1);
        grid.add(nameSnakeField, 1, 1);
        
        Label typeLabel = new Label("字段类型:");
        typeLabel.setStyle(labelStyle);
        grid.add(typeLabel, 0, 2);
        grid.add(typeComboBox, 1, 2);
        
        Label lengthLabel = new Label("字段长度:");
        lengthLabel.setStyle(labelStyle);
        grid.add(lengthLabel, 0, 3);
        grid.add(lengthField, 1, 3);
        
        Label dbTypLabel = new Label("数据库类型:");
        dbTypLabel.setStyle(labelStyle);
        grid.add(dbTypLabel, 0, 4);
        grid.add(dbTypComboBox, 1, 4);
        
        Label commentCnLabel = new Label("中文描述:");
        commentCnLabel.setStyle(labelStyle);
        grid.add(commentCnLabel, 0, 5);
        grid.add(commentCnField, 1, 5);
        
        Label commentEnLabel = new Label("英文描述:");
        commentEnLabel.setStyle(labelStyle);
        grid.add(commentEnLabel, 0, 6);
        grid.add(commentEnField, 1, 6);
        
        Label defaultValueLabel = new Label("默认值:");
        defaultValueLabel.setStyle(labelStyle);
        grid.add(defaultValueLabel, 0, 7);
        grid.add(defaultValueField, 1, 7);
        
        // 添加提示标签
        Label tipLabel = new Label("");
        tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc3545; -fx-padding: 5 0;");
        tipLabel.setWrapText(true);
        tipLabel.setMaxWidth(Double.MAX_VALUE);
        grid.add(tipLabel, 1, 8); // 在第8行显示提示
        
        // 设置列的增长策略，让输入框列占满剩余空间
        GridPane.setHgrow(nameCamelField, Priority.ALWAYS);
        GridPane.setHgrow(nameSnakeField, Priority.ALWAYS);
        GridPane.setHgrow(typeComboBox, Priority.ALWAYS);
        GridPane.setHgrow(lengthField, Priority.ALWAYS);
        GridPane.setHgrow(dbTypComboBox, Priority.ALWAYS);
        GridPane.setHgrow(commentCnField, Priority.ALWAYS);
        GridPane.setHgrow(commentEnField, Priority.ALWAYS);
        GridPane.setHgrow(defaultValueField, Priority.ALWAYS);
        
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
            String nameCamel = nameCamelField.getText().trim();
            String nameSnake = nameSnakeField.getText().trim();
            String type = typeComboBox.getValue() != null ? typeComboBox.getValue().trim() : "";
            
            // 检查必填字段
            if (nameCamel.isEmpty()) {
                tipLabel.setText("业务字段名不能为空");
                return;
            }
            if (nameSnake.isEmpty()) {
                tipLabel.setText("数据库字段名不能为空");
                return;
            }
            if (type.isEmpty()) {
                tipLabel.setText("字段类型不能为空");
                return;
            }
            
            // 清除之前的错误提示
            tipLabel.setText("");
            
            // 创建新的业务字典对象
            BizDictPO newBizDict = new BizDictPO();
            newBizDict.setNameCamel(nameCamel);
            newBizDict.setNameSnake(nameSnake);
            newBizDict.setType(type);
            
            // 处理长度字段
            String lengthText = lengthField.getText().trim();
            if (!lengthText.isEmpty()) {
                try {
                    int length = Integer.parseInt(lengthText);
                    if (length <= 0) {
                        tipLabel.setText("字段长度必须大于0");
                        return;
                    }
                    newBizDict.setLength(length);
                } catch (NumberFormatException e) {
                    tipLabel.setText("字段长度必须是有效的数字");
                    return;
                }
            } else {
                newBizDict.setLength(null);
            }
            
            newBizDict.setDbTyp(dbTypComboBox.getValue() != null ? dbTypComboBox.getValue().trim() : "");
            newBizDict.setCommentCn(commentCnField.getText().trim());
            newBizDict.setCommentEn(commentEnField.getText().trim());
            newBizDict.setDefaultValue(defaultValueField.getText().trim());
            
            // 设置其他必要字段
            if (globalProperties != null) {
                newBizDict.setGroupName(globalProperties.getGroupName());
                newBizDict.setProjectName(globalProperties.getProjectName());
                newBizDict.setAppName(globalProperties.getAppName());
            }
            
            // 设置状态为待审核
            newBizDict.setStatus(DataStatusEnum.WAIT_AUDIT.getCode());
            
            try {
                // 保存到数据库
                bizDictRpcService.save(newBizDict);
                
                // 刷新列表
                doQuery();
                
                // 关闭对话框
                dialogStage.close();
                
                // 显示成功消息
                ViewUtils.alertForSucess("业务字典已成功添加");

            } catch (Exception e) {
                e.printStackTrace();
                tipLabel.setText("添加业务字典时发生错误: " + e.getMessage());
            }
        });
        
        // 设置取消按钮的点击事件
        cancelButton.setOnAction(event -> {
            dialogStage.close();
        });
        
        // 添加按钮区域
        HBox buttonBox = new HBox(10); // 10px间距
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        grid.add(buttonBox, 1, 9); // 在第9行显示按钮区域
        
        // 显示对话框
        dialogStage.showAndWait();
    }

    private void editBizDict(BizDictPO bizDict) {
        // 创建编辑对话框
        Dialog<BizDictPO> dialog = new Dialog<>();
        dialog.setTitle("编辑业务字典");
        dialog.setHeaderText("编辑业务字典: " + bizDict.getNameCamel());
        dialog.setResizable(true);
        dialog.getDialogPane().setMinWidth(500);
        dialog.getDialogPane().setMinHeight(450);
        dialog.getDialogPane().setPrefWidth(550);
        dialog.getDialogPane().setPrefHeight(500);
        
        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
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
        
        // 创建输入字段并设置当前值
        TextField nameCamelField = new TextField(bizDict.getNameCamel() != null ? bizDict.getNameCamel() : "");
        nameCamelField.setPromptText("业务字段名（驼峰命名）");
        nameCamelField.setMaxWidth(Double.MAX_VALUE);
        nameCamelField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField nameSnakeField = new TextField(bizDict.getNameSnake() != null ? bizDict.getNameSnake() : "");
        nameSnakeField.setPromptText("数据库字段名（下划线命名）");
        nameSnakeField.setMaxWidth(Double.MAX_VALUE);
        nameSnakeField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField typeField = new TextField(bizDict.getType() != null ? bizDict.getType() : "");
        typeField.setPromptText("字段类型（如：String、Integer等）");
        typeField.setMaxWidth(Double.MAX_VALUE);
        typeField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField lengthField = new TextField(bizDict.getLength() != null ? bizDict.getLength().toString() : "");
        lengthField.setPromptText("字段长度");
        lengthField.setMaxWidth(Double.MAX_VALUE);
        lengthField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField dbTypField = new TextField(bizDict.getDbTyp() != null ? bizDict.getDbTyp() : "");
        dbTypField.setPromptText("数据库类型（如：varchar、int等）");
        dbTypField.setMaxWidth(Double.MAX_VALUE);
        dbTypField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField commentCnField = new TextField(bizDict.getCommentCn() != null ? bizDict.getCommentCn() : "");
        commentCnField.setPromptText("中文描述");
        commentCnField.setMaxWidth(Double.MAX_VALUE);
        commentCnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField commentEnField = new TextField(bizDict.getCommentEn() != null ? bizDict.getCommentEn() : "");
        commentEnField.setPromptText("英文描述");
        commentEnField.setMaxWidth(Double.MAX_VALUE);
        commentEnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        TextField defaultValueField = new TextField(bizDict.getDefaultValue() != null ? bizDict.getDefaultValue() : "");
        defaultValueField.setPromptText("默认值");
        defaultValueField.setMaxWidth(Double.MAX_VALUE);
        defaultValueField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        
        // 创建状态选择下拉框
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(
            DataStatusEnum.WAIT_AUDIT.getDesc(),
            DataStatusEnum.NORMAL.getDesc(),
            DataStatusEnum.PENDING.getDesc(),
            DataStatusEnum.REVIEW.getDesc(),
            DataStatusEnum.COMPLETED.getDesc()
        );
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        statusComboBox.setMinHeight(28);
        statusComboBox.setPrefHeight(28);
        statusComboBox.setMaxHeight(28);
        statusComboBox.setStyle("-fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: transparent; -fx-border-width: 0; -fx-padding: 0; -fx-font-size: 13px;");
        
        // 设置当前选中的状态
        String currentStatus = bizDict.getStatus();
        if (currentStatus != null) {
            DataStatusEnum statusEnum = DataStatusEnum.getByCode(currentStatus);
            if (statusEnum != null) {
                statusComboBox.setValue(statusEnum.getDesc());
            } else {
                statusComboBox.setValue(DataStatusEnum.WAIT_AUDIT.getDesc());
            }
        } else {
            statusComboBox.setValue(DataStatusEnum.WAIT_AUDIT.getDesc());
        }
        
        // 确保状态下拉框有默认值
        if (statusComboBox.getValue() == null) {
            statusComboBox.setValue(DataStatusEnum.WAIT_AUDIT.getDesc());
        }
        
        // 创建枚举名称和引用字段
        TextField enumNameField = new TextField(bizDict.getEnumNme() != null ? bizDict.getEnumNme() : "");
        enumNameField.setPromptText("枚举名称");
        enumNameField.setMaxWidth(Double.MAX_VALUE);
        enumNameField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        enumNameField.setEditable(false); // 设置为只读
        
        TextField enumRefField = new TextField(bizDict.getEnumRef() != null ? bizDict.getEnumRef() : "");
        enumRefField.setPromptText("枚举引用路径");
        enumRefField.setMaxWidth(Double.MAX_VALUE);
        enumRefField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
        enumRefField.setEditable(false); // 设置为只读
        
        // 创建删除枚举按钮
        Button deleteEnumBtn = new Button("×");
        deleteEnumBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 4 8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-max-width: 24; -fx-max-height: 24;");
        deleteEnumBtn.setTooltip(new Tooltip("删除当前枚举关联"));
        
        // 创建枚举字段的水平布局容器
        HBox enumNameContainer = new HBox(8);
        enumNameContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        enumNameContainer.getChildren().addAll(enumNameField, deleteEnumBtn);
        HBox.setHgrow(enumNameField, Priority.ALWAYS);
        
        HBox enumRefContainer = new HBox(8);
        enumRefContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        enumRefContainer.getChildren().addAll(enumRefField);
        HBox.setHgrow(enumRefField, Priority.ALWAYS);
        
        // 只在有枚举值时才显示删除按钮
        if (bizDict.getEnumNme() == null || bizDict.getEnumNme().trim().isEmpty()) {
            deleteEnumBtn.setVisible(false);
        }
        
        // 添加到网格
        Label nameCamelLabel = new Label("业务字段名:");
        nameCamelLabel.setStyle(labelStyle);
        grid.add(nameCamelLabel, 0, 0);
        grid.add(nameCamelField, 1, 0);
        
        Label nameSnakeLabel = new Label("数据库字段名:");
        nameSnakeLabel.setStyle(labelStyle);
        grid.add(nameSnakeLabel, 0, 1);
        grid.add(nameSnakeField, 1, 1);
        
        Label typeLabel = new Label("字段类型:");
        typeLabel.setStyle(labelStyle);
        grid.add(typeLabel, 0, 2);
        grid.add(typeField, 1, 2);
        
        Label lengthLabel = new Label("字段长度:");
        lengthLabel.setStyle(labelStyle);
        grid.add(lengthLabel, 0, 3);
        grid.add(lengthField, 1, 3);
        
        Label dbTypLabel = new Label("数据库类型:");
        dbTypLabel.setStyle(labelStyle);
        grid.add(dbTypLabel, 0, 4);
        grid.add(dbTypField, 1, 4);
        
        Label commentCnLabel = new Label("中文描述:");
        commentCnLabel.setStyle(labelStyle);
        grid.add(commentCnLabel, 0, 5);
        grid.add(commentCnField, 1, 5);
        
        Label commentEnLabel = new Label("英文描述:");
        commentEnLabel.setStyle(labelStyle);
        grid.add(commentEnLabel, 0, 5);
        grid.add(commentEnField, 1, 5);
        
        Label defaultValueLabel = new Label("默认值:");
        defaultValueLabel.setStyle(labelStyle);
        grid.add(defaultValueLabel, 0, 6);
        grid.add(defaultValueField, 1, 6);
        
        Label statusLabel = new Label("状态:");
        statusLabel.setStyle(labelStyle);
        grid.add(statusLabel, 0, 7);
        grid.add(statusComboBox, 1, 7);
        
        Label enumNameLabel = new Label("枚举名称:");
        enumNameLabel.setStyle(labelStyle);
        grid.add(enumNameLabel, 0, 8);
        grid.add(enumNameContainer, 1, 8);
        
        Label enumRefLabel = new Label("枚举引用:");
        enumRefLabel.setStyle(labelStyle);
        grid.add(enumRefLabel, 0, 9);
        grid.add(enumRefContainer, 1, 9);
        
        // 为删除枚举按钮添加事件处理
        deleteEnumBtn.setOnAction(e -> {
            // 清空枚举相关字段
            enumNameField.clear();
            enumRefField.clear();
            
            // 隐藏删除按钮
            deleteEnumBtn.setVisible(false);
            
            // 显示提示信息
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("枚举删除");
            alert.setHeaderText(null);
            alert.setContentText("枚举关联已清除，保存后将生效。");
            alert.showAndWait();
        });
        
        // 确保删除按钮的可见性正确设置
        deleteEnumBtn.setVisible(bizDict.getEnumNme() != null && !bizDict.getEnumNme().trim().isEmpty());
        
        // 枚举引用字段始终隐藏
        enumRefLabel.setVisible(false);
        enumRefContainer.setVisible(false);
        
        // 只在有枚举名称时才显示删除按钮
        if (bizDict.getEnumNme() == null || bizDict.getEnumNme().trim().isEmpty()) {
            deleteEnumBtn.setVisible(false);
        }
        
        // 设置列的增长策略，让输入框列占满剩余空间
        GridPane.setHgrow(nameCamelField, Priority.ALWAYS);
        GridPane.setHgrow(nameSnakeField, Priority.ALWAYS);
        GridPane.setHgrow(typeField, Priority.ALWAYS);
        GridPane.setHgrow(lengthField, Priority.ALWAYS);
        GridPane.setHgrow(dbTypField, Priority.ALWAYS);
        GridPane.setHgrow(commentCnField, Priority.ALWAYS);
        GridPane.setHgrow(commentEnField, Priority.ALWAYS);
        GridPane.setHgrow(defaultValueField, Priority.ALWAYS);
        GridPane.setHgrow(statusComboBox, Priority.ALWAYS);
        GridPane.setHgrow(enumNameContainer, Priority.ALWAYS);
        GridPane.setHgrow(enumRefContainer, Priority.ALWAYS);
        
        dialog.getDialogPane().setContent(grid);
        
        // 设置保存按钮的默认状态
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDefaultButton(true);
        
        // 为输入字段添加验证监听器
        nameCamelField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateSaveButton(saveButton, nameCamelField, nameSnakeField, typeField);
        });
        nameSnakeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateSaveButton(saveButton, nameCamelField, nameSnakeField, typeField);
        });
        typeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateSaveButton(saveButton, nameCamelField, nameSnakeField, typeField);
        });
        
        // 初始化保存按钮状态
        validateSaveButton(saveButton, nameCamelField, nameSnakeField, typeField);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 验证必填字段
                String nameCamel = nameCamelField.getText().trim();
                String nameSnake = nameSnakeField.getText().trim();
                String type = typeField.getText().trim();
                
                if (nameCamel.isEmpty()) {
                    showValidationError("业务字段名不能为空");
                    return null;
                }
                if (nameSnake.isEmpty()) {
                    showValidationError("数据库字段名不能为空");
                    return null;
                }
                if (type.isEmpty()) {
                    showValidationError("字段类型不能为空");
                    return null;
                }
                
                // 更新业务字典对象
                bizDict.setNameCamel(nameCamel);
                bizDict.setNameSnake(nameSnake);
                bizDict.setType(type);
                
                // 处理长度字段
                String lengthText = lengthField.getText().trim();
                if (!lengthText.isEmpty()) {
                    try {
                        int length = Integer.parseInt(lengthText);
                        if (length <= 0) {
                            showValidationError("字段长度必须大于0");
                            return null;
                        }
                        bizDict.setLength(length);
                    } catch (NumberFormatException e) {
                        showValidationError("字段长度必须是有效的数字");
                        return null;
                    }
                } else {
                    bizDict.setLength(null);
                }
                
                bizDict.setCommentCn(commentCnField.getText().trim());
                bizDict.setCommentEn(commentEnField.getText().trim());
                bizDict.setDefaultValue(defaultValueField.getText().trim());
                bizDict.setDbTyp(dbTypField.getText().trim());
                bizDict.setEnumNme(enumNameField.getText().trim());
                bizDict.setEnumRef(enumRefField.getText().trim());
                
                // 根据选中的状态描述设置状态代码
                String selectedStatusDesc = statusComboBox.getValue();
                if (selectedStatusDesc != null) {
                    for (DataStatusEnum statusEnum : DataStatusEnum.values()) {
                        if (statusEnum.getDesc().equals(selectedStatusDesc)) {
                            bizDict.setStatus(statusEnum.getCode());
                            break;
                        }
                    }
                }
                
                // 确保必要的字段有值
                if (bizDict.getGroupName() == null && globalProperties != null) {
                    bizDict.setGroupName(globalProperties.getGroupName());
                }
                if (bizDict.getProjectName() == null && globalProperties != null) {
                    bizDict.setProjectName(globalProperties.getProjectName());
                }
                if (bizDict.getAppName() == null && globalProperties != null) {
                    bizDict.setAppName(globalProperties.getAppName());
                }
                
                return bizDict;
            }
            return null;
        });
        
        // 显示对话框并处理结果
        Optional<BizDictPO> result = dialog.showAndWait();
        result.ifPresent(updatedBizDict -> {
            try {
                // 创建更新条件对象
                BizDictPO wherePO = new BizDictPO();
                wherePO.setGroupName(updatedBizDict.getGroupName());
                wherePO.setProjectName(updatedBizDict.getProjectName());
                wherePO.setNameCamel(updatedBizDict.getNameCamel());
                wherePO.setAppName(updatedBizDict.getAppName());
                
                // 验证必要字段
                if (wherePO.getGroupName() == null || wherePO.getProjectName() == null || 
                    wherePO.getNameCamel() == null || wherePO.getAppName() == null) {
                    throw new RuntimeException("缺少必要的更新条件字段");
                }
                
                // 保存到数据库
                bizDictRpcService.updateByOne(updatedBizDict, wherePO);
                
                // 刷新列表
                doQuery();
                
                // 显示成功消息
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("编辑成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText("业务字典已成功更新");
                successAlert.showAndWait();
                
            } catch (Exception e) {
                e.printStackTrace();
                // 显示错误消息
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("编辑失败");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("更新业务字典时发生错误: " + e.getMessage());
                errorAlert.showAndWait();
            }
        });
    }



    private void deleteBizDict(BizDictPO bizDict) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除业务字典 '" + bizDict.getNameCamel() + "' 吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: 实现删除功能
                dictList.remove(bizDict);
                bizDictTableView.refresh();
                
                // 更新总数统计
                if (totalCountLabel != null) {
                    totalCountLabel.setText("总数: " + dictList.size());
                }
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("删除成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText("业务字典已删除");
                successAlert.showAndWait();
            }
        });
    }

    /**
     * 为所有列设置单元格工厂（确保显示正确）
     */
    private void setupColumnCellFactories() {
        // 为每个列设置单元格工厂，确保数据显示正确
        setupColumnCellFactory(appNameColumn);
        setupColumnCellFactory(nameCamelColumn);
        setupColumnCellFactory(nameSnakeColumn);
        setupColumnCellFactory(typeColumn);
        setupColumnCellFactory(statusColumn);
        // enumRefColumn已经在initialize中设置了setCellValueFactory，不需要额外的单元格工厂
        setupColumnCellFactory(defaultValueColumn);
        setupColumnCellFactory(commentCnColumn);
        setupColumnCellFactory(commentEnColumn);
        setupColumnCellFactory(lengthColumn);
        setupColumnCellFactory(dbTypColumn);
    }
    
    /**
     * 为单个列设置单元格工厂
     */
    private <T> void setupColumnCellFactory(TableColumn<BizDictPO, T> column) {
        column.setCellFactory(col -> new TableCell<BizDictPO, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
                
                // 为业务字段列和描述列设置左对齐并添加左边距
                if (column == nameCamelColumn || column == nameSnakeColumn || 
                    column == commentCnColumn || column == commentEnColumn) {
                    setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
                } else {
                    setStyle("-fx-alignment: center;");
                }
            }
        });
    }
    
    /**
     * 为引用列设置专门的单元格工厂
     */
    private void setupEnumRefColumnCellFactory() {
        enumRefColumn.setCellFactory(col -> new TableCell<BizDictPO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    BizDictPO bizDict = getTableView().getItems().get(getIndex());
                    if (bizDict != null) {
                        // 添加调试日志
                        System.out.println("引用列调试 - 行索引: " + getIndex() + 
                                        ", enumNme: '" + bizDict.getEnumNme() + "'" +
                                        ", baseRef: '" + bizDict.getBaseRef() + "'");
                        
                        String displayValue;
                        if (bizDict.getEnumNme() != null && !bizDict.getEnumNme().trim().isEmpty()) {
                            // 如果enumNme有值，显示enumNme
                            displayValue = bizDict.getEnumNme();
                            System.out.println("显示enumNme值: " + displayValue);
                        } else if (bizDict.getBaseRef() != null && !bizDict.getBaseRef().trim().isEmpty()) {
                            // 如果enumNme为空但baseRef有值，显示baseRef
                            displayValue = bizDict.getBaseRef();
                            System.out.println("显示baseRef值: " + displayValue);
                        } else {
                            // 如果两者都为空，显示空字符串
                            displayValue = "";
                        }
                        setText(displayValue);
                    } else {
                        setText("");
                        System.out.println("bizDict为null");
                    }
                }
                setStyle("-fx-alignment: center;");
            }
        });
    }
    


    /**
     * 初始化微服务下拉框
     */
    private void initializeAppNameComboBox() {
        if (appNameComboBox != null) {
            // 添加选项：全部和当前应用名称
            ObservableList<String> appNames = FXCollections.observableArrayList();
            appNames.add("全部");
            if (globalProperties != null && globalProperties.getAppName() != null) {
                appNames.add(globalProperties.getAppName());
            }
            
            appNameComboBox.setItems(appNames);
            appNameComboBox.setValue(globalProperties.getAppName()); // 默认选择全部
            
            // 添加选择变化监听器（延迟设置，避免初始化时触发查询）
            javafx.application.Platform.runLater(() -> {
                appNameComboBox.setOnAction(event -> doQuery());
            });
        }
    }
    
    private void doQuery() {
        String searchText = searchField != null ? searchField.getText() : "";
        String selectedAppName = appNameComboBox != null ? appNameComboBox.getValue() : "全部";
        
        // 如果选择了特定应用，需要过滤结果
        List<BizDictPO> list = bizDictRpcService.searchByName(searchText,selectedAppName);
        
        // 添加调试日志，检查数据
        System.out.println("查询结果数量: " + list.size());
        for (int i = 0; i < Math.min(list.size(), 5); i++) { // 只显示前5条记录
            BizDictPO item = list.get(i);
            System.out.println("记录 " + i + ": enumNme='" + item.getEnumNme() + "', baseRef='" + item.getBaseRef() + "'");
        }
        
        dictList.setAll(list);
        bizDictTableView.refresh();
        
        // 更新总数统计
        if (totalCountLabel != null) {
            totalCountLabel.setText("总数: " + list.size());
        }
    }
    
    private void showEnumDetails(BizDictPO bizDict) {
        try {
            // 查询枚举详情
            EnumDictPO query = new EnumDictPO();
            query.setEnumNme(bizDict.getEnumNme());
            query.setEnumRef(bizDict.getEnumRef());
            List<EnumDictPO> enumList = enumDictRpcService.queryForList(query);
            
            if (enumList.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("枚举详情");
                alert.setHeaderText(null);
                alert.setContentText("未找到枚举 '" + bizDict.getEnumNme() + "' 的详细信息");
                alert.showAndWait();
                return;
            }
            
            // 创建详情对话框
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("枚举详情 - " + bizDict.getEnumNme());
            dialog.setHeaderText("枚举路径: " + bizDict.getEnumRef());
            dialog.setResizable(true);
            
            // 设置对话框按钮
            ButtonType closeButton = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            // 创建表格显示枚举值
            TableView<EnumDictPO> enumTableView = new TableView<>();
            
            // 创建列
            TableColumn<EnumDictPO, String> enumValColumn = new TableColumn<>("枚举代码");
            TableColumn<EnumDictPO, String> descCnColumn = new TableColumn<>("中文描述");
            TableColumn<EnumDictPO, String> descEnColumn = new TableColumn<>("英文描述");
            
            // 设置列宽
            enumValColumn.setPrefWidth(60);
            descCnColumn.setPrefWidth(100);
            descEnColumn.setPrefWidth(100);
            
            // 设置数据绑定
            enumValColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEnumVal()));
            descCnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescCn()));
            descEnColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescEn()));
            
            // 设置列居中对齐
            enumValColumn.setStyle("-fx-alignment: center;");
            descCnColumn.setStyle("-fx-alignment: center;");
            descEnColumn.setStyle("-fx-alignment: center;");
            
            // 添加列到表格
            enumTableView.getColumns().addAll(enumValColumn, descCnColumn, descEnColumn);
            
            // 设置数据
            enumTableView.setItems(FXCollections.observableArrayList(enumList));
            
            // 设置对话框内容
            dialog.getDialogPane().setContent(enumTableView);
            
            // 设置对话框大小
            dialog.getDialogPane().setPrefSize(300, 300);
            
            // 显示对话框
            dialog.showAndWait();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("加载枚举详情时发生错误: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * 显示枚举选择对话框
     */
    private void showEnumSelectionDialog(BizDictPO selectedItem) {
        try {
            // 使用新的枚举编辑对话框组件
            Optional<EnumSelectDialog.EnumEditResult> result = enumSelectDialog.showAndWait(selectedItem);
            
            if (result.isPresent()) {
                EnumSelectDialog.EnumEditResult editResult = result.get();
                
                // 更新业务字典对象
                selectedItem.setEnumNme(editResult.getEnumName());
                selectedItem.setEnumRef(editResult.getEnumRef());
                
                // 保存到数据库
                try {
                    bizDictRpcService.updateEnumRef(selectedItem);
                    bizDictTableView.refresh();
                } catch (Exception e) {
                    ViewUtils.alertForFail("保存枚举值时发生错误: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("创建枚举选择对话框时发生错误: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * 验证保存按钮状态
     */
    private void validateSaveButton(Button saveButton, TextField nameCamelField, TextField nameSnakeField, TextField typeField) {
        boolean isValid = !nameCamelField.getText().trim().isEmpty() && 
                         !nameSnakeField.getText().trim().isEmpty() && 
                         !typeField.getText().trim().isEmpty();
        saveButton.setDisable(!isValid);
    }
    
    /**
     * 显示验证错误消息
     */
    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("验证失败");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示基础字典引用对话框
     */
    private void showBaseDictReferenceDialog(BizDictPO bizDict) {
        try {
            // 创建基础字典查询对话框
            Dialog<BaseDictPO> dialog = new Dialog<>();
            dialog.setTitle("引用基础字典");
            dialog.setHeaderText("为业务字典 '" + bizDict.getNameCamel() + "' 选择基础字典");
            dialog.setResizable(true);
            dialog.getDialogPane().setMinWidth(800);
            dialog.getDialogPane().setMinHeight(600);
            dialog.getDialogPane().setPrefWidth(900);
            dialog.getDialogPane().setPrefHeight(700);
            
            // 设置对话框按钮
            ButtonType selectButtonType = new ButtonType("选择", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, cancelButtonType);
            
            // 创建主容器
            VBox mainContainer = new VBox(15);
            mainContainer.setStyle("-fx-padding: 20;");
            
            // 创建搜索区域
            HBox searchBox = new HBox(10);
            searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label searchLabel = new Label("搜索基础字典:");
            searchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            TextField searchField = new TextField();
            searchField.setPromptText("输入字段名、类型或注释进行搜索");
            searchField.setPrefWidth(300);
            searchField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10;");
            
            // 创建基础字典表格
            TableView<BaseDictPO> baseDictTableView = new TableView<>();
            
            Button searchButton = new Button("搜索");
            searchButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 16;");
            searchButton.setOnAction(e -> performBaseDictSearch(searchField.getText(), baseDictTableView));
            
            searchBox.getChildren().addAll(searchLabel, searchField, searchButton);
            
            // 创建列
            TableColumn<BaseDictPO, String> nameSnakeColumn = new TableColumn<>("字段名");
            TableColumn<BaseDictPO, String> typeColumn = new TableColumn<>("Java类型");
            TableColumn<BaseDictPO, Number> lengthColumn = new TableColumn<>("长度");
            TableColumn<BaseDictPO, String> dbTypColumn = new TableColumn<>("数据库类型");
            TableColumn<BaseDictPO, String> defaultValueColumn = new TableColumn<>("默认值");
            TableColumn<BaseDictPO, String> commentCnColumn = new TableColumn<>("中文注释");
            TableColumn<BaseDictPO, String> statusColumn = new TableColumn<>("状态");
            
            // 设置列宽
            nameSnakeColumn.setPrefWidth(120);
            typeColumn.setPrefWidth(100);
            lengthColumn.setPrefWidth(60);
            dbTypColumn.setPrefWidth(100);
            defaultValueColumn.setPrefWidth(100);
            commentCnColumn.setPrefWidth(150);
            statusColumn.setPrefWidth(80);
            
            // 设置数据绑定
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
            
            // 设置列样式
            nameSnakeColumn.setStyle("-fx-alignment: center;");
            typeColumn.setStyle("-fx-alignment: center;");
            lengthColumn.setStyle("-fx-alignment: center;");
            dbTypColumn.setStyle("-fx-alignment: center;");
            defaultValueColumn.setStyle("-fx-alignment: center;");
            commentCnColumn.setStyle("-fx-alignment: center-left; -fx-padding: 0 0 0 5;");
            statusColumn.setStyle("-fx-alignment: center;");
            
            // 添加列到表格
            baseDictTableView.getColumns().addAll(nameSnakeColumn, typeColumn, lengthColumn, dbTypColumn, defaultValueColumn, commentCnColumn, statusColumn);
            
            // 设置表格选择模式
            baseDictTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            
            // 为表格添加双击事件
            baseDictTableView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    BaseDictPO selectedBaseDict = baseDictTableView.getSelectionModel().getSelectedItem();
                    if (selectedBaseDict != null) {
                        // 双击选择基础字典
                        dialog.setResult(selectedBaseDict);
                    }
                }
            });
            
            // 添加组件到主容器
            mainContainer.getChildren().addAll(searchBox, baseDictTableView);
            
            // 设置对话框内容
            dialog.getDialogPane().setContent(mainContainer);
            
            // 设置结果转换器
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == selectButtonType) {
                    return baseDictTableView.getSelectionModel().getSelectedItem();
                }
                return null;
            });
            
            // 初始加载数据
            performBaseDictSearch("", baseDictTableView);
            
            // 显示对话框并处理结果
            Optional<BaseDictPO> result = dialog.showAndWait();
            result.ifPresent(selectedBaseDict -> {
                try {
                    // 更新业务字典的基础字典引用
                    bizDict.setType(selectedBaseDict.getType());
                    bizDict.setLength(selectedBaseDict.getLength());
                    bizDict.setDbTyp(selectedBaseDict.getDbTyp());
                    bizDict.setDefaultValue(selectedBaseDict.getDefaultValue());
                    bizDict.setBaseRef(selectedBaseDict.getNameSnake());
                    bizDict.setEnumNme("");
                    bizDict.setEnumRef("");
                    
                    // 保存到数据库
                    bizDictRpcService.updateByOne(bizDict, createWhereCondition(bizDict));
                    
                    // 刷新表格
                    bizDictTableView.refresh();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    ViewUtils.alertForFail("引用基础字典时发生错误: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("创建基础字典引用对话框时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行基础字典搜索
     */
    private void performBaseDictSearch(String searchText, TableView<BaseDictPO> tableView) {
        try {
            // 使用BaseDictDao进行搜索
            List<BaseDictPO> baseDictList = baseDictRpcService.searchByName(searchText);
            
            // 更新表格数据
            ObservableList<BaseDictPO> observableList = FXCollections.observableArrayList(baseDictList);
            tableView.setItems(observableList);
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("搜索错误");
            alert.setHeaderText(null);
            alert.setContentText("搜索基础字典时发生错误: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * 创建更新条件
     */
    private BizDictPO createWhereCondition(BizDictPO bizDict) {
        BizDictPO wherePO = new BizDictPO();
        wherePO.setGroupName(bizDict.getGroupName());
        wherePO.setProjectName(bizDict.getProjectName());
        wherePO.setNameCamel(bizDict.getNameCamel());
        wherePO.setAppName(bizDict.getAppName());
        return wherePO;
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
            
            List<BizDictPO> importedData = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;
            
            // 从第二行开始读取数据（第一行是标题）
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                try {
                    BizDictPO bizDict = parseRowToBizDict(row);
                    if (bizDict != null) {
                        importedData.add(bizDict);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("解析第" + (rowIndex + 1) + "行数据时出错: " + e.getMessage());
                }
            }
            
            // 批量保存到数据库
            if (!importedData.isEmpty()) {
                bizDictRpcService.upsertAll(importedData);
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
     * 解析行数据为BizDictPO对象
     */
    private BizDictPO parseRowToBizDict(org.apache.poi.ss.usermodel.Row row) {
        BizDictPO bizDict = new BizDictPO();
        
        try {
            // 设置必要的字段
            if (globalProperties != null) {
                bizDict.setGroupName(globalProperties.getGroupName());
                bizDict.setProjectName(globalProperties.getProjectName());
                bizDict.setAppName(globalProperties.getAppName());
            }
            
            // 设置状态为待审核
            bizDict.setStatus(DataStatusEnum.WAIT_AUDIT.getCode());
            
            // 读取各列数据（按照新的列顺序）
            bizDict.setAppName(getCellValueAsString(row.getCell(0))); // 归属系统
            bizDict.setNameCamel(getCellValueAsString(row.getCell(1))); // 业务字段名
            bizDict.setNameSnake(getCellValueAsString(row.getCell(2))); // 数据库字段名
            bizDict.setType(getCellValueAsString(row.getCell(3))); // 字段类型
            bizDict.setDbTyp(getCellValueAsString(row.getCell(4))); // 数据库类型
            bizDict.setLength(getCellValueAsInteger(row.getCell(5))); // 长度
            bizDict.setEnumNme(getCellValueAsString(row.getCell(6))); // 枚举名称
            bizDict.setEnumRef(getCellValueAsString(row.getCell(7))); // 枚举引用
            bizDict.setBaseRef(getCellValueAsString(row.getCell(8))); // 基础字典
            bizDict.setCommentCn(getCellValueAsString(row.getCell(9))); // 中文描述
            bizDict.setCommentEn(getCellValueAsString(row.getCell(10))); // 英文描述
            bizDict.setDefaultValue(getCellValueAsString(row.getCell(11))); // 默认值
            // 状态列（第12列）在导入时不需要读取，会设置为默认的待审核状态
            
            // 验证必填字段
            if (bizDict.getNameCamel() == null || bizDict.getNameCamel().trim().isEmpty() ||
                bizDict.getNameSnake() == null || bizDict.getNameSnake().trim().isEmpty() ||
                bizDict.getType() == null || bizDict.getType().trim().isEmpty()) {
                return null; // 跳过无效数据
            }
            
            return bizDict;
            
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
     * 导出Excel文件
     */
    private void exportToExcel() {
        try {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择导出Excel文件保存位置");
            fileChooser.setInitialFileName("业务字典_" + System.currentTimeMillis() + ".xlsx");
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
                Task<Void> exportTask = new Task<Void>() {
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
                    PauseTransition delay =
                        new PauseTransition(Duration.millis(500));
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
                    PauseTransition delay =
                        new PauseTransition(Duration.millis(500));
                    delay.setOnFinished(e -> {
                        // 最后检查一次，确保进度对话框已关闭
                        if (progressDialog.isShowing()) {
                            progressDialog.forceCloseImmediate();
                        }
                        // 使用Platform.runLater确保在正确的时机显示提示
                        Platform.runLater(() -> {
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
            Sheet sheet = workbook.createSheet("业务字典");
            
            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建标题行
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                "归属系统", "业务字段名", "数据库字段名", "字段类型", "数据库类型", "长度", 
                "枚举名称", "枚举引用", "基础字典", "中文描述", "英文描述", "默认值", "状态"
            };
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                
                // 根据列内容调整列宽
                if (i == 0) { // 归属系统
                    sheet.setColumnWidth(i, 15 * 256);
                } else if (i == 1 || i == 2) { // 业务字段名、数据库字段名
                    sheet.setColumnWidth(i, 20 * 256);
                } else if (i == 3 || i == 4) { // 字段类型、数据库类型
                    sheet.setColumnWidth(i, 15 * 256);
                } else if (i == 5) { // 长度
                    sheet.setColumnWidth(i, 10 * 256);
                } else if (i == 6) { // 枚举名称
                    sheet.setColumnWidth(i, 20 * 256);
                } else if (i == 7 || i == 8) { // 枚举引用、基础字典
                    sheet.setColumnWidth(i, 30 * 256);
                } else if (i == 9 || i == 10) { // 中文描述、英文描述
                    sheet.setColumnWidth(i, 25 * 256);
                } else if (i == 11) { // 默认值
                    sheet.setColumnWidth(i, 15 * 256);
                } else if (i == 12) { // 状态
                    sheet.setColumnWidth(i, 12 * 256);
                } else {
                    sheet.setColumnWidth(i, 15 * 256);
                }
            }
            
            // 导出当前数据
            List<BizDictPO> dataToExport = new ArrayList<>(dictList);
            
            // 如果没有数据，尝试从数据库查询
            if (dataToExport.isEmpty()) {
                dataToExport = bizDictRpcService.searchByName("", "全部");
            }
            
            // 填充数据行
            int rowIndex = 1;
            for (BizDictPO bizDict : dataToExport) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(rowIndex++);
                
                // 归属系统列
                dataRow.createCell(0).setCellValue(bizDict.getAppName() != null ? bizDict.getAppName() : "");
                
                // 业务字段名列
                dataRow.createCell(1).setCellValue(bizDict.getNameCamel() != null ? bizDict.getNameCamel() : "");
                
                // 数据库字段名列
                dataRow.createCell(2).setCellValue(bizDict.getNameSnake() != null ? bizDict.getNameSnake() : "");
                
                // 字段类型列
                dataRow.createCell(3).setCellValue(bizDict.getType() != null ? bizDict.getType() : "");
                
                // 数据库类型列
                dataRow.createCell(4).setCellValue(bizDict.getDbTyp() != null ? bizDict.getDbTyp() : "");
                
                // 长度列
                dataRow.createCell(5).setCellValue(bizDict.getLength() != null ? bizDict.getLength() : 0);
                
                // 枚举名称列
                dataRow.createCell(6).setCellValue(bizDict.getEnumNme() != null ? bizDict.getEnumNme() : "");
                
                // 枚举引用列
                dataRow.createCell(7).setCellValue(bizDict.getEnumRef() != null ? bizDict.getEnumRef() : "");
                
                // 基础字典列
                dataRow.createCell(8).setCellValue(bizDict.getBaseRef() != null ? bizDict.getBaseRef() : "");
                
                // 中文描述列
                dataRow.createCell(9).setCellValue(bizDict.getCommentCn() != null ? bizDict.getCommentCn() : "");
                
                // 英文描述列
                dataRow.createCell(10).setCellValue(bizDict.getCommentEn() != null ? bizDict.getCommentEn() : "");
                
                // 默认值列
                dataRow.createCell(11).setCellValue(bizDict.getDefaultValue() != null ? bizDict.getDefaultValue() : "");
                
                // 状态列
                String statusDesc = "";
                if (bizDict.getStatus() != null) {
                    DataStatusEnum statusEnum = DataStatusEnum.getByCode(bizDict.getStatus());
                    statusDesc = statusEnum != null ? statusEnum.getDesc() : bizDict.getStatus();
                }
                dataRow.createCell(12).setCellValue(statusDesc);
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
     * 设置可编辑列的单元格工厂和编辑事件
     */
    private void setupEditableColumns() {
        StringConverter<String> converter = new DefaultStringConverter();
        
        // 业务字段名列 - 可编辑
        nameCamelColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        nameCamelColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setNameCamel(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 数据库字段名列 - 可编辑
        nameSnakeColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        nameSnakeColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setNameSnake(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 字段类型列 - 可编辑，支持下拉选择
        ObservableList<String> typeOptions = FXCollections.observableArrayList(JavaTypeEnum.getAllTypeNames());
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(typeOptions));
        typeColumn.setEditable(true);
        typeColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setType(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 长度列 - 可编辑
        lengthColumn.setCellFactory(col -> new TableCell<BizDictPO, Number>() {
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
                BizDictPO item = getTableView().getItems().get(getIndex());
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
            BizDictPO item = event.getRowValue();
            item.setDbTyp(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 默认值列 - 可编辑
        defaultValueColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        defaultValueColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setDefaultValue(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 中文描述列 - 可编辑
        commentCnColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        commentCnColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setCommentCn(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 英文描述列 - 可编辑
        commentEnColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        commentEnColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
            item.setCommentEn(event.getNewValue());
            // 标记数据已修改
            markDataModified();
        });
        
        // 状态列 - 使用下拉选择
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            DataStatusEnum.WAIT_AUDIT.getDesc(),
            DataStatusEnum.NORMAL.getDesc(),
            DataStatusEnum.PENDING.getDesc(),
            DataStatusEnum.REVIEW.getDesc(),
            DataStatusEnum.COMPLETED.getDesc()
        );
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(statusOptions));
        statusColumn.setEditable(true);
        statusColumn.setOnEditCommit(event -> {
            BizDictPO item = event.getRowValue();
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
     * 保存单条记录
     */
    private void saveSingleRecord(BizDictPO bizDict) {
        try {
            bizDictRpcService.upsert(bizDict);
            ViewUtils.alertForSucess("记录保存成功！");
            
            // 刷新列表
            doQuery();
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("保存记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 保存表格中的更改
     */
    private void saveChanges() {
        try {
            BatchWriteResult writeResult = bizDictRpcService.upsertAll(new ArrayList<>(dictList));
            int successCount = writeResult.getSuccessCount();
            int errorCount = writeResult.getErrorCount();
            
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