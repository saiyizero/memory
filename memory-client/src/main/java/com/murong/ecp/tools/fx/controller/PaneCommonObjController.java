package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.CommonClassDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.SqlDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.FilteredEditingCell;
import com.murong.ecp.tools.fx.infrastructure.converter.InterfaceConvert;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.converter.DefaultStringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PaneCommonObjController implements Initializable {

    /**
     * 类类型选项类
     */
    public static class ClassTypeOption {
        private String value;
        private String description;
        
        public ClassTypeOption(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return value + "-" + description;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ClassTypeOption that = (ClassTypeOption) obj;
            return value != null ? value.equals(that.value) : that.value == null;
        }
        
        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @Autowired
    private CommonClassDao commonClassDao;

    @Autowired
    private GlobalProperties globalProps;

    // 左侧表格相关控件
    @FXML private TableView<CommonClassPO> tableView;
    @FXML private TableColumn<CommonClassPO, Boolean> selectColumn;
    @FXML private TableColumn<CommonClassPO, String> classNameColumn;
    @FXML private TableColumn<CommonClassPO, String> classTypeColumn;
    @FXML private TableColumn<CommonClassPO, String> classPathColumn;
    @FXML private TableColumn<CommonClassPO, String> appNameColumn;
    
    // 左侧功能控件
    @FXML private CheckBox selectAllCheckBox;
    @FXML private TextField filterField;
    @FXML private Button clearButton;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    
    // 右侧详情控件
    @FXML private TextField classNameField;
    @FXML private ComboBox<ClassTypeOption> classTypeComboBox;
    @FXML private TextField classPathField;
    @FXML private TextField classCommentCnField;
    @FXML private TextField classCommentEnField;
    @FXML private TextField completedFlgField;
    @FXML private Button saveButton;
    
    // 字段详情表格控件
    @FXML private TableView<RxField> fieldsTable;
    @FXML private TableColumn<RxField, String> fieldNameColumn;
    @FXML private TableColumn<RxField, String> fieldTypeColumn;
    @FXML private TableColumn<RxField, String> fieldDbTypeColumn;
    @FXML private TableColumn<RxField, String> fieldLengthColumn;
    @FXML private TableColumn<RxField, String> fieldNotNullColumn;
    @FXML private TableColumn<RxField, String> fieldDefaultColumn;
    @FXML private TableColumn<RxField, String> fieldCommentCnColumn;
    @FXML private TableColumn<RxField, String> fieldCommentEnColumn;
    @FXML private TableColumn<RxField, Void> fieldDeleteColumn;
    @FXML private Button addFieldButton;
    @FXML private Button deleteFieldButton;

    private ObservableList<CommonClassPO> dataList = FXCollections.observableArrayList();
    private FilteredList<CommonClassPO> filteredData = new FilteredList<>(dataList, p -> true);
    private ObservableList<RxField> fieldsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        initializeLeftTable();
        initializeRightPanel();
        initializeFieldsTable();
        initializeEventHandlers();
        loadData();
    }

    private void initializeLeftTable() {
        // 设置表头
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        classTypeColumn.setCellValueFactory(new PropertyValueFactory<>("classType"));
        classPathColumn.setCellValueFactory(new PropertyValueFactory<>("classPath"));
        appNameColumn.setCellValueFactory(new PropertyValueFactory<>("appName"));
        appNameColumn.setStyle("-fx-alignment: center;");

        // 复选框列
        selectColumn.setCellValueFactory(cellData -> {
            CommonClassPO po = cellData.getValue();
            if (po.getSelectedProperty() == null) {
                po.setSelectedProperty(new SimpleBooleanProperty(false));
            }
            return po.getSelectedProperty();
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));

        // 全选/全不选逻辑
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (CommonClassPO po : dataList) {
                if (po.getSelectedProperty() == null) {
                    po.setSelectedProperty(new SimpleBooleanProperty(false));
                }
                po.getSelectedProperty().set(newVal);
            }
            tableView.refresh();
        });

        // 过滤功能
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(po -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filterText = newVal.toLowerCase();
                return (po.getClassName() != null && po.getClassName().toLowerCase().contains(filterText)) ||
                       (po.getClassType() != null && po.getClassType().toLowerCase().contains(filterText)) ||
                       (po.getClassPath() != null && po.getClassPath().toLowerCase().contains(filterText));
            });
        });
        tableView.setItems(filteredData);
    }

    private void initializeRightPanel() {
        // 右侧面板初始化为只读状态
        classNameField.setEditable(false);
        
        // 设置类类型下拉框的选项
        ObservableList<ClassTypeOption> classTypeOptions = FXCollections.observableArrayList(
            new ClassTypeOption("TAB", "表公共字段"),
            new ClassTypeOption("PCLS", "继承父类"),
            new ClassTypeOption("REF", "引用对象")
        );
        classTypeComboBox.setItems(classTypeOptions);
        classTypeComboBox.setPromptText("请选择类类型");
    }

    private void initializeFieldsTable() {
        // 设置字段表格
        fieldsTable.setEditable(true);
        fieldsTable.setItems(fieldsList);
        
        // 字段名列
        fieldNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNameCamel()));
        fieldNameColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldNameColumn.setOnEditCommit(event -> event.getRowValue().setNameCamel(event.getNewValue()));

        // 字段类型列
        fieldTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        fieldTypeColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldTypeColumn.setOnEditCommit(event -> event.getRowValue().setType(event.getNewValue()));

        // 数据库类型列
        fieldDbTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDbTyp()));
        fieldDbTypeColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldDbTypeColumn.setOnEditCommit(event -> event.getRowValue().setDbTyp(event.getNewValue()));

        // 字段长度列
        fieldLengthColumn.setCellValueFactory(cell -> {
            Integer length = cell.getValue().getLength();
            return new SimpleStringProperty(length != null ? length.toString() : "");
        });
        fieldLengthColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldLengthColumn.setOnEditCommit(event -> {
            try {
                Integer length = Integer.parseInt(event.getNewValue());
                event.getRowValue().setLength(length);
            } catch (NumberFormatException e) {
                // 忽略无效输入
            }
        });
        fieldLengthColumn.setStyle("-fx-alignment: center;");

        // 非空列
        fieldNotNullColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isNotNull() ? "是" : "否"));
        fieldNotNullColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldNotNullColumn.setOnEditCommit(event -> {
            boolean notNull = "是".equals(event.getNewValue()) || "true".equals(event.getNewValue().toLowerCase());
            event.getRowValue().setNotNull(notNull);
        });
        fieldNotNullColumn.setStyle("-fx-alignment: center;");

        // 默认值列
        fieldDefaultColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDefaultValue()));
        fieldDefaultColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldDefaultColumn.setOnEditCommit(event -> event.getRowValue().setDefaultValue(event.getNewValue()));

        // 中文注释列
        fieldCommentCnColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCommentCn()));
        fieldCommentCnColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldCommentCnColumn.setOnEditCommit(event -> event.getRowValue().setCommentCn(event.getNewValue()));

        // 英文注释列
        fieldCommentEnColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCommentEn()));
        fieldCommentEnColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        fieldCommentEnColumn.setOnEditCommit(event -> event.getRowValue().setCommentEn(event.getNewValue()));

        // 删除操作列
        fieldDeleteColumn.setCellFactory(col -> new TableCell<RxField, Void>() {
            private final Button delBtn = new Button();
            {
                // 设置删除按钮样式 - 使用与枚举界面相同的SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                delBtn.setGraphic(trashIcon);
                delBtn.setTooltip(new Tooltip("删除"));
                delBtn.setMinWidth(20);
                delBtn.setPrefWidth(20);
                delBtn.setMaxWidth(20);
                delBtn.setMinHeight(20);
                delBtn.setPrefHeight(20);
                delBtn.setMaxHeight(20);
                delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent;"));
                delBtn.setOnMousePressed(e -> delBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                delBtn.setOnMouseReleased(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                delBtn.setOnAction(e -> {
                    RxField field = getTableView().getItems().get(getIndex());
                    fieldsList.remove(field);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(delBtn);
                }
                setStyle("-fx-alignment: center;");
            }
        });
    }

    private void initializeEventHandlers() {
        // 清空按钮
        clearButton.setOnAction(e -> {
            filterField.clear();
            tableView.getSelectionModel().clearSelection();
            clearRightPanel();
            loadData();
        });

        // 导出按钮
        exportButton.setOnAction(e -> {
            List<CommonClassPO> selectedItems = dataList.stream()
                    .filter(po -> po.getSelectedProperty() != null && po.getSelectedProperty().get())
                    .toList();
            
            if (selectedItems.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "请先勾选要导出的公共对象").showAndWait();
                return;
            }
            
            StringBuilder exportText = new StringBuilder();
            exportText.append("-- ==========================================\n");
            exportText.append("-- 公共对象导出 - 共 ").append(selectedItems.size()).append(" 个对象\n");
            exportText.append("-- ==========================================\n\n");
            
            for (int i = 0; i < selectedItems.size(); i++) {
                CommonClassPO po = selectedItems.get(i);
                if (i > 0) {
                    exportText.append("\n-- ------------------------------------------\n");
                }
                exportText.append("-- 对象 ").append(i + 1).append(": ").append(po.getClassName()).append("\n");
                exportText.append("-- 类型: ").append(po.getClassType()).append("\n");
                exportText.append("-- 路径: ").append(po.getClassPath()).append("\n");
                exportText.append("-- 中文注释: ").append(po.getClassCommentCn()).append("\n");
                exportText.append("-- 英文注释: ").append(po.getClassCommentEn()).append("\n");
                exportText.append("-- 完成标志: ").append(po.getCompletedFlg()).append("\n");
                exportText.append("-- 应用名称: ").append(po.getAppName()).append("\n");
                
                if (po.getFieldsJson() != null && !po.getFieldsJson().isEmpty()) {
                    exportText.append("-- 字段JSON:\n").append(po.getFieldsJson()).append("\n");
                }
                if (po.getIndexesJson() != null && !po.getIndexesJson().isEmpty()) {
                    exportText.append("-- 索引JSON:\n").append(po.getIndexesJson()).append("\n");
                }
            }
            
            String title = "公共对象导出 - 共 " + selectedItems.size() + " 个对象";
            SqlDialogUtil.showLogDialog(exportText.toString(), title, 900, 600);
        });

        // 删除按钮
        deleteButton.setOnAction(e -> {
            List<CommonClassPO> selectedItems = dataList.stream()
                    .filter(po -> po.getSelectedProperty() != null && po.getSelectedProperty().get())
                    .toList();
            
            if (selectedItems.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "请先勾选要删除的公共对象").showAndWait();
                return;
            }
            
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("确认删除");
            confirmDialog.setHeaderText("确认删除选中的公共对象？");
            confirmDialog.setContentText("将删除 " + selectedItems.size() + " 个对象，此操作不可恢复。");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // 这里可以添加实际的删除逻辑
                    dataList.removeAll(selectedItems);
                    new Alert(Alert.AlertType.INFORMATION, "删除成功").showAndWait();
                    updateSelectAllCheckBox();
                }
            });
        });

        // 保存按钮
        saveButton.setOnAction(e -> {
            CommonClassPO selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "请先选择一个公共对象").showAndWait();
                return;
            }
            
            // 更新选中对象的信息
            ClassTypeOption selectedTypeOption = classTypeComboBox.getValue();
            selected.setClassType(selectedTypeOption != null ? selectedTypeOption.getValue() : null);
            selected.setClassPath(classPathField.getText());
            selected.setClassCommentCn(classCommentCnField.getText());
            selected.setClassCommentEn(classCommentEnField.getText());
            selected.setCompletedFlg(completedFlgField.getText());
            
            // 保存字段信息到JSON
            try {
                String fieldsJson = InterfaceConvert.fieldsToJson(fieldsList);
                selected.setFieldsJson(fieldsJson);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "字段信息保存失败: " + ex.getMessage()).showAndWait();
                return;
            }
            
            // 保存到数据库
            commonClassDao.updateByOne(selected, selected);
            new Alert(Alert.AlertType.INFORMATION, "保存成功").showAndWait();
            tableView.refresh();
        });

        // 新增字段按钮
        addFieldButton.setOnAction(e -> {
            RxField newField = new RxField();
            newField.setNameCamel("newField");
            newField.setType("String");
            newField.setDbTyp("VARCHAR");
            newField.setLength(50);
            newField.setNotNull(false);
            newField.setCommentCn("新字段");
            newField.setCommentEn("New Field");
            fieldsList.add(newField);
        });

        // 删除字段按钮
        deleteFieldButton.setOnAction(e -> {
            RxField selected = fieldsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fieldsList.remove(selected);
            }
        });

        // 选中行时，右侧显示对应详情
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayObjectDetails(newVal);
            } else {
                clearRightPanel();
            }
        });
    }

    private void loadData() {
        CommonClassPO query = new CommonClassPO();
        query.setAppName(globalProps.getAppName());
        query.setGroupName(globalProps.getGroupName());
        List<CommonClassPO> list = commonClassDao.queryForList(query);
        dataList.setAll(list);
        
        // 监听每个PO的selectedProperty
        for (CommonClassPO po : dataList) {
            if (po.getSelectedProperty() != null) {
                po.getSelectedProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
            }
        }
        updateSelectAllCheckBox();
    }

    private void displayObjectDetails(CommonClassPO po) {
        classNameField.setText(po.getClassName());
        
        // 根据值设置下拉框选中项
        String classType = po.getClassType();
        ClassTypeOption selectedOption = null;
        if (classType != null) {
            for (ClassTypeOption option : classTypeComboBox.getItems()) {
                if (classType.equals(option.getValue())) {
                    selectedOption = option;
                    break;
                }
            }
        }
        classTypeComboBox.setValue(selectedOption);
        
        classPathField.setText(po.getClassPath());
        classCommentCnField.setText(po.getClassCommentCn());
        classCommentEnField.setText(po.getClassCommentEn());
        completedFlgField.setText(po.getCompletedFlg());
        
        // 加载字段信息
        loadFields(po);
    }

    private void clearRightPanel() {
        classNameField.clear();
        classTypeComboBox.setValue(null);
        classPathField.clear();
        classCommentCnField.clear();
        classCommentEnField.clear();
        completedFlgField.clear();
        
        // 清空字段列表
        fieldsList.clear();
    }

    private void loadFields(CommonClassPO po) {
        fieldsList.clear();
        if (po.getFieldsJson() != null && !po.getFieldsJson().isEmpty()) {
            try {
                List<RxField> fields = InterfaceConvert.jsonToFields(po.getFieldsJson());
                if (fields != null) {
                    fieldsList.addAll(fields);
                }
            } catch (Exception e) {
                // 如果解析失败，显示错误信息
                new Alert(Alert.AlertType.WARNING, "字段信息解析失败: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void updateSelectAllCheckBox() {
        if (selectAllCheckBox == null) return;
        if (dataList.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            return;
        }
        long selectedCount = dataList.stream().filter(po -> po.getSelectedProperty() != null && po.getSelectedProperty().get()).count();
        if (selectedCount == dataList.size()) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }
} 