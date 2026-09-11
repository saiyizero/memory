package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.factory.TableEntityFactory;
import com.murong.ecp.tools.fx.domain.service.database.DataBaseHandler;
import com.murong.ecp.tools.fx.domain.service.database.TableEntityService;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.TableDataRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.DbConnectionRpcService;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.ImpddlDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.SqlDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.ToggleSwitch;
import com.murong.ecp.tools.fx.infrastructure.view.SelectDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.DeleteDialogUtil;
import com.murong.ecp.tools.fx.domain.service.different.TableDiffService;
import com.murong.ecp.tools.fx.domain.service.database.DataMigrationService;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.util.Callback;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class PaneTableMngController {

    @Autowired
    GlobalProperties globalProps;
    @Autowired
    private TableDataRpcService tableDataRpcService;
    @Autowired
    private TableEntityService tableEntityService;
    @Autowired
    private TableEntityFactory tableEntityFactory;
    @Autowired
    private TableDiffService tableDiffService;
    @Autowired
    private DataMigrationService dataMigrationService;
    @Autowired
    private DbConnectionRpcService dbConnectionRpcService;


    @FXML
    private TableView<TableDataPO> tableTableView;
    @FXML
    private TableColumn<TableDataPO, Boolean> selectColumn;
    @FXML
    private TableColumn<TableDataPO, String> tableSnakeColumn;
    @FXML
    private TableColumn<TableDataPO, String> tableNameColumn;
    @FXML
    private TableColumn<TableDataPO, String> labeNameColumn;
    @FXML
    private TableColumn<TableDataPO, String> tabCommentZhColumn;
    @FXML
    private TableColumn<TableDataPO, String> tabCommentEnColumn;
    @FXML
    private TableColumn<TableDataPO, String> parentClassPackageColumn;
    @FXML
    private TableColumn<TableDataPO, String> parentClassNameColumn;
    @FXML
    private TableColumn<TableDataPO, String> generCdFlgColumn;
    @FXML
    private TableColumn<TableDataPO, String> createTabFlgColumn;
    @FXML
    private TableColumn<TableDataPO, String> statusColumn;
    @FXML
    private TableColumn<TableDataPO, Void> actionColumn;

    private ObservableList<TableDataPO> transactionList = FXCollections.observableArrayList();
    // 用于存储每行的选中状态
    private ObservableList<SimpleBooleanProperty> selectedList = FXCollections.observableArrayList();
    @FXML
    private CheckBox selectAllCheckBox;
    @FXML
    private ComboBox<String> labenameComboBox;
    @FXML
    private Button syncTabButton;
    @FXML
    private Button importDdlButton;
    @FXML
    private Button generateDdlButton;
    @FXML
    private Button compareButton;
    @FXML
    private Button dataMigrationButton;
    @FXML
    private Button generateExcelButton;
    @FXML
    private TextField nameFilterField; // 名称输入框
    @FXML
    private Button queryButton;        // 查询按钮

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        TableDataPO reqPO = new TableDataPO();
        reqPO.setAppName(globalProps.getAppName());
        reqPO.setProjectName(globalProps.getProjectName());
        reqPO.setGroupName(globalProps.getGroupName());
        List<TableDataPO> list = tableDataRpcService.queryForList(reqPO);
        transactionList.setAll(list);
        tableTableView.setItems(transactionList);
        tableTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        nameFilterField.clear();
        // 初始化每行的选中状态
        selectedList.clear();
        for (int i = 0; i < transactionList.size(); i++) {
            selectedList.add(new SimpleBooleanProperty(false));
        }

        // 全选/全不选功能
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (SimpleBooleanProperty prop : selectedList) {
                prop.set(newVal);
            }
            tableTableView.refresh();
        });

        // 复选框列绑定
        selectColumn.setCellValueFactory(param -> {
            int index = transactionList.indexOf(param.getValue());
            if (index >= 0 && index < selectedList.size()) {
                return selectedList.get(index);
            } else {
                return new SimpleBooleanProperty(false);
            }
        });
        selectColumn.setCellFactory(col -> new TableCell<TableDataPO, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0 && index < selectedList.size()) {
                        selectedList.get(index).set(checkBox.isSelected());
                    }
                    // 更新全选框状态
                    updateSelectAllCheckBox();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= selectedList.size()) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(selectedList.get(getIndex()).get());
                    setGraphic(checkBox);
                }
            }
        });

        // 设置可复制的文本列
        setCopyableCellFactory(tableNameColumn, TableDataPO::getTableNameCamel);
        setCopyableCellFactory(labeNameColumn, TableDataPO::getLableName);
        setCopyableCellFactory(tabCommentZhColumn, TableDataPO::getTableCommentCn);
        setCopyableCellFactory(tabCommentEnColumn, TableDataPO::getTableCommentEn);
        setCopyableCellFactoryWithSchema(tableSnakeColumn, TableDataPO::getTableNameSnake);
        
        // 设置新列的可复制功能
        setCopyableCellFactory(parentClassPackageColumn, po -> {
            String parentClass = po.getParentClass();
            if (parentClass == null || parentClass.isEmpty()) return "";
            int lastDotIndex = parentClass.lastIndexOf('.');
            return lastDotIndex > 0 ? parentClass.substring(0, lastDotIndex) : "";
        });
        setCopyableCellFactory(parentClassNameColumn, po -> {
            String parentClass = po.getParentClass();
            if (parentClass == null || parentClass.isEmpty()) return "";
            int lastDotIndex = parentClass.lastIndexOf('.');
            return lastDotIndex > 0 ? parentClass.substring(lastDotIndex + 1) : parentClass;
        }, true);
        
        // 设置生成代码列为ToggleSwitch
        generCdFlgColumn.setCellFactory(createToggleSwitchCellFactory(TableDataPO::getGenerCdFlg, (po, value) -> {
            po.setGenerCdFlg(value ? "Y" : "N");
            // 这里可以添加保存到数据库的逻辑
            TableDataPO tableDataPO = new TableDataPO();
            tableDataPO.setGenerCdFlg(po.getGenerCdFlg());
            TableDataPO wherePO = new TableDataPO();
            wherePO.setTableNameCamel(po.getTableNameCamel());
            wherePO.setGroupName(po.getGroupName());
            wherePO.setProjectName(po.getProjectName());
            tableDataRpcService.updateByOne(tableDataPO, wherePO);
        }));
        
        // 设置修改DB列为ToggleSwitch
        createTabFlgColumn.setCellFactory(createToggleSwitchCellFactory(TableDataPO::getCreateTabFlg, (po, value) -> {
            po.setCreateTabFlg(value ? "Y" : "N");
            // 这里可以添加保存到数据库的逻辑
            TableDataPO tableDataPO = new TableDataPO();
            tableDataPO.setCreateTabFlg(po.getCreateTabFlg());
            TableDataPO wherePO = new TableDataPO();
            wherePO.setTableNameCamel(po.getTableNameCamel());
            wherePO.setGroupName(po.getGroupName());
            wherePO.setProjectName(po.getProjectName());
            tableDataRpcService.updateByOne(tableDataPO, wherePO);
        }));
        setCopyableCellFactory(statusColumn, po -> {
            if (po.getStatus() == null) return "";
            DataStatusEnum status = DataStatusEnum.getByCode(po.getStatus());
            return status != null ? status.getDesc() : po.getStatus();
        }, true);
        
        // 设置表名称和驼峰名列居中
        tableNameColumn.setStyle("-fx-alignment: center;");
        tableSnakeColumn.setStyle("-fx-alignment: center;");
        parentClassNameColumn.setStyle("-fx-alignment: center;");
        generCdFlgColumn.setStyle("-fx-alignment: center;");
        createTabFlgColumn.setStyle("-fx-alignment: center;");
        statusColumn.setStyle("-fx-alignment: center;");
        
        // 禁用所有列的排序功能
        selectColumn.setSortable(false);
        tableSnakeColumn.setSortable(false);
        tableNameColumn.setSortable(false);
        labeNameColumn.setSortable(false);
        tabCommentZhColumn.setSortable(false);
        tabCommentEnColumn.setSortable(false);
        parentClassPackageColumn.setSortable(false);
        parentClassNameColumn.setSortable(false);
        generCdFlgColumn.setSortable(false);
        createTabFlgColumn.setSortable(false);
        statusColumn.setSortable(false);
        actionColumn.setSortable(false);

        // 操作列：添加按钮
        actionColumn.setCellFactory(col -> new TableCell<TableDataPO, Void>() {
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            private final HBox hbox = new HBox(8, editBtn, delBtn);
            {
                // 设置编辑按钮样式 - 使用与PaneTransactionController相同的SVG图标样式
                javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
                editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                editIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                editIcon.setScaleX(0.7);
                editIcon.setScaleY(0.7);
                editBtn.setGraphic(editIcon);
                editBtn.setTooltip(new Tooltip("编辑"));
                editBtn.setMinWidth(28);
                editBtn.setPrefWidth(28);
                editBtn.setMaxWidth(28);
                editBtn.setMinHeight(28);
                editBtn.setPrefHeight(28);
                editBtn.setMaxHeight(28);
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent;"));
                editBtn.setOnMousePressed(e -> editBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                editBtn.setOnMouseReleased(e -> editBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                editBtn.setOnAction(e -> {
                    TableDataPO po = getTableView().getItems().get(getIndex());
                    openTableEdit(po.getTableNameSnake(), po.getCreateTabFlg(), po.getGenerCdFlg(), po.getParentClass());
                });
                
                // 设置删除按钮样式 - 使用与PaneExeRecordController相同的SVG图标样式
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
                    TableDataPO po = getTableView().getItems().get(getIndex());
                    if (po != null) {
                        deleteTableRecord(po);
                    }
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
        });

        // 填充标签名称下拉框，增加ALL选项
        List<String> labelNames = transactionList.stream()
                .map(TableDataPO::getLableName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .toList();
        labenameComboBox.getItems().setAll();
        labenameComboBox.getItems().add("ALL");
        labenameComboBox.getItems().addAll(labelNames);
        labenameComboBox.setValue("ALL");
        labenameComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty() || "ALL".equals(newVal)) {
                TableDataPO newReqPO = new TableDataPO();
                newReqPO.setAppName(globalProps.getAppName());
                newReqPO.setProjectName(globalProps.getProjectName());
                newReqPO.setGroupName(globalProps.getGroupName());
                List<TableDataPO> newList = tableDataRpcService.queryForList(newReqPO);
                transactionList.clear();
                transactionList.setAll(newList);
                tableTableView.setItems(transactionList);
            } else {
                TableDataPO newReqPO = new TableDataPO();
                newReqPO.setAppName(globalProps.getAppName());
                newReqPO.setProjectName(globalProps.getProjectName());
                newReqPO.setGroupName(globalProps.getGroupName());
                newReqPO.setLableName(newVal);
                List<TableDataPO> newList = tableDataRpcService.queryForList(newReqPO);
                transactionList.clear();
                transactionList.setAll(newList);
                tableTableView.setItems(transactionList);
            }
        });

        syncTabButton.setOnAction(event -> syncTable());
        importDdlButton.setOnAction(event -> importDdl());
        generateDdlButton.setOnAction(event -> generateDdl());
        generateExcelButton.setOnAction(event -> generateExcel());
        compareButton.setOnAction(event -> compareTableStructure());
        dataMigrationButton.setOnAction(event -> showDataMigrationDialog());

        // 绑定查询输入框和按钮
        if (nameFilterField == null) {
            nameFilterField = (TextField) syncTabButton.getScene().lookup(".text-field[promptText='名称']");
        }
        if (queryButton == null) {
            queryButton = (Button) syncTabButton.getScene().lookup(".button[text='查询']");
        }
        if (queryButton != null) {
            queryButton.setOnAction(event -> doQuery());
        }
    }

    // 更新全选框状态
    private void updateSelectAllCheckBox() {
        long selectedCount = selectedList.stream().filter(SimpleBooleanProperty::get).count();
        if (selectedCount == selectedList.size() && selectedCount > 0) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }



    // 跳转到表结构编辑界面并传递表名、标识字段和公共字段
    private void openTableEdit(String tableName, String createTabFlg, String generCdFlg, String parentClass) {
        try {
            MainController mainController = MrSpringContextHolder.getBean(MainController.class);
            Tab tableTab = mainController.getTabByKey("tableManager");
            if (tableTab != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pane_table.fxml"));
                loader.setControllerFactory(clazz -> MrSpringContextHolder.getBean(clazz));
                javafx.scene.Node tablePage = loader.load();
                tableTab.setContent(tablePage);
                mainController.getPageContainer().getSelectionModel().select(tableTab);
                // 设置表名和标识字段到新加载的Controller
                PaneTableController tableController = loader.getController();
                // 使用新的方法，在加载表结构后设置标识字段和公共字段
                tableController.setSelectedTableWithFlagsAndParentClass(tableName, createTabFlg, generCdFlg, parentClass);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("加载表结构编辑界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void syncTable() {
        try {
            // 显示进度对话框
            Dialog<Void> progressDialog = new Dialog<>();
            progressDialog.setTitle("正在同步表结构...");
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);
            Label progressLabel = new Label("正在同步数据库表结构...");
            VBox vbox = new VBox(16, progressLabel, progressBar);
            vbox.setPrefWidth(420);
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            progressDialog.getDialogPane().setContent(vbox);
            progressDialog.setResizable(false);
            progressDialog.show();
            
            // 在新线程中执行同步操作
            new Thread(() -> {
                try {
                    CrResult syncResult = tableEntityService.synchTableStructure(
                        (progress, tableName) -> Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            progressLabel.setText("正在同步表结构: " + tableName + " (" + String.format("%.1f", progress * 100) + "%)");
                        })
                    );
                    Platform.runLater(() -> {
                        progressDialog.close();
                        if (syncResult.isSucess()) {
                            ViewUtils.alertForSucess("已成功同步数据库中的表结构信息。");
                            refreshTable();
                        } else {
                            ViewUtils.alertForFail("同步过程中发生错误：" + syncResult.getMsgInf());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        progressDialog.close();
                        ViewUtils.alertForFail("同步过程中发生异常：" + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("同步表结构失败: " + e.getMessage());
        }
    }

    private void setCopyableCellFactory(TableColumn<TableDataPO, String> column, Function<TableDataPO, String> getter) {
        setCopyableCellFactory(column, getter, false);
    }
    
    private void setCopyableCellFactory(TableColumn<TableDataPO, String> column, Function<TableDataPO, String> getter, boolean centerAlign) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<TableDataPO, String>() {
            private final Label label = new Label();
            {
                label.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(label.getText());
                        clipboard.setContent(content);
                        Tooltip tp = new Tooltip("已复制: " + label.getText());
                        Tooltip.install(label, tp);
                        tp.show(label, event.getScreenX(), event.getScreenY());
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(tp::hide);
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
                if (centerAlign) {
                    setStyle("-fx-alignment: center;");
                } else {
                    setStyle("-fx-alignment: center-left;");
                }
            }
        });
    }
    
    private void setCopyableCellFactoryWithSchema(TableColumn<TableDataPO, String> column, Function<TableDataPO, String> getter) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<TableDataPO, String>() {
            private final Label label = new Label();
            {
                label.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        
                        // 获取schema和表名
                        String tableName = label.getText();
                        String schema = globalProps.getDDLSchema();
                        String fullTableName = schema != null && !schema.trim().isEmpty() ? 
                            schema + "." + tableName : tableName;
                        
                        content.putString(fullTableName);
                        clipboard.setContent(content);
                        Tooltip tp = new Tooltip("已复制: " + fullTableName);
                        Tooltip.install(label, tp);
                        tp.show(label, event.getScreenX(), event.getScreenY());
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(tp::hide);
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
                setStyle("-fx-alignment: center-left;");
            }
        });
    }

    @FXML
    private void refreshTable() {
        initialize();
    }





    private void openRestfulTest(TableDataPO po) {

    }

    @FXML
    private void importDdl() {
        ImpddlDialogUtil.showImportDdlDialog(statement -> {
            try {
                DataBaseHandler dataBaseHandler = globalProps.getInputDataBaseHandler();
                TableEntity tableEntity = dataBaseHandler.handlerCreateTable(statement);
                // 保存表结构到数据库
                CrResult crResult = tableEntityService.saveTableEntity(tableEntity);
                if (crResult != null && crResult.isSucess()) {
                    ViewUtils.alertForSucess("DDL导入成功！");
                    refreshTable();
                } else {
                    ViewUtils.alertForFail("DDL导入失败：" + (crResult != null ? crResult.getMsgInf() : "未知错误"));
                }
            } catch (Exception e) {
                ViewUtils.alertForFail("DDL解析失败：" + e.getMessage());
            }
        });
    }

    @FXML
    private void generateDdl() {
        // 获取选中的表
        List<TableDataPO> selectedTables = new ArrayList<>();
        for (int i = 0; i < transactionList.size(); i++) {
            if (selectedList.get(i).get()) {
                selectedTables.add(transactionList.get(i));
            }
        }
        
        if (selectedTables.isEmpty()) {
            ViewUtils.alertForFail("请先选择要生成DDL的表！");
            return;
        }
        
        try {
            // 构建TableEntity列表
            List<TableEntity> tableEntityList = new ArrayList<>();
            List<String> failedTables = new ArrayList<>();
            
            for (TableDataPO selectedTable : selectedTables) {
                String tableName = selectedTable.getTableNameSnake();
                
                if (StringUtils.isBlank(tableName)) {
                    failedTables.add(selectedTable.getTableNameCamel() + " (表名为空)");
                    continue;
                }
                
                try {
                    // 获取表结构
                    TableEntity tableEntity = tableEntityFactory.getTableEntity(tableName);
                    if (tableEntity == null) {
                        failedTables.add(selectedTable.getTableNameCamel() + " (未找到表结构)");
                        continue;
                    }
                    
                    String ddlSchema = globalProps.getDDLSchema();
                    tableEntity.setSchema(ddlSchema);
                    tableEntityList.add(tableEntity);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    failedTables.add(selectedTable.getTableNameCamel() + " (获取表结构异常: " + e.getMessage() + ")");
                }
            }
            
            // 如果有失败的表，显示错误信息并终止操作
            if (!failedTables.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("以下表获取表结构失败:\n");
                for (String failedTable : failedTables) {
                    errorMsg.append("- ").append(failedTable).append("\n");
                }
                ViewUtils.alertForFail(errorMsg.toString());
                return; // 直接返回，不继续执行
            }
            
            // 调用批量生成DDL方法
            if (!tableEntityList.isEmpty()) {
                CrResult<String> batchResult = tableEntityService.generateBatchDdl(tableEntityList);
                
                if (batchResult != null && batchResult.isSucess()) {
                    String ddlContent = batchResult.getData();
                    
                    // 如果有错误信息（部分失败的情况），显示错误信息并终止操作
                    if (StringUtils.isNotBlank(batchResult.getMsgInf())) {
                        ViewUtils.alertForFail(batchResult.getMsgInf());
                        return; // 直接返回，不显示DDL对话框
                    }
                    
                    // 只有在没有错误的情况下才显示DDL内容
                    String title = "DDL建表语句 (" + selectedTables.size() + "个表)";
                    SqlDialogUtil.showLogDialog(ddlContent, title, 900, 600);
                } else {
                    ViewUtils.alertForFail("生成DDL失败：" + (batchResult != null ? batchResult.getMsgInf() : "未知错误"));
                }
            } else {
                ViewUtils.alertForFail("没有成功获取任何表结构！");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("生成DDL过程中发生异常：" + e.getMessage());
        }
    }

    private void doQuery() {
        String name = nameFilterField != null ? nameFilterField.getText() : "";

        List<TableDataPO> list = null;
        if (StringUtils.isBlank(name)) {
            TableDataPO reqPO = new TableDataPO();
            reqPO.setAppName(globalProps.getAppName());
            reqPO.setProjectName(globalProps.getProjectName());
            reqPO.setGroupName(globalProps.getGroupName());
            list = tableDataRpcService.queryForList(reqPO);
        }else {
            list = tableDataRpcService.queryForSearch(name);
        }

        transactionList.setAll(list);
        tableTableView.setItems(transactionList);
        selectedList.clear();
        for (int i = 0; i < transactionList.size(); i++) {
            selectedList.add(new SimpleBooleanProperty(false));
        }
        tableTableView.refresh();
    }
    
    /**
     * 对比表结构
     */
    private void compareTableStructure() {
        // 获取选中的表
        List<TableDataPO> selectedTables = new ArrayList<>();
        for (int i = 0; i < transactionList.size(); i++) {
            if (selectedList.get(i).get()) {
                selectedTables.add(transactionList.get(i));
            }
        }
        
        if (selectedTables.isEmpty()) {
            ViewUtils.alertForFail("请先选择要对比的表！");
            return;
        }
        
        // 使用SelectDialogUtil显示带复选框的环境选择对话框
        List<String> envOptions = List.of("dev", "sit", "uat", "poc");
        SelectDialogUtil.showSelectDialogWithCheckboxes("选择对比环境", envOptions, "dev",
            compareOptions -> {
                // 执行对比
                try {
                    // 显示进度对话框
                    Dialog<Void> progressDialog = new Dialog<>();
                    progressDialog.setTitle("正在对比表结构...");
                    progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
                    ProgressBar progressBar = new ProgressBar(0);
                    progressBar.setPrefWidth(400);
                    Label progressLabel = new Label("正在对比表结构...");
                    VBox vbox = new VBox(16, progressLabel, progressBar);
                    vbox.setPrefWidth(420);
                    vbox.setAlignment(Pos.CENTER);
                    progressDialog.getDialogPane().setContent(vbox);
                    progressDialog.setResizable(false);
                    progressDialog.show();
                    
                    // 在新线程中执行对比操作
                    new Thread(() -> {
                        try {
                            CrResult<List<TableDiffPO>> compareResult =
                                tableDiffService.compareTableStructure(selectedTables, compareOptions, 
                                    (progress, message) -> {
                                        // 在JavaFX应用线程中更新UI
                                        Platform.runLater(() -> {
                                            progressBar.setProgress(progress);
                                            progressLabel.setText(message + " (" + String.format("%.1f", progress * 100) + "%)");
                                        });
                                    });
                            
                            Platform.runLater(() -> {
                                progressDialog.close();
                                if (compareResult.isSucess()) {
                                    List<com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO> diffList = compareResult.getData();
                                    if (diffList.isEmpty()) {
                                        ViewUtils.alertForSucess("对比完成，未发现差异！");
                                    } else {
                                        ViewUtils.alertForSucess("对比完成，发现 " + diffList.size() + " 个差异！");
                                    }
                                } else {
                                    ViewUtils.alertForFail("对比过程中发生错误：" + compareResult.getMsgInf());
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() -> {
                                progressDialog.close();
                                ViewUtils.alertForFail("对比过程中发生异常：" + e.getMessage());
                            });
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    ViewUtils.alertForFail("对比表结构失败: " + e.getMessage());
                }
            },
            () -> {}
        );
    }
    
    /**
     * 创建ToggleSwitch单元格工厂
     */
    private Callback<TableColumn<TableDataPO, String>, TableCell<TableDataPO, String>> createToggleSwitchCellFactory(
            java.util.function.Function<TableDataPO, String> getter,
            java.util.function.BiConsumer<TableDataPO, Boolean> setter) {
        
        return column -> new TableCell<TableDataPO, String>() {
            private final ToggleSwitch toggleSwitch = new ToggleSwitch();
            private boolean isUpdating = false; // 添加标志位防止初始化时触发
            
            {
                // 监听ToggleSwitch状态变化
                toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    // 只有在非更新状态下才执行setter
                    if (!isUpdating) {
                        TableDataPO po = getTableView().getItems().get(getIndex());
                        if (po != null) {
                            setter.accept(po, newVal);
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TableDataPO po = getTableView().getItems().get(getIndex());
                    if (po != null) {
                        String value = getter.apply(po);
                        boolean isSelected = "Y".equals(value);
                        
                        // 设置标志位，防止初始化时触发监听器
                        isUpdating = true;
                        toggleSwitch.setSelected(isSelected);
                        isUpdating = false;
                        
                        setGraphic(toggleSwitch);
                    }
                }
            }
        };
    }
    
    /**
     * 删除表记录
     * @param po 要删除的表记录
     */
    private void deleteTableRecord(TableDataPO po) {
        try {
            // 获取当前窗口的Stage
            Stage ownerStage = (Stage) tableTableView.getScene().getWindow();
            
            // 构建删除消息
            String deleteMessage = "确定要删除表 \"" + po.getTableNameCamel() + "\" 吗？";
            String itemName = po.getTableNameCamel();
            
            // 创建自定义复选框选项
            List<DeleteDialogUtil.CheckBoxOption> checkBoxOptions = new ArrayList<>();
            checkBoxOptions.add(new DeleteDialogUtil.CheckBoxOption("同时删除表结构", false, "delete_structure"));
            
            // 显示自定义删除对话框
            DeleteDialogUtil.DeleteResult result = DeleteDialogUtil.showDeleteDialog(
                "确认删除", 
                deleteMessage, 
                itemName,
                ownerStage,
                checkBoxOptions
            );
            
            // 处理删除结果
            if (result.isConfirmed()) {
                try {
                    // 根据用户选择的选项执行不同的删除逻辑
                    boolean deleteStructure = result.getCheckBoxStateByAction("delete_structure");
                    
                    if (deleteStructure) {
                        // 如果选择删除表结构，先检查表是否存在
                        try {
                            TableEntity tableEntity = tableEntityFactory.getTableEntity(po.getTableNameSnake());
                            if (tableEntity != null) {
                                // 删除表结构
                                CrResult dropResult = tableEntityService.dropTableEntity(tableEntity);
                                if (!dropResult.isSucess()) {
                                    ViewUtils.alertForFail("删除表结构失败：" + dropResult.getMsgInf());
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            // 表结构删除失败，但继续删除记录
                            ViewUtils.alertForFail("删除表结构时发生异常：" + e.getMessage());
                            return;
                        }
                    }
                    
                    // 删除表记录
                    TableDataPO wherePO = new TableDataPO();
                    wherePO.setTableNameCamel(po.getTableNameCamel());
                    wherePO.setGroupName(po.getGroupName());
                    wherePO.setProjectName(po.getProjectName());
                    
                    tableDataRpcService.delete(wherePO);
                    ViewUtils.alertForSucess("表记录删除成功！");
                    // 刷新表格数据
                    refreshTable();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    ViewUtils.alertForFail("删除过程中发生异常：" + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("删除操作失败：" + e.getMessage());
        }
    }
    
    /**
     * 显示数据迁移对话框
     */
    private void showDataMigrationDialog() {
        try {
            // 检查是否有选中的表
            List<TableDataPO> selectedTables = new ArrayList<>();
            for (int i = 0; i < transactionList.size(); i++) {
                if (selectedList.get(i).get()) {
                    selectedTables.add(transactionList.get(i));
                }
            }
            
            if (selectedTables.isEmpty()) {
                ViewUtils.alertForFail("请先选择要迁移数据的表！");
                return;
            }
            
            // 创建对话框
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("数据迁移");
            dialog.setHeaderText("请选择源环境和目标环境，将迁移选中的 " + selectedTables.size() + " 个表的数据");
            
            // 设置对话框按钮
            ButtonType confirmButtonType = new ButtonType("开始迁移", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
            
            // 创建对话框内容
            VBox content = new VBox(15);
            content.setPadding(new javafx.geometry.Insets(20));
            
            // 源环境下拉菜单
            Label sourceLabel = new Label("源环境:");
            sourceLabel.setStyle("-fx-font-weight: bold;");
            ComboBox<String> sourceComboBox = new ComboBox<>();
            sourceComboBox.setPromptText("请选择源环境");
            sourceComboBox.setPrefWidth(200);
            
            // 目标环境下拉菜单
            Label targetLabel = new Label("目标环境:");
            targetLabel.setStyle("-fx-font-weight: bold;");
            ComboBox<String> targetComboBox = new ComboBox<>();
            targetComboBox.setPromptText("请选择目标环境");
            targetComboBox.setPrefWidth(200);
            
            // 填充固定的环境选项
            List<String> environmentOptions = List.of("dev", "sit", "uat", "poc");
            sourceComboBox.getItems().addAll(environmentOptions);
            targetComboBox.getItems().addAll(environmentOptions);
            
            // 设置默认值
            sourceComboBox.setValue("uat");
            targetComboBox.setValue("dev");
            
            content.getChildren().addAll(sourceLabel, sourceComboBox, targetLabel, targetComboBox);
            dialog.getDialogPane().setContent(content);
            
            // 设置结果转换器
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    String sourceEnv = sourceComboBox.getValue();
                    String targetEnv = targetComboBox.getValue();
                    
                    if (sourceEnv == null || sourceEnv.isEmpty()) {
                        ViewUtils.alertForFail("请选择源环境");
                        return null;
                    }
                    
                    if (targetEnv == null || targetEnv.isEmpty()) {
                        ViewUtils.alertForFail("请选择目标环境");
                        return null;
                    }
                    
                    if (sourceEnv.equals(targetEnv)) {
                        ViewUtils.alertForFail("源环境和目标环境不能相同");
                        return null;
                    }
                    
                    // 执行数据迁移
                    executeDataMigration(selectedTables, sourceEnv, targetEnv);
                    
                    return sourceEnv + " -> " + targetEnv;
                }
                return null;
            });
            
            // 显示对话框
            dialog.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("显示数据迁移对话框失败：" + e.getMessage());
        }
    }
    
    /**
     * 执行数据迁移
     */
    private void executeDataMigration(List<TableDataPO> selectedTables, String sourceEnv, String targetEnv) {
        try {
            // 显示进度对话框
            Dialog<Void> progressDialog = new Dialog<>();
            progressDialog.setTitle("正在执行数据迁移...");
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            
            // 创建进度条和标签
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);
            Label progressLabel = new Label("正在准备数据迁移...");
            Label detailLabel = new Label(""); // 添加详细进度标签
            
            VBox vbox = new VBox(16, progressLabel, detailLabel, progressBar);
            vbox.setPrefWidth(420);
            vbox.setAlignment(Pos.CENTER);
            progressDialog.getDialogPane().setContent(vbox);
            progressDialog.setResizable(false);
            
            // 设置取消按钮的处理逻辑
            Button cancelButton = (Button) progressDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelButton != null) {
                cancelButton.setOnAction(event -> {
                    progressDialog.close();
                    ViewUtils.alertForSucess("数据迁移已取消");
                });
            }
            
            progressDialog.show();
            
            // 在新线程中执行数据迁移
            new Thread(() -> {
                try {
                    // 构建表名列表
                    List<String> tableNames = selectedTables.stream()
                        .map(TableDataPO::getTableNameSnake)
                        .filter(name -> name != null && !name.trim().isEmpty())
                        .toList();
                    
                    if (tableNames.isEmpty()) {
                        Platform.runLater(() -> {
                            progressDialog.close();
                            ViewUtils.alertForFail("没有有效的表名进行迁移");
                        });
                        return;
                    }
                    
                    // 获取数据库连接配置
                    DbConnectionPO sourceConnection = getDbConnection(sourceEnv,true);
                    DbConnectionPO targetConnection = getDbConnection(targetEnv,false);
                    
                    if (sourceConnection == null || targetConnection == null) {
                        Platform.runLater(() -> {
                            progressDialog.close();
                            ViewUtils.alertForFail("无法获取数据库连接配置");
                        });
                        return;
                    }
                    
                    // 使用带进度回调的数据迁移方法
                    CrResult<String> migrationResult = dataMigrationService.migrateWithProgress(
                        sourceConnection, 
                        targetConnection, 
                        tableNames,
                        new DataMigrationService.ProgressCallback() {
                            @Override
                            public void onProgress(int currentRows, int totalRows, double progress) {
                                Platform.runLater(() -> {
                                    // 更新进度条
                                    progressBar.setProgress(progress / 100.0);
                                    
                                    // 更新进度标签
                                    if (totalRows > 0) {
                                        progressLabel.setText(String.format("数据迁移进度: %.1f%%", progress));
                                        detailLabel.setText(String.format("已迁移: %d / %d 行数据", currentRows, totalRows));
                                    } else {
                                        progressLabel.setText("正在迁移数据...");
                                        detailLabel.setText(String.format("已迁移: %d 行数据", currentRows));
                                    }
                                });
                            }
                            
                            @Override
                            public void onProgress(int currentRows, int totalRows, double progress, String tableName) {
                                Platform.runLater(() -> {
                                    // 更新进度条
                                    progressBar.setProgress(progress / 100.0);
                                    
                                    // 更新进度标签
                                    if (totalRows > 0) {
                                        progressLabel.setText(String.format("数据迁移进度: %.1f%%", progress));
                                        detailLabel.setText(String.format("已迁移: %d / %d 行数据 (当前表: %s)", currentRows, totalRows, tableName));
                                    } else {
                                        progressLabel.setText("正在迁移数据...");
                                        detailLabel.setText(String.format("已迁移: %d 行数据 (当前表: %s)", currentRows, tableName));
                                    }
                                });
                            }
                        }
                    );
                    
                    Platform.runLater(() -> {
                        progressDialog.close();
                        if (migrationResult.isSucess()) {
                            ViewUtils.alertForSucess("数据迁移完成！\n\n" + migrationResult.getData());
                        } else {
                            // 显示详细的错误信息
                            String errorTitle = "数据迁移失败";
                            String errorContent = migrationResult.getMsgInf();
                            
                            // 如果错误信息很长，使用对话框显示
                            if (errorContent != null && errorContent.length() > 200) {
                                SqlDialogUtil.showLogDialog(errorContent, errorTitle, 800, 600);
                            } else {
                                ViewUtils.alertForFail("数据迁移失败：" + errorContent);
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        progressDialog.close();
                        ViewUtils.alertForFail("数据迁移过程中发生异常：" + e.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("启动数据迁移失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据环境获取数据库连接配置
     */
    private DbConnectionPO getDbConnection(String envName,boolean readFlg) {
        try {
            // 构建查询条件
            DbConnectionPO queryPO = new DbConnectionPO();
            queryPO.setGroupName(globalProps.getGroupName());
            queryPO.setProjectName(globalProps.getProjectName());
            queryPO.setAppName(globalProps.getAppName());
            queryPO.setEnvName(envName);
            
            // 查询数据库连接配置
            List<DbConnectionPO> connections = dbConnectionRpcService.queryForList(queryPO);
            
            if (connections != null && !connections.isEmpty()) {
                // 优先返回主数据库连接
                for (DbConnectionPO conn : connections) {
                    if ("Y".equals(conn.getMainFlg())) {
                        return conn;
                    }
                }
                // 如果没有主数据库，返回第一个
                return connections.get(0);
            }
            
            // 如果没有找到配置，尝试查询"all"环境的配置
            queryPO.setAppName("all");
            queryPO.setProjectName("all");
            connections = dbConnectionRpcService.queryForList(queryPO);
            
            if (connections != null && !connections.isEmpty()) {
                for (DbConnectionPO conn : connections) {
                    if ("Y".equals(conn.getMainFlg())) {
                        return conn;
                    }
                }
                return connections.get(0);
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 生成Excel文件
     */
    @FXML
    private void generateExcel() {
        try {
            // 获取选中的表
            List<TableDataPO> selectedTables = new ArrayList<>();
            for (int i = 0; i < transactionList.size(); i++) {
                if (selectedList.get(i).get()) {
                    selectedTables.add(transactionList.get(i));
                }
            }
            
            if (selectedTables.isEmpty()) {
                ViewUtils.alertForFail("请先选择要生成Excel的表！");
                return;
            }
            
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择Excel文件保存位置");
            String fileName = globalProps.getProjectName() + "_表结构_" + System.currentTimeMillis() + ".xlsx";
            fileChooser.setInitialFileName(fileName);
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
            );
            
            // 获取当前窗口
            Stage stage = (Stage) generateExcelButton.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);
            
            if (selectedFile != null) {
                // 显示进度对话框
                Dialog<Void> progressDialog = new Dialog<>();
                progressDialog.setTitle("正在生成Excel...");
                progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
                ProgressBar progressBar = new ProgressBar(0);
                progressBar.setPrefWidth(400);
                Label progressLabel = new Label("正在生成Excel文件...");
                VBox vbox = new VBox(16, progressLabel, progressBar);
                vbox.setPrefWidth(420);
                vbox.setAlignment(Pos.CENTER);
                progressDialog.getDialogPane().setContent(vbox);
                progressDialog.setResizable(false);
                progressDialog.show();
                
                // 在新线程中执行Excel生成操作
                Task<Void> excelTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        generateExcelData(selectedFile, selectedTables, progressLabel);
                        return null;
                    }
                };
                
                excelTask.setOnSucceeded(event -> {
                    progressDialog.close();
                    ViewUtils.alertForSucess("Excel文件生成成功！\n文件位置：" + selectedFile.getAbsolutePath());
                });
                
                excelTask.setOnFailed(event -> {
                    progressDialog.close();
                    ViewUtils.alertForFail("Excel生成失败：" + excelTask.getException().getMessage());
                });
                
                excelTask.setOnCancelled(event -> {
                    progressDialog.close();
                    ViewUtils.alertForSucess("Excel生成已取消");
                });
                
                new Thread(excelTask).start();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("生成Excel时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行Excel数据生成
     */
    private void generateExcelData(File file, List<TableDataPO> selectedTables, Label progressLabel) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            int totalTables = selectedTables.size();
            final int[] processedTables = {0};
            
            // 为每个选中的表创建一个Sheet
            for (TableDataPO selectedTable : selectedTables) {
                processedTables[0]++;
                
                // 更新进度
                final int currentProcessed = processedTables[0];
                Platform.runLater(() -> {
                    progressLabel.setText(String.format("正在生成表结构: %s (%d/%d)", 
                        selectedTable.getTableNameCamel(), currentProcessed, totalTables));
                });
                
                String tableName = selectedTable.getTableNameSnake();
                if (StringUtils.isBlank(tableName)) {
                    continue; // 跳过表名为空的记录
                }
                
                try {
                    // 获取表结构
                    TableEntity tableEntity = tableEntityFactory.getTableEntity(tableName);
                    if (tableEntity == null) {
                        continue; // 跳过无法获取表结构的记录
                    }
                    
                    // 创建Sheet，使用驼峰名作为Sheet名称
                    String sheetName = selectedTable.getTableNameCamel();
                    // Excel Sheet名称长度限制为31个字符
                    if (sheetName.length() > 31) {
                        sheetName = sheetName.substring(0, 31);
                    }
                    Sheet sheet = workbook.createSheet(sheetName);
                    
                    // 创建标题行样式
                    CellStyle headerStyle = createHeaderStyle(workbook);
                    CellStyle dataStyle = createDataStyle(workbook);
                    
                    // 创建标题行
                    Row headerRow = sheet.createRow(0);
                    String[] headers = {
                        "字段名", "Java类型", "数据库类型", "长度", "精度", "是否主键", 
                        "是否非空", "默认值", "中文注释", "英文注释", "状态"
                    };
                    
                    for (int i = 0; i < headers.length; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                        sheet.setColumnWidth(i, 15 * 256); // 设置列宽
                    }
                    
                    // 填充字段数据
                    List<RxField> fields = tableEntity.getRxFields();
                    if (fields != null && !fields.isEmpty()) {
                        int rowIndex = 1;
                        for (RxField field : fields) {
                            Row dataRow = sheet.createRow(rowIndex++);
                            
                            dataRow.createCell(0).setCellValue(field.getNameSnake() != null ? field.getNameSnake() : "");
                            dataRow.createCell(1).setCellValue(field.getType() != null ? field.getType() : "");
                            dataRow.createCell(2).setCellValue(field.getDbTyp() != null ? field.getDbTyp() : "");
                            dataRow.createCell(3).setCellValue(field.getLength() != null ? field.getLength() : 0);
                            dataRow.createCell(4).setCellValue(""); // 精度字段在RxField中不存在，设为空
                            dataRow.createCell(5).setCellValue(""); // 主键标识在RxField中不存在，设为空
                            dataRow.createCell(6).setCellValue(field.isNotNull() ? "Y" : "N");
                            dataRow.createCell(7).setCellValue(field.getDefaultValue() != null ? field.getDefaultValue() : "");
                            dataRow.createCell(8).setCellValue(field.getCommentCn() != null ? field.getCommentCn() : "");
                            dataRow.createCell(9).setCellValue(field.getCommentEn() != null ? field.getCommentEn() : "");
                            
                            // 状态列
                            String statusDesc = "";
                            if (field.getBizUpdSts() != null) {
                                statusDesc = field.getBizUpdSts().getDesc();
                            }
                            dataRow.createCell(10).setCellValue(statusDesc);
                            
                            // 应用数据样式
                            for (int i = 0; i < 11; i++) {
                                dataRow.getCell(i).setCellStyle(dataStyle);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    // 继续处理下一个表，不中断整个流程
                }
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
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    /**
     * 创建数据行样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
} 