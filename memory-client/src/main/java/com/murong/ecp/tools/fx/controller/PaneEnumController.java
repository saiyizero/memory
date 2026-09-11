package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.GenerateSqlService;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumGroupPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.FilteredEditingCell;
import com.murong.ecp.tools.fx.infrastructure.view.SqlDialogUtil;
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
public class PaneEnumController implements Initializable {

    @Autowired
    private EnumDictDao enumDictDao;

    @Autowired
    private GlobalProperties globalPropes;

    @Autowired
    private GenerateSqlService generaSqlService;

    @FXML private TableView<EnumGroupPO> enumTypeTableView;
    @FXML private TableColumn<EnumGroupPO, Boolean> selectColumn;
    @FXML private TableColumn<EnumGroupPO, String> enumNmeColumn;
    @FXML private TableColumn<EnumGroupPO, String> dbNameColumn;
    @FXML private TableColumn<EnumGroupPO, String> appNameColumn;
    @FXML private CheckBox selectAllCheckBox;
    @FXML private TextField filterField;
    @FXML private Button clearButton;
    @FXML private Button addEnumTypeButton;
    @FXML private Button deleteEnumTypeButton;

    @FXML private TextField enumNmeField;
    @FXML private TextField dbNameField;
    @FXML private Button saveButton;

    @FXML private TableView<EnumDictPO> enumItemTable;
    @FXML private TableColumn<EnumDictPO, String> enumCdColumn;
    @FXML private TableColumn<EnumDictPO, String> enumValColumn;
    @FXML private TableColumn<EnumDictPO, String> descCnItemColumn;
    @FXML private TableColumn<EnumDictPO, String> descEnItemColumn;
    @FXML private TableColumn<EnumDictPO, Void> deleteColumn;
    @FXML private Button addEnumItemButton;
    @FXML private Button deleteEnumItemButton;

