package com.murong.ecp.tools.fx.controller;

import com.google.common.base.CaseFormat;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.factory.TableEntityFactory;
import com.murong.ecp.tools.fx.domain.service.database.DataBaseHandler;
import com.murong.ecp.tools.fx.domain.service.database.TableEntityService;
import com.murong.ecp.tools.fx.domain.service.interfaces.JavaCodeService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.CommonClassDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.BizDictEditingCell;
import com.murong.ecp.tools.fx.infrastructure.view.FilteredEditingCell;
import com.murong.ecp.tools.fx.infrastructure.view.ReminderDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.ToggleSwitch;
import com.murong.ecp.tools.fx.infrastructure.view.valueobj.ColumnSchemaVo;
import com.murong.ecp.tools.fx.infrastructure.view.valueobj.IndexSchemaVo;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PaneTableController implements Initializable {
    
    /**
     * 公共字段选项类
     */
    public static class CommonFieldOption {
        private String className;
        private String classPath;
        
        public CommonFieldOption(String className, String classPath) {
            this.className = className;
            this.classPath = classPath;
        }
        
        public String getClassName() {
            return className;
        }
        
        public String getClassPath() {
            return classPath;
        }
        
        @Override
        public String toString() {
            return className;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CommonFieldOption that = (CommonFieldOption) obj;
            return classPath != null ? classPath.equals(that.classPath) : that.classPath == null;
        }
        
        @Override
        public int hashCode() {
            return classPath != null ? classPath.hashCode() : 0;
        }
    }
    
    private static String curTabNm="";
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    @Autowired
    GlobalProperties globalProps;
    @Autowired
    TableEntityFactory tableEntityFactory;
    @Autowired
    JavaCodeService javaCodeService;
    @Autowired
    TableEntityService tableEntityService;
    @Autowired
    private BizDictDao bizDictDao;
    @Autowired
    private CommonClassDao commonClassDao;

    @FXML
    private Button saveButton;
    @FXML
    private TextField tableNameField;
    @FXML
    private TextField tableChineseNameField;
    @FXML
    private TextField tableEnglishNameField;

    // 新增的控件
    @FXML
    private ComboBox<CommonFieldOption> commonFieldsComboBox;
    @FXML
    private ComboBox<String> tagsComboBox;

    // 隐藏的输入框，用于存储自动创建表结构标识和自动生成代码标识
    @FXML
    private TextField createTabFlgField;
    @FXML
    private TextField generCdFlgField;
    
    // 隐藏字段，用于存储选中的公共字段class_path
    private String selectedCommonFieldPath;

    @FXML
    private TableView<ColumnSchemaVo> columnsTable;
    @FXML
    private TableColumn<ColumnSchemaVo, Boolean> pkColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> columnNameColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> dataTypeColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> lengthColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, Boolean> notNullColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> defaultValueColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> chineseCommentColumn;
    @FXML
    private TableColumn<ColumnSchemaVo, String> englishCommentColumn;
    @FXML
    private TableView<IndexSchemaVo> indexesTable;
    @FXML
    private TableColumn<IndexSchemaVo, Boolean> indexSavColumn;
    @FXML
    private TableColumn<IndexSchemaVo, String> indexTypeColumn;
    @FXML
    private TableColumn<IndexSchemaVo, String> indexNameColumn;
    @FXML
    private TableColumn<IndexSchemaVo, String> indexColumnsColumn;
    @FXML
    private CheckBox selectAllCheckBox;
    @FXML
    private TableColumn<ColumnSchemaVo, Void> deleteColumn;
    @FXML
    private TableView<OrderByVo> orderTable;
    @FXML
    private TableColumn<OrderByVo, String> orderByLabelColumn;
    @FXML
    private TableColumn<OrderByVo, String> orderByFieldColumn;
    @FXML
    private Button backButton;
    @FXML
    private TextField columnSearchField;
    @FXML
    private Button columnSearchButton;
    
    // 搜索相关变量
    private List<Integer> searchResultIndices = new ArrayList<>();
    private int currentSearchIndex = -1;
    
    // 隐藏字段的备用存储，当FXML字段为null时使用
    private String createTabFlgValue = "Y";
    private String generCdFlgValue = "Y";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        System.out.println("[PaneTableController] initialize called");
        System.out.println("[PaneTableController] columnsTable injected? " + (columnsTable != null));
        //初始化表菜单
        tableNameField.setEditable(true);
        
        // 初始化隐藏字段的默认值
        if (createTabFlgField != null) {
            createTabFlgField.setText("Y"); // 默认自动创建表结构
        }
        if (generCdFlgField != null) {
            generCdFlgField.setText("Y"); // 默认自动生成代码
        }
        // 同时初始化备用存储
        this.createTabFlgValue = "Y";
        this.generCdFlgValue = "Y";
        
        // loadTableMenu(); // 右侧菜单栏已删除，不再需要加载表菜单
        
        setupColumnsTable();
        setupIndexesTable();
        setupOrderTable();
        
        // 初始化新增的下拉框
        setupCommonFieldsComboBox();
        setupTagsComboBox();
        
        // 添加搜索框回车键监听和文本变化监听
        if (columnSearchField != null) {
            columnSearchField.setOnAction(event -> searchColumn());
            // 监听搜索框文本变化，重置搜索状态
            columnSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!StringUtils.equals(oldValue, newValue)) {
                    // 搜索内容变化时，重置搜索状态
                    searchResultIndices.clear();
                    currentSearchIndex = -1;
                    // 清除高亮
                    clearColumnHighlight();
                }
            });
        }
        
        if (backButton != null) {
            backButton.setOnAction(event -> handleBack());
        }
    }

    private void setupColumnsTable() {
        columnsTable.setEditable(true);
        // 禁用表格排序功能，但不影响选中行为
        columnsTable.setSortPolicy(tableView -> false);

        if (selectAllCheckBox == null) {
            selectAllCheckBox = new CheckBox();
            selectAllCheckBox.setTooltip(new Tooltip("全选/全不选"));
        }
        pkColumn.setGraphic(selectAllCheckBox);

        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (selectAllCheckBox.isIndeterminate()) return;
            for (ColumnSchemaVo vo : columnsTable.getItems()) {
                vo.setNeedSave(newVal);
            }
            columnsTable.refresh();
        });

        pkColumn.setCellValueFactory(cellData -> cellData.getValue().needSaveProperty());
        pkColumn.setCellFactory(CheckBoxTableCell.forTableColumn(p -> columnsTable.getItems().get(p).needSaveProperty()));
        // 禁用排序
        pkColumn.setSortable(false);

        StringConverter<String> converter = new DefaultStringConverter();

        columnNameColumn.setCellValueFactory(cellData -> cellData.getValue().columnNameProperty());
        columnNameColumn.setCellFactory(col -> new BizDictEditingCell<>(converter, bizDictDao, bizDict -> {
            // 当用户选择业务字段时，自动填充相关字段
            ColumnSchemaVo vo = columnsTable.getItems().get(columnNameColumn.getTableView().getSelectionModel().getSelectedIndex());
            if (vo != null) {
                vo.setColumnName(bizDict.getNameSnake());
                vo.setDataType(bizDict.getDbTyp());
                vo.setLength(String.valueOf(bizDict.getLength()));
                vo.setChineseComment(bizDict.getCommentCn());
                vo.setEnglishComment(bizDict.getCommentEn());
                vo.setNotNull("Y".equals(bizDict.getNotNull()));
                vo.setDefaultValue(bizDict.getDefaultValue());
                columnsTable.refresh();
                
                // 检查是否需要自动新增一行
                Platform.runLater(() -> {
                    if (columnsTable.getItems().isEmpty()) {
                        return;
                    }
                    int lastIndex = columnsTable.getItems().size() - 1;
                    if (lastIndex >= 0 && columnsTable.getItems().get(lastIndex) == vo) {
                        // 如果当前行是最后一行，且字段名不为空，则添加新行
                        if (StringUtils.isNotBlank(bizDict.getNameSnake())) {
                            columnsTable.getItems().add(new ColumnSchemaVo());
                            // 更新全选复选框状态
                            updateSelectAllCheckBox();
                        }
                    }
                });
            }
        }));
        columnNameColumn.setOnEditCommit(event -> {
            ColumnSchemaVo vo = event.getRowValue();
            vo.setColumnName(event.getNewValue());
            Platform.runLater(() -> {
                if (columnsTable.getItems().isEmpty()) {
                    return;
                }
                int lastIndex = columnsTable.getItems().size() - 1;
                if (lastIndex >= 0 && columnsTable.getItems().get(lastIndex) == vo) {
                    // 只有当字段名不为空（不是空格、空字符串等）时才添加新行
                    if (StringUtils.isNotBlank(event.getNewValue())) {
                        columnsTable.getItems().add(new ColumnSchemaVo());
                    }
                }
            });
        });
        // 禁用排序
        columnNameColumn.setSortable(false);

        dataTypeColumn.setCellValueFactory(cellData -> cellData.getValue().dataTypeProperty());
        dataTypeColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        dataTypeColumn.setOnEditCommit(event -> event.getRowValue().setDataType(event.getNewValue()));
        // 禁用排序
        dataTypeColumn.setSortable(false);

        lengthColumn.setCellValueFactory(cellData -> cellData.getValue().lengthProperty());
        lengthColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        lengthColumn.setOnEditCommit(event -> event.getRowValue().setLength(event.getNewValue()));
        // 禁用排序
        lengthColumn.setSortable(false);

        notNullColumn.setCellValueFactory(cellData -> cellData.getValue().notNullProperty());
        notNullColumn.setCellFactory(createToggleSwitchCellFactory());
        // 禁用排序
        notNullColumn.setSortable(false);

        defaultValueColumn.setCellValueFactory(cellData -> cellData.getValue().defaultValueProperty());
        defaultValueColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        defaultValueColumn.setOnEditCommit(event -> event.getRowValue().setDefaultValue(event.getNewValue()));
        // 禁用排序
        defaultValueColumn.setSortable(false);

        chineseCommentColumn.setCellValueFactory(cellData -> cellData.getValue().chineseCommentProperty());
        chineseCommentColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        chineseCommentColumn.setOnEditCommit(event -> event.getRowValue().setChineseComment(event.getNewValue()));
        // 禁用排序
        chineseCommentColumn.setSortable(false);

        englishCommentColumn.setCellValueFactory(cellData -> cellData.getValue().englishCommentProperty());
        englishCommentColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        englishCommentColumn.setOnEditCommit(event -> event.getRowValue().setEnglishComment(event.getNewValue()));
        // 禁用排序
        englishCommentColumn.setSortable(false);

        columnsTable.setRowFactory(tv -> {
            TableRow<ColumnSchemaVo> row = new TableRow<>();

            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && StringUtils.isNotBlank(row.getItem().getColumnName())) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    ObservableList<ColumnSchemaVo> items = columnsTable.getItems();
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    int dropIndex;

                    if (row.isEmpty()) {
                        dropIndex = items.size() - 1;
                    } else {
                        dropIndex = row.getIndex();
                    }

                    ColumnSchemaVo draggedItem = items.remove(draggedIndex);

                    if (draggedIndex < dropIndex) {
                        dropIndex--;
                    }

                    items.add(dropIndex, draggedItem);

                    success = true;
                    columnsTable.getSelectionModel().select(dropIndex);
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });

        columnsTable.getItems().forEach(vo -> {
            if (vo != null) {
                vo.needSaveProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
            }
        });
        columnsTable.getItems().addListener((javafx.collections.ListChangeListener<ColumnSchemaVo>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ColumnSchemaVo vo : change.getAddedSubList()) {
                        if (vo != null) {
                            vo.needSaveProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
                        }
                    }
                }
            }
            updateSelectAllCheckBox();
        });
        updateSelectAllCheckBox();
        
        columnsTable.setOnDragOver(event -> {
            if (event.getGestureSource() != columnsTable && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        columnsTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                TableEntity tableEntity = javaCodeService.javaEntityConvTable(file.getAbsolutePath());
                DataBaseHandler dataBaseHandler = globalProps.getInputDataBaseHandler();
                dataBaseHandler.adjustTableEntity(tableEntity);

                List<RxField> rxFields = tableEntity.getRxFields();
                final Set<String> existingColumnNames = columnsTable.getItems().stream()
                        .map(ColumnSchemaVo::getColumnName)
                        .filter(name -> name != null && !name.isEmpty())
                        .collect(Collectors.toSet());
                if(columnsTable.getItems()!=null && columnsTable.getItems().size()>0){
                    columnsTable.getItems().removeLast();
                }
                for (RxField rxField : rxFields) {
                    if (!existingColumnNames.contains(rxField.getNameSnake())) {
                        ColumnSchemaVo column = createColumn(false,
                                rxField.getNameSnake(), rxField.getDbTyp(),
                                String.valueOf(rxField.getLength()), true, rxField.getDefaultValue(),
                                rxField.getCommentCn(), rxField.getCommentEn());
                        if(column!=null){
                            columnsTable.getItems().add(column);
                            column.needSaveProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
                        }
                    }
                }
                columnsTable.getItems().add(new ColumnSchemaVo());
                updateSelectAllCheckBox();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        if (deleteColumn != null) {
            deleteColumn.setCellFactory(col -> new TableCell<ColumnSchemaVo, Void>() {
                private final Button deleteButton = new Button();
                {
                    javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                    trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                    trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                    trashIcon.setScaleX(0.53);
                    trashIcon.setScaleY(0.53);
                    deleteButton.setGraphic(trashIcon);
                    deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                    deleteButton.setOnAction(event -> {
                        ColumnSchemaVo vo = getTableView().getItems().get(getIndex());
                        if (vo != null && org.apache.commons.lang3.StringUtils.isNotBlank(vo.getColumnName())) {
                            getTableView().getItems().remove(vo);
                            updateSelectAllCheckBox();
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableView().getItems().get(getIndex()) == null
                            || org.apache.commons.lang3.StringUtils.isBlank(getTableView().getItems().get(getIndex()).getColumnName())) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteButton);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            });
        }
    }

    private void updateSelectAllCheckBox() {
        if (selectAllCheckBox == null) return;
        ObservableList<ColumnSchemaVo> items = columnsTable.getItems();
        if (items == null || items.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            return;
        }
        long selectedCount = items.stream()
                .filter(Objects::nonNull)
                .filter(ColumnSchemaVo::isNeedSave)
                .count();
        long totalCount = items.stream().filter(Objects::nonNull).count();
        if (totalCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            return;
        }
        if (selectedCount == totalCount) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }
    /**
     * 创建ToggleSwitch单元格工厂
     */
    private javafx.util.Callback<TableColumn<ColumnSchemaVo, Boolean>, TableCell<ColumnSchemaVo, Boolean>> createToggleSwitchCellFactory() {
        return column -> new TableCell<ColumnSchemaVo, Boolean>() {
            private final ToggleSwitch toggleSwitch = new ToggleSwitch();
            
            {
                // 监听ToggleSwitch状态变化
                toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    ColumnSchemaVo columnVo = getTableView().getItems().get(getIndex());
                    if (columnVo != null) {
                        columnVo.setNotNull(newVal);
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ColumnSchemaVo columnVo = getTableView().getItems().get(getIndex());
                    if (columnVo != null) {
                        // 临时禁用监听器，避免初始化时触发
                        boolean wasSelected = toggleSwitch.isSelected();
                        boolean shouldBeSelected = columnVo.isNotNull();
                        
                        // 只有当值真的不同时才设置，避免触发监听器
                        if (wasSelected != shouldBeSelected) {
                            toggleSwitch.setSelected(shouldBeSelected);
                        }
                        setGraphic(toggleSwitch);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
    }



    private void setupIndexesTable() {
        indexesTable.setEditable(true);
        // 禁用表格排序功能
        indexesTable.setSortPolicy(null);
        
        StringConverter<String> converter = new DefaultStringConverter();

        indexSavColumn.setCellValueFactory(cellData -> cellData.getValue().needSaveProperty());
        indexSavColumn.setCellFactory(CheckBoxTableCell.forTableColumn(p -> indexesTable.getItems().get(p).needSaveProperty()));
        // 禁用排序
        indexSavColumn.setSortable(false);

        indexTypeColumn.setCellValueFactory(cellData -> cellData.getValue().indexTypeProperty());
        ObservableList<String> indexTypes = FXCollections.observableArrayList("primary", "unique", "index");
        indexTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(indexTypes));
        indexTypeColumn.setOnEditCommit(event -> {
            IndexSchemaVo vo = event.getRowValue();
            vo.setIndexType(event.getNewValue());
            
            // 当选择主键类型时，自动设置索引名为"pk_"+表名
            if ("primary".equalsIgnoreCase(event.getNewValue())) {
                String tableName = tableNameField.getText();
                if (StringUtils.isNotBlank(tableName)) {
                    // 如果表名以"t_"开头，则去掉"t_"前缀
                    String processedTableName = tableName;
                    if (tableName.startsWith("t_")) {
                        processedTableName = tableName.substring(2);
                    }
                    vo.setIndexName("pk_" + processedTableName);
                    // 刷新表格以显示更新后的索引名
                    indexesTable.refresh();
                }
            }
        });
        // 禁用排序
        indexTypeColumn.setSortable(false);

        indexNameColumn.setCellValueFactory(cellData -> cellData.getValue().indexNameProperty());
        indexNameColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        indexNameColumn.setOnEditCommit(event -> {
            IndexSchemaVo vo = event.getRowValue();
            vo.setIndexName(event.getNewValue());
            Platform.runLater(() -> {
                if (indexesTable.getItems().isEmpty()) {
                    return;
                }
                int lastIndex = indexesTable.getItems().size() - 1;
                if (lastIndex >= 0 && indexesTable.getItems().get(lastIndex) == vo) {
                    // 只有当索引名不为空（不是空格、空字符串等）时才添加新行
                    if (StringUtils.isNotBlank(event.getNewValue())) {
                        indexesTable.getItems().add(new IndexSchemaVo());
                    }
                }
            });
        });
        // 禁用排序
        indexNameColumn.setSortable(false);

        indexColumnsColumn.setCellValueFactory(cellData -> cellData.getValue().columnsProperty());
        indexColumnsColumn.setCellFactory(col -> new FilteredEditingCell<>(converter));
        indexColumnsColumn.setOnEditCommit(event -> {
            IndexSchemaVo vo = event.getRowValue();
            vo.setColumns(event.getNewValue());
            Platform.runLater(() -> {
                if (indexesTable.getItems().isEmpty()) {
                    return;
                }
                int lastIndex = indexesTable.getItems().size() - 1;
                if (lastIndex >= 0 && indexesTable.getItems().get(lastIndex) == vo) {
                    // 只有当索引列不为空（不是空格、空字符串等）时才添加新行
                    if (StringUtils.isNotBlank(event.getNewValue())) {
                        indexesTable.getItems().add(new IndexSchemaVo());
                    }
                }
            });
        });
        // 禁用排序
        indexColumnsColumn.setSortable(false);
    }





    @FXML
    private void save() {

        String newTableName = tableNameField.getText();
        if (StringUtils.isBlank(newTableName)) {
            ViewUtils.alertForSucess("Table name cannot be empty.");
            return;
        }

        // 获取隐藏输入框的值并输出到控制台
        String createTabFlgValue = getCreateTabFlg();
        String generCdFlgValue = getGenerCdFlg();
        System.out.println("[PaneTableController] 保存前隐藏输入框值:");
        System.out.println("[PaneTableController] create_tab_flg = " + (createTabFlgField != null ? createTabFlgField.getText() : createTabFlgValue));
        System.out.println("[PaneTableController] gener_cd_flg = " + (generCdFlgField != null ? generCdFlgField.getText() : generCdFlgValue));

        // 根据隐藏输入框的值确定复选框默认状态
        boolean createTabDefault = "Y".equalsIgnoreCase(createTabFlgValue);
        boolean generCdDefault = "Y".equalsIgnoreCase(generCdFlgValue);

        // 创建复选框选项列表
        List<ReminderDialogUtil.CheckBoxOption> checkBoxOptions = new ArrayList<>();
        checkBoxOptions.add(new ReminderDialogUtil.CheckBoxOption("自动创建表结构", createTabDefault, "createTab"));
        checkBoxOptions.add(new ReminderDialogUtil.CheckBoxOption("自动生成代码", generCdDefault, "generCd"));

        // 使用ReminderDialogUtil显示确认对话框
        ReminderDialogUtil.showConfirmDialogWithResult(
            "确认保存", 
            "确认保存表结构", 
            "确定要保存表 '" + newTableName + "' 的结构吗？", 
            checkBoxOptions,
            dialogResult -> {
                if (dialogResult.isConfirmed()) {
                    // 根据复选框状态更新隐藏输入框的值
                    String newCreateTabFlg = dialogResult.getCheckBoxStateByAction("createTab") ? "Y" : "N";
                    String newGenerCdFlg = dialogResult.getCheckBoxStateByAction("generCd") ? "Y" : "N";
                    
                    if (createTabFlgField != null) {
                        createTabFlgField.setText(newCreateTabFlg);
                    }
                    if (generCdFlgField != null) {
                        generCdFlgField.setText(newGenerCdFlg);
                    }
                    
                    // 输出更新后的值到控制台
                    System.out.println("[PaneTableController] 用户确认后隐藏输入框值:");
                    System.out.println("[PaneTableController] create_tab_flg = " + newCreateTabFlg);
                    System.out.println("[PaneTableController] gener_cd_flg = " + newGenerCdFlg);


                    // 执行保存逻辑
                    TableEntity tableEntity = assemblyTableEntity(newTableName);
                    tableEntity.setCreateTabFlg(newCreateTabFlg);
                    tableEntity.setGenerCdFlg(newGenerCdFlg);
                    CrResult crResult = tableEntityService.saveTableEntity(tableEntity);
                    if(crResult!=null && crResult.isSucess()){
                        ViewUtils.alertForSucess("保存成功！");
                    }else {
                        ViewUtils.alertForSucess(crResult.getMsgInf());
                    }
                } else {
                    System.out.println("[PaneTableController] 用户取消了保存操作");
                }
                // 取消操作，不做任何处理
            }
        );
    }

    private TableEntity assemblyTableEntity(String newTableName) {
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableNameSnake(newTableName);
        tableEntity.setTableCommentCn(tableChineseNameField.getText());
        tableEntity.setTableCommentEn(tableEnglishNameField.getText());
        tableEntity.setGroupName(globalProps.getGroupName());
        tableEntity.setProjectName(globalProps.getProjectName());
        tableEntity.setAppName(globalProps.getAppName());
        
        // 设置选中的公共字段class_path
        if (selectedCommonFieldPath != null && !selectedCommonFieldPath.isEmpty()) {
            tableEntity.setParentClass(selectedCommonFieldPath);
        }else {
            tableEntity.setParentClass("");
        }
        
        // 设置自动创建表结构标识和自动生成代码标识
        tableEntity.setCreateTabFlg(getCreateTabFlg());
        tableEntity.setGenerCdFlg(getGenerCdFlg());

        List<RxField> rxFields = columnsTable.getItems().stream()
                .filter(Objects::nonNull)
                .filter(column -> column.isNeedSave() && StringUtils.isNotBlank(column.getColumnName()))
                .map(column -> {
                    RxField rxField = new RxField();
                    rxField.setNameSnake(column.getColumnName());
                    if (StringUtils.isNotBlank(column.getColumnName())) {
                        rxField.setNameCamel(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, column.getColumnName()));
                    }
                    rxField.setDbTyp(column.getDataType());
                    
                    // 对于text类型，不设置长度，避免对比差异
                    String upperDbType = column.getDataType().toUpperCase();
                    if ("TEXT".equals(upperDbType) || "LONGTEXT".equals(upperDbType) || "MEDIUMTEXT".equals(upperDbType) || "TINYTEXT".equals(upperDbType)) {
                        // text类型不设置长度
                        rxField.setLength(null);
                    } else if (StringUtils.isNotBlank(column.getLength())) {
                        try {
                            // 修复：去除逗号后再转int，防止1,024等格式导致异常
                            String cleanLength = column.getLength().replaceAll(",", "");
                            rxField.setLength(Integer.parseInt(cleanLength));
                        } catch (NumberFormatException e) {
                            rxField.setLength(0);
                        }
                    }
                    rxField.setCommentCn(column.getChineseComment());
                    rxField.setCommentEn(column.getEnglishComment());
                    rxField.setNotNull(column.isNotNull());
                    rxField.setDefaultValue(column.getDefaultValue());
                    return rxField;
                }).collect(Collectors.toList());
        tableEntity.setRxFields(rxFields);

        List<TableEntity.Index> indexes = new ArrayList<>();
        TableEntity.Index primaryKey = null;
        for (IndexSchemaVo indexVo : indexesTable.getItems()) {
            if (indexVo.isNeedSave() && StringUtils.isNotBlank(indexVo.getIndexName())) {
                TableEntity.Index index = new TableEntity.Index();
                index.setType(indexVo.getIndexType());
                index.setName(indexVo.getIndexName());
                if (StringUtils.isNotBlank(indexVo.getColumns())) {
                    List<String> camelCaseFields = Arrays.stream(indexVo.getColumns().split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    index.setFields(camelCaseFields);
                }

                if ("primKey".equalsIgnoreCase(indexVo.getIndexType()) || "primary".equalsIgnoreCase(indexVo.getIndexType())) {
                    primaryKey = index;
                } else {
                    indexes.add(index);
                }
            }
        }
        tableEntity.setPrimaryKey(primaryKey);
        tableEntity.setIndexes(indexes);

        if (orderTable != null && orderTable.getItems() != null && !orderTable.getItems().isEmpty()) {
            String orderField = orderTable.getItems().get(0).getField();
            tableEntity.setDefOrderBy(orderField);
        }

        try {
            DataBaseHandler dataBaseHandler = globalProps.getInputDataBaseHandler();
            dataBaseHandler.adjustTableEntity(tableEntity);
        }catch (Exception e){e.printStackTrace();}
        return tableEntity;
    }



    // private void loadTableMenu() {
    //     List<String> tableList = tableEntityFactory.getTableList();
    //     ObservableList<String> tables = FXCollections.observableArrayList(tableList);
    //     FilteredList<String> filteredTables = new FilteredList<>(tables, p -> true);
    //     // setupTableListView(filteredTables); // 右侧菜单栏已删除，不再需要加载表菜单
    // }

    private void loadSampleData(TableEntity tableEntity) {
        System.out.println("[PaneTableController] loadSampleData called");
        System.out.println("[PaneTableController] columnsTable is " + (columnsTable == null ? "null" : "not null"));
        if (columnsTable == null) {
            System.err.println("[PaneTableController] columnsTable is null! FXML未正确注入，或页面未初始化完成。");
            return;
        }
        tableNameField.setText(tableEntity.getTableNameSnake());
        tableChineseNameField.setText(tableEntity.getTableCommentCn());
        tableEnglishNameField.setText(tableEntity.getTableCommentEn());
        
        // 设置自动创建表结构标识和自动生成代码标识
        // 只有当字段不为空时才设置，避免覆盖外部传入的值
        if (createTabFlgField != null && tableEntity.getCreateTabFlg() != null && !tableEntity.getCreateTabFlg().isEmpty()) {
            createTabFlgField.setText(tableEntity.getCreateTabFlg());
            this.createTabFlgValue = tableEntity.getCreateTabFlg();
        }
        if (generCdFlgField != null && tableEntity.getGenerCdFlg() != null && !tableEntity.getGenerCdFlg().isEmpty()) {
            generCdFlgField.setText(tableEntity.getGenerCdFlg());
            this.generCdFlgValue = tableEntity.getGenerCdFlg();
        }

        ObservableList<ColumnSchemaVo> columns = FXCollections.observableArrayList();
        List<RxField> rxFields = tableEntity.getRxFields();
        for (RxField rxField : rxFields) {
            columns.add(createColumn(true, rxField.getNameSnake(), rxField.getDbTyp(),
                    String.valueOf(rxField.getLength()), true, rxField.getDefaultValue(),
                    rxField.getCommentCn(), rxField.getCommentEn()));
        }
        columns.add(new ColumnSchemaVo());
        columnsTable.setItems(columns);

        ObservableList<IndexSchemaVo> indexes = FXCollections.observableArrayList();
        TableEntity.Index primaryKey = tableEntity.getPrimaryKey();
        if(primaryKey != null) {
            IndexSchemaVo pkIndex = new IndexSchemaVo();
            pkIndex.setNeedSave(true);
            pkIndex.setIndexType(primaryKey.getType());
            pkIndex.setIndexName(primaryKey.getName());
            StringBuilder builder = new StringBuilder();
            List<String> indexlist = primaryKey.getFields();
            for (String idx:indexlist) {
                builder.append(MrStringUtils.toUnderline(idx)).append(",");
            }
            if(builder.length() > 0) {
                String indexcol = builder.toString().substring(0, builder.length() - 1);
                pkIndex.setColumns(indexcol);
                indexes.add(pkIndex);
            }
        }

        List<TableEntity.Index> indexesList = tableEntity.getIndexes();
        if(!CollectionUtils.isEmpty(indexesList)) {
            for (TableEntity.Index index : indexesList) {
                IndexSchemaVo normalIndex = new IndexSchemaVo();
                normalIndex.setNeedSave(true);
                normalIndex.setIndexType(index.getType());
                normalIndex.setIndexName(index.getName());
                StringBuilder builder = new StringBuilder();
                List<String> indexlist = index.getFields();
                for (String idx:indexlist) {
                    builder.append(idx).append(",");
                }
                if(builder.length() > 0) {
                    String indexcol = builder.toString().substring(0, builder.length() - 1);
                    normalIndex.setColumns(indexcol);
                    indexes.add(normalIndex);
                }
            }
        }


        indexes.add(new IndexSchemaVo());
        indexesTable.setItems(indexes);

        columns.forEach(vo -> {
            if (vo != null) {
                vo.needSaveProperty().addListener((o, ov, nv) -> updateSelectAllCheckBox());
            }
        });
        updateSelectAllCheckBox();
    }

    private ColumnSchemaVo createColumn(boolean needSave, String name, String type, String length,
                                        boolean notNull, String def, String chineseComment, String englishComment) {
        ColumnSchemaVo vo = new ColumnSchemaVo();
        vo.setNeedSave(needSave);
        vo.setColumnName(name);
        vo.setDataType(StringUtils.defaultString(type));
        vo.setLength(StringUtils.defaultString(length));
        vo.setNotNull(notNull);
        vo.setDefaultValue(def);
        vo.setChineseComment(chineseComment);
        vo.setEnglishComment(englishComment);
        return vo;
    }



    private void setupOrderTable() {
        // 禁用表格排序功能
        orderTable.setSortPolicy(null);
        
        orderByLabelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        orderByLabelColumn.setEditable(false);
        // 设置参数名列居中对齐
        orderByLabelColumn.setCellFactory(col -> new TableCell<OrderByVo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        // 禁用排序
        orderByLabelColumn.setSortable(false);

        orderByFieldColumn.setCellValueFactory(cellData -> cellData.getValue().fieldProperty());
        orderByFieldColumn.setCellFactory(col -> new TextFieldTableCell<OrderByVo, String>() {
            @Override
            public void startEdit() {
                if (getTableRow() != null && getTableRow().getItem() != null && getTableRow().getItem().isEditable()) {
                    super.startEdit();
                    TextField tf = (TextField) getGraphic();
                    if (tf != null) {
                        tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
                            if (!newVal && isEditing()) {
                                commitEdit(tf.getText());
                            }
                        });
                    }
                }
            }
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    setEditable(getTableRow().getItem().isEditable());
                }
            }
        });
        orderByFieldColumn.setOnEditCommit(event -> {
            if (event.getRowValue().isEditable()) {
                event.getRowValue().setField(event.getNewValue());
            }
        });
        // 禁用排序
        orderByFieldColumn.setSortable(false);

        ObservableList<OrderByVo> orderList = FXCollections.observableArrayList();
        orderList.add(new OrderByVo(true, "默认排序"));
        orderList.add(new OrderByVo(false, ""));
        orderTable.setItems(orderList);
        orderTable.setEditable(true);
    }

    public static class OrderByVo {
        private SimpleStringProperty label;
        private final SimpleStringProperty field = new SimpleStringProperty("");
        private final boolean editable;

        public OrderByVo(boolean editable, String name) {
            label = new SimpleStringProperty(name);
            this.editable = editable;
        }
        public String getLabel() { return label.get(); }
        public SimpleStringProperty labelProperty() { return label; }

        public String getField() { return field.get(); }
        public void setField(String value) { field.set(value); }
        public SimpleStringProperty fieldProperty() { return field; }
        public boolean isEditable() { return editable; }
    }

    // 新增：接收表名并加载表结构
    public void setSelectedTable(String tableName) {
        System.out.println("[PaneTableController] setSelectedTable called, tableName=" + tableName);
        if (tableName == null || tableName.isEmpty()) return;
        this.curTabNm = tableName;
        TableEntity tableEntity = tableEntityFactory.getTableEntity(tableName);
        if (tableEntity != null) {
            loadSampleData(tableEntity);
        } else {
            // 弹窗提示未找到表结构
            Platform.runLater(() -> {
                ViewUtils.alertForFail("未找到表结构: " + tableName);
            });
        }
    }
    
    // 新增：接收表名并加载表结构，同时设置标识字段
    public void setSelectedTableWithFlags(String tableName, String createTabFlg, String generCdFlg) {
        System.out.println("[PaneTableController] setSelectedTableWithFlags called, tableName=" + tableName + ", createTabFlg=" + createTabFlg + ", generCdFlg=" + generCdFlg);
        if (tableName == null || tableName.isEmpty()) return;
        this.curTabNm = tableName;
        TableEntity tableEntity = tableEntityFactory.getTableEntity(tableName);
        if (tableEntity != null) {
            loadSampleData(tableEntity);
            // 在加载完表结构后，设置标识字段的值
            // 如果传入的值为空，则使用默认值 "Y"
            setCreateTabFlg(createTabFlg != null && !createTabFlg.isEmpty() ? createTabFlg : "Y");
            setGenerCdFlg(generCdFlg != null && !generCdFlg.isEmpty() ? generCdFlg : "Y");
        } else {
            // 弹窗提示未找到表结构
            Platform.runLater(() -> {
                ViewUtils.alertForFail("未找到表结构: " + tableName);
            });
        }
    }
    
    // 新增：接收表名并加载表结构，同时设置标识字段和公共字段
    public void setSelectedTableWithFlagsAndParentClass(String tableName, String createTabFlg, String generCdFlg, String parentClass) {
        System.out.println("[PaneTableController] setSelectedTableWithFlagsAndParentClass called, tableName=" + tableName + ", createTabFlg=" + createTabFlg + ", generCdFlg=" + generCdFlg + ", parentClass=" + parentClass);
        if (tableName == null || tableName.isEmpty()) return;
        this.curTabNm = tableName;
        TableEntity tableEntity = tableEntityFactory.getTableEntity(tableName);
        if (tableEntity != null) {
            loadSampleData(tableEntity);
            // 在加载完表结构后，设置标识字段的值
            // 如果传入的值为空，则使用默认值 "Y"
            setCreateTabFlg(createTabFlg != null && !createTabFlg.isEmpty() ? createTabFlg : "Y");
            setGenerCdFlg(generCdFlg != null && !generCdFlg.isEmpty() ? generCdFlg : "Y");
            // 设置公共字段下拉框的选中值
            setSelectedCommonField(parentClass);
        } else {
            // 弹窗提示未找到表结构
            Platform.runLater(() -> {
                ViewUtils.alertForFail("未找到表结构: " + tableName);
            });
        }
    }
    
    /**
     * 设置自动创建表结构标识
     * @param createTabFlg 标识值（Y/N）
     */
    public void setCreateTabFlg(String createTabFlg) {
        if (createTabFlgField != null) {
            createTabFlgField.setText(createTabFlg);
        }
        // 同时更新备用存储
        this.createTabFlgValue = createTabFlg;
    }
    
    /**
     * 设置自动生成代码标识
     * @param generCdFlg 标识值（Y/N）
     */
    public void setGenerCdFlg(String generCdFlg) {
        if (generCdFlgField != null) {
            generCdFlgField.setText(generCdFlg);
        }
        // 同时更新备用存储
        this.generCdFlgValue = generCdFlg;
    }
    
    /**
     * 获取自动创建表结构标识
     * @return 标识值
     */
    public String getCreateTabFlg() {
        return createTabFlgField != null ? createTabFlgField.getText() : createTabFlgValue;
    }
    
    /**
     * 获取自动生成代码标识
     * @return 标识值
     */
    public String getGenerCdFlg() {
        return generCdFlgField != null ? generCdFlgField.getText() : generCdFlgValue;
    }
    
    /**
     * 设置公共字段下拉框的选中值
     * @param parentClass 公共字段的class_path
     */
    public void setSelectedCommonField(String parentClass) {
        // 等待下拉框初始化完成后再设置值
        Platform.runLater(() -> {
            if (commonFieldsComboBox != null && commonFieldsComboBox.getItems() != null) {
                if (parentClass == null || parentClass.isEmpty()) {
                    // 如果没有公共字段，选择"请选择公共字段"选项
                    for (CommonFieldOption option : commonFieldsComboBox.getItems()) {
                        if (option.getClassPath().isEmpty()) {
                            commonFieldsComboBox.setValue(option);
                            selectedCommonFieldPath = null;
                            System.out.println("设置公共字段下拉框为默认选项");
                            break;
                        }
                    }
                } else {
                    // 在下拉框的选项中查找匹配的class_path
                    for (CommonFieldOption option : commonFieldsComboBox.getItems()) {
                        if (parentClass.equals(option.getClassPath())) {
                            commonFieldsComboBox.setValue(option);
                            selectedCommonFieldPath = option.getClassPath();
                            System.out.println("设置公共字段下拉框选中值: " + option.getClassName() + " (" + option.getClassPath() + ")");
                            break;
                        }
                    }
                }
            }
        });
    }

    @FXML
    private void searchColumn() {
        String searchText = columnSearchField.getText();
        if (StringUtils.isBlank(searchText)) {
            ViewUtils.alertForFail("请输入搜索内容！");
            return;
        }

        // 如果是新的搜索，重新收集所有匹配项
        if (currentSearchIndex == -1 || searchResultIndices.isEmpty()) {
            searchResultIndices.clear();
            currentSearchIndex = -1;
            
            // 在字段表格中搜索所有匹配项
            ObservableList<ColumnSchemaVo> items = columnsTable.getItems();
            for (int i = 0; i < items.size(); i++) {
                ColumnSchemaVo item = items.get(i);
                if (item != null && (
                    (item.getColumnName() != null && item.getColumnName().toLowerCase().contains(searchText.toLowerCase())) ||
                    (item.getChineseComment() != null && item.getChineseComment().toLowerCase().contains(searchText.toLowerCase())) ||
                    (item.getEnglishComment() != null && item.getEnglishComment().toLowerCase().contains(searchText.toLowerCase()))
                )) {
                    searchResultIndices.add(i);
                }
            }
        }

        if (searchResultIndices.isEmpty()) {
            ViewUtils.alertForFail("未找到匹配的字段！");
            return;
        }

        // 循环到下一个匹配项
        currentSearchIndex = (currentSearchIndex + 1) % searchResultIndices.size();
        int foundIndex = searchResultIndices.get(currentSearchIndex);
        ColumnSchemaVo foundItem = columnsTable.getItems().get(foundIndex);

        // 选中并滚动到找到的项目
        columnsTable.getSelectionModel().select(foundIndex);
        columnsTable.scrollTo(foundIndex);
        
        // 高亮显示找到的项目
        highlightFoundColumn(foundIndex);
    }

    /**
     * 高亮显示找到的字段
     */
    private void highlightFoundColumn(int foundIndex) {
        // 清除之前的高亮
        clearColumnHighlight();
        
        // 设置高亮样式，保持原有的拖拽功能
        columnsTable.setRowFactory(tv -> {
            TableRow<ColumnSchemaVo> row = new TableRow<ColumnSchemaVo>() {
                @Override
                protected void updateItem(ColumnSchemaVo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) {
                        setStyle("");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;");
                    } else if (getIndex() == foundIndex) {
                        // 高亮当前选中的匹配项
                        setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: black;");
                    } else if (searchResultIndices.contains(getIndex())) {
                        // 高亮其他匹配项（稍微淡一点的颜色）
                        setStyle("-fx-background-color: #fff3cd; -fx-text-fill: black;");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #f7f7f7;");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            };

            // 保持原有的拖拽功能
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && StringUtils.isNotBlank(row.getItem().getColumnName())) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    ObservableList<ColumnSchemaVo> items = columnsTable.getItems();
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    int dropIndex;

                    if (row.isEmpty()) {
                        dropIndex = items.size() - 1;
                    } else {
                        dropIndex = row.getIndex();
                    }

                    ColumnSchemaVo draggedItem = items.remove(draggedIndex);

                    if (draggedIndex < dropIndex) {
                        dropIndex--;
                    }

                    items.add(dropIndex, draggedItem);

                    success = true;
                    columnsTable.getSelectionModel().select(dropIndex);
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });
    }

    /**
     * 清除字段高亮
     */
    private void clearColumnHighlight() {
        // 重新设置默认的行工厂，保持原有的拖拽功能
        columnsTable.setRowFactory(tv -> {
            TableRow<ColumnSchemaVo> row = new TableRow<ColumnSchemaVo>() {
                @Override
                protected void updateItem(ColumnSchemaVo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) {
                        setStyle("");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;");
                    } else if (searchResultIndices.contains(getIndex())) {
                        // 保持搜索结果的淡黄色高亮
                        setStyle("-fx-background-color: #fff3cd; -fx-text-fill: black;");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #f7f7f7;");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            };

            // 保持原有的拖拽功能
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && StringUtils.isNotBlank(row.getItem().getColumnName())) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    ObservableList<ColumnSchemaVo> items = columnsTable.getItems();
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    int dropIndex;

                    if (row.isEmpty()) {
                        dropIndex = items.size() - 1;
                    } else {
                        dropIndex = row.getIndex();
                    }

                    ColumnSchemaVo draggedItem = items.remove(draggedIndex);

                    if (draggedIndex < dropIndex) {
                        dropIndex--;
                    }

                    items.add(dropIndex, draggedItem);

                    success = true;
                    columnsTable.getSelectionModel().select(dropIndex);
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });
    }

    @FXML
    private void handleBack() {
        MainController mainController = com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder.getBean(MainController.class);
        Tab tableTab = mainController.getTabByKey("tableManager");
        if (tableTab != null) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pane_table_mng.xml"));
                loader.setControllerFactory(clazz -> com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder.getBean(clazz));
                javafx.scene.Node mngPage = loader.load();
                tableTab.setContent(mngPage);
                mainController.getPageContainer().getSelectionModel().select(tableTab);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 初始化公共字段下拉框
     */
    private void setupCommonFieldsComboBox() {
        if (commonFieldsComboBox != null) {
            // 从数据库查询公共字段数据
            ObservableList<CommonFieldOption> commonFields = FXCollections.observableArrayList();
            
            // 添加"请选择公共字段"选项
            commonFields.add(new CommonFieldOption("请选择公共字段", ""));
            
            try {
                CommonClassPO query = new CommonClassPO();
                query.setClassType("TAB");
                query.setAppName("pub");
                query.setGroupName(globalProps.getGroupName());
                List<CommonClassPO> list = commonClassDao.queryForList(query);
                
                for (CommonClassPO po : list) {
                    if (po.getClassName() != null && po.getClassPath() != null) {
                        commonFields.add(new CommonFieldOption(po.getClassName(), po.getClassPath()));
                    }
                }
            } catch (Exception e) {
                // 如果查询失败，使用默认值
                commonFields.addAll(
                    new CommonFieldOption("基础字段", "com.murong.ecp.common.entity.BaseEntity"),
                    new CommonFieldOption("用户字段", "com.murong.ecp.common.entity.UserEntity"),
                    new CommonFieldOption("审计字段", "com.murong.ecp.common.entity.AuditEntity")
                );
                System.err.println("查询公共字段失败: " + e.getMessage());
            }
            
            commonFieldsComboBox.setItems(commonFields);
            commonFieldsComboBox.setPromptText("选择公共字段");
            
            // 添加选择监听器
            commonFieldsComboBox.setOnAction(event -> {
                CommonFieldOption selectedOption = commonFieldsComboBox.getValue();
                if (selectedOption != null) {
                    if (selectedOption.getClassPath().isEmpty()) {
                        // 如果选择的是"请选择公共字段"，清空选择
                        selectedCommonFieldPath = null;
                        System.out.println("清空公共字段选择");
                    } else {
                        // 当选择公共字段时，保存class_path到隐藏字段
                        selectedCommonFieldPath = selectedOption.getClassPath();
                        System.out.println("选择的公共字段: " + selectedOption.getClassName() + ", 路径: " + selectedOption.getClassPath());
                    }
                }
            });
        }
    }
    
    /**
     * 初始化标签下拉框
     */
    private void setupTagsComboBox() {
        if (tagsComboBox != null) {
            ObservableList<String> tags = FXCollections.observableArrayList(
                "用户管理", "订单管理", "商品管理", "支付管理", "库存管理",
                "日志管理", "系统管理", "权限管理", "报表管理", "其他"
            );
            tagsComboBox.setItems(tags);
            tagsComboBox.setPromptText("选择标签");
            
            // 添加选择监听器
            tagsComboBox.setOnAction(event -> {
                String selectedTag = tagsComboBox.getValue();
                if (selectedTag != null && !selectedTag.isEmpty()) {
                    // 当选择标签时，可以在这里添加相应的逻辑
                    System.out.println("选择的标签: " + selectedTag);
                }
            });
        }
    }
}