    private ObservableList<EnumGroupPO> enumTypeList = FXCollections.observableArrayList();
    private FilteredList<EnumGroupPO> filteredEnumTypes = new FilteredList<>(enumTypeList, p -> true);
    private ObservableList<EnumDictPO> enumItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalPropes)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 设置表头
        enumNmeColumn.setCellValueFactory(new PropertyValueFactory<>("enumNme"));
        dbNameColumn.setCellValueFactory(new PropertyValueFactory<>("dbName"));
        appNameColumn.setCellValueFactory(new PropertyValueFactory<>("appName"));
        appNameColumn.setStyle("-fx-alignment: center;");

        // 复选框列
        selectColumn.setCellValueFactory(cellData -> {
            EnumGroupPO po = cellData.getValue();
            if (po.getSelectedProperty() == null) {
                po.setSelectedProperty(new SimpleBooleanProperty(false));
            }
            return po.getSelectedProperty();
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));

        // 全选/全不选逻辑
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (EnumGroupPO po : enumTypeList) {
                if (po.getSelectedProperty() == null) {
                    po.setSelectedProperty(new SimpleBooleanProperty(false));
                }
                po.getSelectedProperty().set(newVal);
            }
            enumTypeTableView.refresh();
        });

        // 复选框单独监听，更新全选状态
        enumTypeList.addListener((javafx.collections.ListChangeListener<EnumGroupPO>) change -> updateSelectAllCheckBox());
        // 每个PO的selectedProperty监听
        for (EnumGroupPO po : enumTypeList) {
            if (po.getSelectedProperty() != null) {
                po.getSelectedProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
            }
        }

        // 过滤
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredEnumTypes.setPredicate(type -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return type.getEnumNme().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        enumTypeTableView.setItems(filteredEnumTypes);

        // 清空按钮
        clearButton.setOnAction(e -> {
            filterField.clear();
            enumTypeTableView.getSelectionModel().clearSelection();
            enumNmeField.clear();
            dbNameField.clear();
            enumItems.clear();
            // 重新查询枚举表刷新数据
            loadEnumTypeList();
        });

        // 新增类型按钮（改为导出数据）
        addEnumTypeButton.setText("导出数据");
        addEnumTypeButton.setOnAction(e -> {
            // 获取所有勾选的枚举类型
            List<EnumGroupPO> selectedTypes = enumTypeList.stream()
                    .filter(po -> po.getSelectedProperty() != null && po.getSelectedProperty().get())
                    .toList();
            
            if (selectedTypes.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "请先勾选要导出的枚举类型").showAndWait();
                return;
            }
            
            StringBuilder allSqlText = new StringBuilder();
            int processedCount = 0;
            
            for (EnumGroupPO selectedType : selectedTypes) {
                // 查询该类型下所有枚举项
                EnumDictPO query = new EnumDictPO();
                query.setEnumNme(selectedType.getEnumNme());
                List<EnumDictPO> items = enumDictDao.queryForList(query);
                
                if (items == null || items.isEmpty()) {
                    continue; // 跳过没有枚举项的类型
                }
                
                // 生成SQL
                List<String> buiHlpInsertSqlList = generaSqlService.generateBuiHlpInsertSql(items);
                List<String> pubI18nInsertSqlList = generaSqlService.generatePubI18nInsertSql(items);
                List<String> buiHlpDeleteSqlList = generaSqlService.generateBuiHlpDeleteSql(items);
                List<String> pubI18nDeleteSqlList = generaSqlService.generatePubI18nDeleteSql(items);
                
                String buiHlpDeleteSqlText = String.join("\n", buiHlpDeleteSqlList);
                String pubI18nDeleteSqlText = String.join("\n", pubI18nDeleteSqlList);
                String buiHlpInsertSqlText = String.join("\n\n", buiHlpInsertSqlList);
                String pubI18nInsertSqlText = String.join("\n\n", pubI18nInsertSqlList);

                // 添加分隔符和标题
                if (processedCount > 0) {
                    allSqlText.append("\n\n-- ==========================================\n");
                    allSqlText.append("-- 枚举类型: ").append(selectedType.getEnumNme()).append("\n");
                    allSqlText.append("-- ==========================================\n\n");
                } else {
                    allSqlText.append("-- ==========================================\n");
                    allSqlText.append("-- 枚举类型: ").append(selectedType.getEnumNme()).append("\n");
                    allSqlText.append("-- ==========================================\n\n");
                }
                
                allSqlText.append("-- 清除表: buiHlp 中枚举 ").append(items.get(0).getEnumNme()).append("\n");
                allSqlText.append(buiHlpDeleteSqlText).append("\n\n");
                allSqlText.append("-- 清除表: pubI18n 中枚举 ").append(items.get(0).getEnumNme()).append("\n");
                allSqlText.append(pubI18nDeleteSqlText).append("\n\n");
                allSqlText.append("-- 插入表: buiHlp 中枚举 ").append(items.get(0).getEnumNme()).append("\n");
                allSqlText.append(buiHlpInsertSqlText).append("\n\n");
                allSqlText.append("-- 插入表: pubI18n 中枚举 ").append(items.get(0).getEnumNme()).append("\n");
                allSqlText.append(pubI18nInsertSqlText);
                
                processedCount++;
            }
            
            if (processedCount == 0) {
                new Alert(Alert.AlertType.WARNING, "选中的枚举类型都没有枚举项").showAndWait();
                return;
            }
            
            // 使用SqlDialogUtil显示SQL内容
            String title = "批量导出SQL - 共处理 " + processedCount + " 个枚举类型";
            SqlDialogUtil.showLogDialog(allSqlText.toString(), title, 900, 600);
        });

        // 删除类型按钮（可根据需要实现）
        deleteEnumTypeButton.setOnAction(e -> {
            // ...
        });

        // 选中行时，右侧显示对应枚举项
        enumTypeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadEnumItems(newVal.getEnumNme());
                enumNmeField.setText(newVal.getEnumNme());
                dbNameField.setText(newVal.getDbName());
            }
        });

        // 右侧表格
        enumItemTable.setEditable(true); // 确保表格是可编辑的
        enumItemTable.setItems(enumItems);
        enumCdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEnumCd()));
        enumCdColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        enumCdColumn.setOnEditCommit(event -> event.getRowValue().setEnumCd(event.getNewValue()));

        enumValColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEnumVal()));
        enumValColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        enumValColumn.setOnEditCommit(event -> event.getRowValue().setEnumVal(event.getNewValue()));
        enumValColumn.setStyle("-fx-alignment: center;");

        descCnItemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescCn()));
        descCnItemColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        descCnItemColumn.setOnEditCommit(event -> event.getRowValue().setDescCn(event.getNewValue()));

        descEnItemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescEn()));
        descEnItemColumn.setCellFactory(col -> new FilteredEditingCell<>(new DefaultStringConverter()));
        descEnItemColumn.setOnEditCommit(event -> event.getRowValue().setDescEn(event.getNewValue()));

        deleteColumn.setCellFactory(col -> new TableCell<EnumDictPO, Void>() {
            private final Button delBtn = new Button();
            {
                // 设置删除按钮样式 - 使用与PaneTransactionController相同的SVG图标样式
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
                    EnumDictPO po = getTableView().getItems().get(getIndex());
                    enumItems.remove(po);
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

        addEnumItemButton.setOnAction(e -> {
            EnumDictPO po = new EnumDictPO();
            po.setEnumNme(enumNmeField.getText());
            enumItems.add(po);
        });

        deleteEnumItemButton.setOnAction(e -> {
            EnumDictPO selected = enumItemTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                enumItems.remove(selected);
            }
        });

        saveButton.setOnAction(e -> {
            String enumNme = enumNmeField.getText();
            if (enumNme == null || enumNme.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "类型名不能为空").showAndWait();
                return;
            }
            for (EnumDictPO po : enumItems) {
                po.setEnumNme(enumNme);
            }
            enumDictDao.saveAll(enumItems);
            new Alert(Alert.AlertType.INFORMATION, "保存成功").showAndWait();
            loadEnumTypeList();
        });

        // 初始化加载分组
        loadEnumTypeList();
    }

    private void updateSelectAllCheckBox() {
        if (selectAllCheckBox == null) return;
        if (enumTypeList.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            return;
        }
        long selectedCount = enumTypeList.stream().filter(po -> po.getSelectedProperty() != null && po.getSelectedProperty().get()).count();
        if (selectedCount == enumTypeList.size()) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }

    private void loadEnumTypeList() {
        EnumDictPO enumDictPO = new EnumDictPO();
        enumDictPO.setGroupName(globalPropes.getGroupName());
        List<EnumGroupPO> enumGroupPOS = enumDictDao.groupByEnumNme(enumDictPO,null);
        enumTypeList.setAll(enumGroupPOS);
        // 监听每个PO的selectedProperty
        for (EnumGroupPO po : enumTypeList) {
            if (po.getSelectedProperty() != null) {
                po.getSelectedProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
            }
        }
        updateSelectAllCheckBox();
    }

    private void loadEnumItems(String enumNme) {
        EnumDictPO enumDictPO = new EnumDictPO();
        enumDictPO.setEnumNme(enumNme);
        List<EnumDictPO> items = enumDictDao.queryForList(enumDictPO);
        enumItems.setAll(items);
    }
}
