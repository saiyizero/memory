package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.common.TranslationService;
import com.murong.ecp.tools.fx.domain.service.interfaces.MsgCodeService;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.BizMsgInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.DeleteDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
public class PaneInfoCodeController {
    @Autowired
    private BizMsgInfoRpcService bizMsgInfoRpcService;
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private MsgCodeService msgCodeService;
    @Autowired
    private TranslationService translationService;

    // 搜索字段
    @FXML private TextField searchField;
    @FXML private Button queryButton;
    @FXML private Button addButton;
    @FXML private Label totalCountLabel;

    @FXML private TableView<BizMsgInfoPO> tableView;
    @FXML private TableColumn<BizMsgInfoPO, String> msgClassColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> msgRefColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> msgKeyColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> msgCdColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> msgDescCnColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> msgDescEnColumn;
    @FXML private TableColumn<BizMsgInfoPO, String> statusColumn;
    @FXML private TableColumn<BizMsgInfoPO, Void> actionColumn;

    private ObservableList<BizMsgInfoPO> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalPropes)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        setupTableColumns();
        loadData();
        setupSearchHandlers();
    }

    private void setupTableColumns() {
        // 设置可复制的文本列
        setCopyableCellFactory(msgClassColumn, po -> po.getMsgClass() != null ? po.getMsgClass() : "", true);
        setCopyableCellFactory(msgRefColumn, BizMsgInfoPO::getMsgRef, false);
        // 消息键列特殊处理：复制格式为 "消息分类.消息键"
        setCopyableCellFactoryWithCustomFormat(msgKeyColumn, BizMsgInfoPO::getMsgKey, false);
        setCopyableCellFactory(msgCdColumn, po -> po.getMsgCd() != null ? po.getMsgCd() : "", true);
        setCopyableCellFactory(msgDescCnColumn, BizMsgInfoPO::getMsgDescCn, false);
        setCopyableCellFactory(msgDescEnColumn, BizMsgInfoPO::getMsgDescEn, false);
        statusColumn.setCellValueFactory(cellData -> {
            String statusCode = cellData.getValue() == null ? null : cellData.getValue().getStatus();
            if (StringUtils.isBlank(statusCode)) {
                return new SimpleStringProperty("");
            }
            DataStatusEnum statusEnum = DataStatusEnum.getByCode(statusCode);
            return new SimpleStringProperty(statusEnum != null ? statusEnum.getDesc() : statusCode);
        });
        statusColumn.setStyle("-fx-alignment: center;");
        
        // 设置操作列
        setupActionColumn();
    }

    private void loadData() {
        BizMsgInfoPO bizMsgInfoPO = new BizMsgInfoPO();
        bizMsgInfoPO.setGroupName(globalPropes.getGroupName());
        bizMsgInfoPO.setAppName(globalPropes.getAppName());
        List<BizMsgInfoPO> list = bizMsgInfoRpcService.queryForList(bizMsgInfoPO);
        dataList.setAll(list);
        tableView.setItems(dataList);
        updateTotalCount(list.size());
        
        // 清除自动补全建议缓存，确保数据一致性
        clearSuggestionsCache();
    }

    private void setupSearchHandlers() {
        // 为查询按钮添加事件处理
        if (queryButton != null) {
            queryButton.setOnAction(event -> doQuery());
        }

        // 为新增按钮添加事件处理
        if (addButton != null) {
            addButton.setOnAction(event -> showAddDialog());
        }

        // 为搜索字段添加回车键监听
        if (searchField != null) {
            searchField.setOnAction(event -> doQuery());
        }
    }

    @FXML
    private void doQuery() {
        // 设置查询条件 - 支持组合搜索
        List<BizMsgInfoPO> list = null;
        if (searchField != null && StringUtils.isNotBlank(searchField.getText())) {
            String searchText = searchField.getText().trim();
            list = bizMsgInfoRpcService.queryForSearch(searchText);
        }else {
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(globalPropes.getGroupName());
            queryPO.setProjectName(globalPropes.getProjectName());
            list = bizMsgInfoRpcService.queryForList(queryPO);
        }

        dataList.setAll(list);
        tableView.setItems(dataList);
        updateTotalCount(list.size());
    }

    private void updateTotalCount(int count) {
        if (totalCountLabel != null) {
            totalCountLabel.setText("总数: " + count);
        }
    }
    
    /**
     * 获取当前选中行的消息引用值
     * @return 消息引用值，如果没有选中行则返回null
     */
    public String getSelectedMsgRef() {
        BizMsgInfoPO selectedItem = tableView.getSelectionModel().getSelectedItem();
        return selectedItem != null ? selectedItem.getMsgRef() : null;
    }
    
    /**
     * 根据消息引用值查询数据
     * @param msgRef 消息引用值
     * @return 查询结果列表
     */
    public List<BizMsgInfoPO> queryByMsgRef(String msgRef) {
        if (StringUtils.isNotBlank(msgRef)) {
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setMsgRef(msgRef.trim());
            return bizMsgInfoRpcService.queryForList(queryPO);
        }
        return new ArrayList<>();
    }
    
    /**
     * 获取当前选中行的完整数据对象
     * @return 选中的BizMsgInfoPO对象，如果没有选中行则返回null
     */
    public BizMsgInfoPO getSelectedItem() {
        return tableView.getSelectionModel().getSelectedItem();
    }
    
    /**
     * 设置操作列
     */
    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<BizMsgInfoPO, Void>() {
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            private final HBox hbox = new HBox(8, editBtn, delBtn);
            {
                // 设置编辑按钮样式 - 使用SVG图标
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
                    BizMsgInfoPO po = getTableView().getItems().get(getIndex());
                    openMsgEdit(po);
                });
                
                // 设置删除按钮样式 - 使用SVG图标
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
                    BizMsgInfoPO po = getTableView().getItems().get(getIndex());
                    deleteMsg(po);
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
    }
    
    /**
     * 打开消息编辑界面
     */
    private void openMsgEdit(BizMsgInfoPO po) {
        showEditDialog(po);
    }
    
    /**
     * 删除消息
     */
    private void deleteMsg(BizMsgInfoPO po) {
        // 获取当前窗口的Stage
        Stage ownerStage = (Stage) tableView.getScene().getWindow();
        
        // 构建删除消息
        String deleteMessage = "删除消息 \"" + po.getMsgClass() + "." + po.getMsgKey() + "\"?";
        
        // 创建自定义复选框选项
        List<DeleteDialogUtil.CheckBoxOption> checkBoxOptions = new ArrayList<>();
        checkBoxOptions.add(new DeleteDialogUtil.CheckBoxOption("自动同步代码", true, "synch_code"));
        
        // 显示自定义删除对话框
        DeleteDialogUtil.DeleteResult result = DeleteDialogUtil.showDeleteDialog(
            "删除", 
            deleteMessage, 
            po.getMsgClass() + "." + po.getMsgKey(),
            ownerStage,
            checkBoxOptions
        );
        
        // 处理删除结果
        if (result.isConfirmed()) {
            try {
                // 根据用户选择的选项执行不同的删除逻辑
                if (result.isSecureDelete()) {
                    // 安全删除：检查引用
                    boolean hasReferences = checkMessageReferences(po);
                    if (hasReferences) {
                        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                        warningAlert.setTitle("删除警告");
                        warningAlert.setHeaderText("发现引用");
                        warningAlert.setContentText("该消息被其他地方引用，建议谨慎删除。\n是否继续删除？");
                        warningAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                        
                        Optional<ButtonType> warningResult = warningAlert.showAndWait();
                        if (warningResult.isPresent() && warningResult.get() == ButtonType.NO) {
                            return; // 用户取消删除
                        }
                    }
                }
                
                // 执行删除
                msgCodeService.deleteMessage(po);

                // 如果勾选了自动同步代码，则重新生成Java类代码
                if (result.getCheckBoxStateByAction("synch_code")) {
                    try {
                        msgCodeService.generateCode(po.getMsgClass());
                        System.out.println("删除后自动同步代码成功: " + po.getMsgClass());
                    } catch (Exception e) {
                        System.err.println("删除后自动同步代码失败: " + e.getMessage());
                        // 显示警告，但不阻止删除操作完成
                        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                        warningAlert.setTitle("代码同步警告");
                        warningAlert.setHeaderText("代码同步失败");
                        warningAlert.setContentText("删除消息成功，但代码同步失败: " + e.getMessage() + "\n请手动检查代码生成配置");
                        warningAlert.showAndWait();
                    }
                }

                // 刷新数据
                loadData();
                
            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("删除失败");
                errorAlert.setHeaderText("删除操作失败");
                errorAlert.setContentText("删除消息失败: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
    
    /**
     * 检查消息引用
     * @param po 消息对象
     * @return 是否有引用
     */
    private boolean checkMessageReferences(BizMsgInfoPO po) {
        // TODO: 实现检查消息引用的逻辑
        // 这里可以检查该消息是否在其他地方被使用
        // 例如：检查代码中的引用、配置文件中的引用等
        return false; // 暂时返回false，表示没有引用
    }
    
    /**
     * 加载消息分类下拉框选项
     */
    private void loadMsgClassOptions(ComboBox<String> comboBox) {
        try {
            // 构建查询条件
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(globalPropes.getGroupName());
            queryPO.setProjectName(globalPropes.getProjectName());
            
            // 查询所有消息分类
            List<BizMsgInfoPO> allMsgs = bizMsgInfoRpcService.queryForList(queryPO);
            
            // 提取并去重消息分类
            List<String> msgClasses = allMsgs.stream()
                    .map(BizMsgInfoPO::getMsgClass)
                    .filter(msgClass -> msgClass != null && !msgClass.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
            
            // 设置下拉框选项
            comboBox.getItems().setAll(msgClasses);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String suggestedMsgClass() {
        String abbr = StringUtils.trimToEmpty(globalPropes.getAppName());
        if (abbr.isEmpty()) {
            return "MsgCd";
        }
        return abbr + "MsgCd";
    }

    private String resolveComboText(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return "";
        }
        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            String typed = StringUtils.trimToEmpty(comboBox.getEditor().getText());
            if (!typed.isEmpty()) {
                return typed;
            }
        }
        return comboBox.getValue() != null ? comboBox.getValue().trim() : "";
    }
    
    /**
     * 显示新增消息对话框
     */
    private void showAddDialog() {
        showMessageDialog(null);
    }
    
    /**
     * 显示消息编辑对话框
     */
    private void showEditDialog(BizMsgInfoPO po) {
        showMessageDialog(po);
    }
    
    /**
     * 显示消息对话框（新增或编辑）
     */
    private void showMessageDialog(BizMsgInfoPO existingMsg) {
        boolean isEditMode = existingMsg != null;
        
        // 使用Stage而不是Dialog，这样可以更好地控制窗口行为
        Stage dialogStage = new Stage();
        dialogStage.setTitle(isEditMode ? "编辑消息" : "新增消息");
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
        
        // 创建输入字段
        ComboBox<String> msgClassComboBox = new ComboBox<>();
        msgClassComboBox.setMaxWidth(Double.MAX_VALUE);
        
        if (isEditMode) {
            // 编辑模式：只读，保持边框和清晰文字
            msgClassComboBox.setEditable(false);
            msgClassComboBox.setDisable(true);
            msgClassComboBox.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 0; -fx-min-height: 28px; -fx-pref-height: 28px; -fx-max-height: 28px; -fx-font-size: 13px; -fx-background-color: #f8f9fa; -fx-text-fill: #495057; -fx-opacity: 0.8;");
            msgClassComboBox.setValue(existingMsg.getMsgClass());
        } else {
            msgClassComboBox.setEditable(true);
            msgClassComboBox.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 0; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px; -fx-font-size: 13px;");
            loadMsgClassOptions(msgClassComboBox);
            if (!msgClassComboBox.getItems().isEmpty()) {
                msgClassComboBox.setValue(msgClassComboBox.getItems().get(0));
            } else {
                String hint = suggestedMsgClass();
                msgClassComboBox.setPromptText("请输入消息分类，如：" + hint);
                if (msgClassComboBox.getEditor() != null) {
                    msgClassComboBox.getEditor().setPromptText("请输入消息分类，如：" + hint);
                }
            }
        }
        
        TextField msgKeyField = new TextField();
        msgKeyField.setMaxWidth(Double.MAX_VALUE);
        
        if (isEditMode) {
            // 编辑模式：只读，灰色背景
            msgKeyField.setEditable(false);
            msgKeyField.setPromptText("");
            msgKeyField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-background-color: #f8f9fa; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
            msgKeyField.setText(existingMsg.getMsgKey());
        } else {
            // 新增模式：可编辑
            msgKeyField.setPromptText("请输入消息键");
            msgKeyField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
            // 为消息键字段添加自动补全功能
            setupAutoComplete(msgKeyField, "msgKey");
        }
        
        TextField msgCdField = new TextField();
        msgCdField.setPromptText("请输入消息代码");
        msgCdField.setMaxWidth(Double.MAX_VALUE);
        
        if (isEditMode) {
            // 编辑模式：只读，灰色背景
            msgCdField.setEditable(false); // 设置为只读
            msgCdField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-background-color: #f8f9fa; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
            msgCdField.setText(existingMsg.getMsgCd());
        } else {
            // 新增模式：可编辑，自动生成默认值
            msgCdField.setEditable(true); // 设置为可编辑
            msgCdField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
            
            try {
                String latestMsgCode = msgCodeService.getLatestMsgCode();
                msgCdField.setText(latestMsgCode);
            } catch (Exception e) {
                // 如果生成失败，清空文本但保持可编辑
                msgCdField.setText(""); // 清空文本
                msgCdField.setPromptText("自动生成失败，请手动输入");
                System.err.println("自动生成消息代码失败: " + e.getMessage());
            }
            
            // 为消息代码字段添加自动补全功能（仅在新增模式下）
            setupAutoComplete(msgCdField, "msgCd");
        }
        
        TextField msgDescCnField = new TextField();
        msgDescCnField.setPromptText("请输入中文描述");
        msgDescCnField.setMaxWidth(Double.MAX_VALUE);
        msgDescCnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        if (isEditMode) {
            msgDescCnField.setText(existingMsg.getMsgDescCn());
        }
        
        // 为中文描述字段添加自动补全功能
        setupAutoComplete(msgDescCnField, "msgDescCn");
        
        TextField msgDescEnField = new TextField();
        msgDescEnField.setPromptText("请输入英文描述");
        msgDescEnField.setMaxWidth(Double.MAX_VALUE);
        msgDescEnField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-padding: 6 10; -fx-min-height: 32px; -fx-pref-height: 32px; -fx-max-height: 32px;");
        if (isEditMode) {
            msgDescEnField.setText(existingMsg.getMsgDescEn());
        }
        
        // 为英文描述字段添加自动补全功能
        setupAutoComplete(msgDescEnField, "msgDescEn");
        
        // 创建自动同步代码复选框
        CheckBox autoSyncCheckBox = new CheckBox("自动同步代码");
        autoSyncCheckBox.setSelected(true); // 默认选中
        autoSyncCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-padding: 6 0;");
        
        // 创建标签样式
        String labelStyle = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #495057; -fx-padding: 6 0;";
        
        // 添加到网格
        Label msgClassLabel = new Label("消息分类:");
        msgClassLabel.setStyle(labelStyle);
        grid.add(msgClassLabel, 0, 0);
        grid.add(msgClassComboBox, 1, 0);
        
        Label msgKeyLabel = new Label("消息键:");
        msgKeyLabel.setStyle(labelStyle);
        grid.add(msgKeyLabel, 0, 1);
        grid.add(msgKeyField, 1, 1);
        
        Label msgCdLabel = new Label("消息代码:");
        msgCdLabel.setStyle(labelStyle);
        grid.add(msgCdLabel, 0, 2);
        grid.add(msgCdField, 1, 2);
        
        Label msgDescCnLabel = new Label("中文描述:");
        msgDescCnLabel.setStyle(labelStyle);
        grid.add(msgDescCnLabel, 0, 3);
        grid.add(msgDescCnField, 1, 3);
        
        Label msgDescEnLabel = new Label("英文描述:");
        msgDescEnLabel.setStyle(labelStyle);
        grid.add(msgDescEnLabel, 0, 4);
        grid.add(msgDescEnField, 1, 4);
        
        grid.add(autoSyncCheckBox, 1, 5); // 复选框跨越两列
        
        // 添加提示标签
        Label tipLabel = new Label("");
        tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc3545; -fx-padding: 5 0;");
        tipLabel.setWrapText(true);
        tipLabel.setMaxWidth(Double.MAX_VALUE);
        grid.add(tipLabel, 1, 6); // 在第6行显示提示
        
        // 添加按钮区域
        HBox buttonBox = new HBox(10); // 10px间距
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        // 创建翻译按钮
        Button translateButton = new Button("翻译");
        translateButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-font-size: 13px; -fx-cursor: hand;");
        translateButton.setOnAction(event -> {
            // 处理翻译逻辑
            handleTranslateButtonClick(msgKeyField, msgDescCnField, msgDescEnField, tipLabel);
        });
        
        buttonBox.getChildren().add(translateButton);
        grid.add(buttonBox, 1, 7); // 在第7行显示按钮区域
        
        // 设置列的增长策略，让输入框列占满剩余空间
        GridPane.setHgrow(msgClassComboBox, Priority.ALWAYS);
        GridPane.setHgrow(msgKeyField, Priority.ALWAYS);
        GridPane.setHgrow(msgCdField, Priority.ALWAYS);
        GridPane.setHgrow(msgDescCnField, Priority.ALWAYS);
        GridPane.setHgrow(msgDescEnField, Priority.ALWAYS);
        
        // 创建场景并设置内容
        Scene scene = new Scene(grid);
        dialogStage.setScene(scene);
        
        // 创建自定义保存按钮（不需要移除，因为我们已经没有默认的保存按钮了）
        
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
            String msgClass = resolveComboText(msgClassComboBox);
            String msgKey = msgKeyField.getText().trim();
            String msgCd = msgCdField.getText().trim();
            String msgDescCn = msgDescCnField.getText().trim();
            String msgDescEn = msgDescEnField.getText().trim();
            
            // 检查必填字段
            if (msgClass.isEmpty()) {
                tipLabel.setText("消息分类不能为空");
                return;
            }
            if (msgKey.isEmpty()) {
                tipLabel.setText("消息键不能为空");
                return;
            }
            if (msgCd.isEmpty()) {
                tipLabel.setText("消息代码不能为空");
                return;
            }
            if (msgDescCn.isEmpty() && msgDescEn.isEmpty()) {
                tipLabel.setText("中文描述和英文描述至少填写一个");
                return;
            }
            
            // 清除之前的错误提示
            tipLabel.setText("");
            
            // 构建消息对象
            BizMsgInfoPO resultMsg = new BizMsgInfoPO();
            resultMsg.setMsgClass(msgClass);
            resultMsg.setMsgKey(msgKey);
            resultMsg.setMsgCd(msgCd);
            resultMsg.setMsgDescCn(msgDescCn);
            resultMsg.setMsgDescEn(msgDescEn);
            resultMsg.setStatus(DataStatusEnum.WAIT_AUDIT.getCode());
            
            // 如果是编辑模式，保留原有的其他字段
            if (isEditMode) {
                resultMsg.setMsgRef(existingMsg.getMsgRef());
                resultMsg.setGroupName(existingMsg.getGroupName());
                resultMsg.setProjectName(existingMsg.getProjectName());
                resultMsg.setModuleName(existingMsg.getModuleName());
                resultMsg.setAppName(existingMsg.getAppName());
                resultMsg.setUpdateBy(existingMsg.getUpdateBy());
                resultMsg.setUpdateTime(existingMsg.getUpdateTime());
            }
            
            // 获取自动同步代码的状态
            boolean autoSync = autoSyncCheckBox.isSelected();
            boolean operationSuccess = false;
            
            try {
                // 根据模式执行不同的操作
                if (isEditMode) {
                    // 更新到数据库
                    msgCodeService.updateMessage(resultMsg);
                    operationSuccess = true;
                } else {
                    // 插入到数据库
                    msgCodeService.addMessage(resultMsg);
                    operationSuccess = true;
                }
                
                // 如果勾选了自动同步代码，则重新生成Java类代码
                if (autoSync) {
                    try {
                        msgCodeService.generateCode(resultMsg.getMsgClass());
                        System.out.println("自动同步代码成功: " + resultMsg.getMsgRef());
                    } catch (Exception e) {
                        System.err.println("自动同步代码失败: " + e.getMessage());
                        // 显示警告，但不阻止操作完成
                        tipLabel.setText("代码生成失败: " + e.getMessage() + " (请手动检查代码生成配置)");
                    }
                }
                
                // 只有在操作成功时才关闭对话框并刷新数据
                if (operationSuccess) {
                    // 刷新数据
                    loadData();
                    // 关闭对话框
                    dialogStage.close();
                }
                
            } catch (Exception e) {
                // 显示错误信息，但不关闭对话框
                tipLabel.setText("操作失败: " + e.getMessage());
            }
        });
        
        // 设置取消按钮的点击事件
        cancelButton.setOnAction(event -> {
            dialogStage.close();
        });
        
        // 将按钮添加到按钮区域
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        
        // 显示对话框
        dialogStage.showAndWait();
    }
    
    /**
     * 设置可复制的单元格工厂
     */
    private void setCopyableCellFactory(TableColumn<BizMsgInfoPO, String> column, Function<BizMsgInfoPO, String> getter) {
        setCopyableCellFactory(column, getter, false);
    }
    
    /**
     * 设置可复制的单元格工厂
     */
    private void setCopyableCellFactory(TableColumn<BizMsgInfoPO, String> column, Function<BizMsgInfoPO, String> getter, boolean centerAlign) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<BizMsgInfoPO, String>() {
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
    
    /**
     * 设置可复制的单元格工厂（自定义复制格式）
     * 专门用于消息键列，复制格式为 "消息分类.消息键"
     */
    private void setCopyableCellFactoryWithCustomFormat(TableColumn<BizMsgInfoPO, String> column, Function<BizMsgInfoPO, String> getter, boolean centerAlign) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<BizMsgInfoPO, String>() {
            private final Label label = new Label();
            {
                label.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        
                        // 获取当前行的数据
                        BizMsgInfoPO currentItem = getTableView().getItems().get(getIndex());
                        if (currentItem != null) {
                            // 构建复制格式：消息分类.消息键
                            String msgClass = currentItem.getMsgClass();
                            String msgKey = currentItem.getMsgKey();
                            String copyText = (msgClass != null ? msgClass : "") + "." + (msgKey != null ? msgKey : "");
                            
                            content.putString(copyText);
                            clipboard.setContent(content);
                            Tooltip tp = new Tooltip("已复制: " + copyText);
                            Tooltip.install(label, tp);
                            tp.show(label, event.getScreenX(), event.getScreenY());
                            new Thread(() -> {
                                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                                Platform.runLater(tp::hide);
                            }).start();
                        }
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
    
    /**
     * 设置自动补全功能
     * @param textField 要添加自动补全的文本框
     * @param fieldType 字段类型（msgKey, msgCd, msgDescCn, msgDescEn）
     */
    private void setupAutoComplete(TextField textField, String fieldType) {
        // 创建自动补全弹出窗口
        Popup popup = new Popup();
        ListView<String> suggestionList = new ListView<>();
        suggestionList.setPrefWidth(textField.getWidth());
        suggestionList.setPrefHeight(150);
        suggestionList.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1;");
        
        popup.getContent().add(suggestionList);
        
        // 获取建议数据
        List<String> suggestions = getSuggestions(fieldType);
        ObservableList<String> suggestionItems = FXCollections.observableArrayList(suggestions);
        suggestionList.setItems(suggestionItems);
        
        // 监听文本框输入变化
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                popup.hide();
                return;
            }
            
            // 过滤建议
            List<String> filteredSuggestions = suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().contains(newValue.toLowerCase()))
                    .limit(10) // 限制显示数量
                    .collect(java.util.stream.Collectors.toList());
            
            if (filteredSuggestions.isEmpty()) {
                popup.hide();
                return;
            }
            
            suggestionList.getItems().setAll(filteredSuggestions);
            
            // 显示弹出窗口
            if (!popup.isShowing()) {
                // 计算弹出窗口位置
                javafx.geometry.Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
                popup.show(textField, bounds.getMinX(), bounds.getMaxY());
            }
        });
        
        // 监听建议列表选择
        suggestionList.setOnMouseClicked(event -> {
            String selectedItem = suggestionList.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                textField.setText(selectedItem);
                popup.hide();
            }
        });
        
        // 监听键盘事件
        textField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN:
                    if (popup.isShowing() && !suggestionList.getItems().isEmpty()) {
                        suggestionList.requestFocus();
                        suggestionList.getSelectionModel().select(0);
                    }
                    break;
                case ENTER:
                    if (popup.isShowing()) {
                        String selectedItem = suggestionList.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            textField.setText(selectedItem);
                        }
                        popup.hide();
                    }
                    break;
                case ESCAPE:
                    popup.hide();
                    break;
            }
        });
        
        // 监听建议列表的键盘事件
        suggestionList.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    String selectedItem = suggestionList.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        textField.setText(selectedItem);
                        popup.hide();
                    }
                    break;
                case ESCAPE:
                    popup.hide();
                    textField.requestFocus();
                    break;
            }
        });
        
        // 监听焦点变化
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                // 延迟隐藏，给用户时间点击建议
                javafx.application.Platform.runLater(() -> {
                    if (!popup.isShowing() || !suggestionList.isFocused()) {
                        popup.hide();
                    }
                });
            }
        });
        
        // 监听建议列表的焦点变化
        suggestionList.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                popup.hide();
            }
        });
    }
    
    // 缓存自动补全建议数据
    private List<BizMsgInfoPO> cachedSuggestions = null;
    private long lastCacheTime = 0;
    private static final long CACHE_EXPIRE_TIME = 30000; // 缓存30秒
    
    /**
     * 获取自动补全建议数据（带缓存）
     * @param fieldType 字段类型
     * @return 建议列表
     */
    private List<String> getSuggestions(String fieldType) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 检查缓存是否有效
            long currentTime = System.currentTimeMillis();
            if (cachedSuggestions == null || (currentTime - lastCacheTime) > CACHE_EXPIRE_TIME) {
                // 缓存过期或不存在，重新查询数据库
                BizMsgInfoPO queryPO = new BizMsgInfoPO();
                queryPO.setGroupName(globalPropes.getGroupName());
                queryPO.setProjectName(globalPropes.getProjectName());
                cachedSuggestions = bizMsgInfoRpcService.queryForList(queryPO);
                lastCacheTime = currentTime;
                System.out.println("自动补全建议数据已缓存，共 " + cachedSuggestions.size() + " 条记录");
            }
            
            // 从缓存中提取建议
            if (cachedSuggestions != null) {
                switch (fieldType) {
                    case "msgKey":
                        suggestions = cachedSuggestions.stream()
                                .map(BizMsgInfoPO::getMsgKey)
                                .filter(key -> key != null && !key.trim().isEmpty())
                                .distinct()
                                .sorted()
                                .collect(java.util.stream.Collectors.toList());
                        break;
                    case "msgCd":
                        suggestions = cachedSuggestions.stream()
                                .map(BizMsgInfoPO::getMsgCd)
                                .filter(cd -> cd != null && !cd.trim().isEmpty())
                                .distinct()
                                .sorted()
                                .collect(java.util.stream.Collectors.toList());
                        break;
                    case "msgDescCn":
                        suggestions = cachedSuggestions.stream()
                                .map(BizMsgInfoPO::getMsgDescCn)
                                .filter(desc -> desc != null && !desc.trim().isEmpty())
                                .distinct()
                                .sorted()
                                .collect(java.util.stream.Collectors.toList());
                        break;
                    case "msgDescEn":
                        suggestions = cachedSuggestions.stream()
                                .map(BizMsgInfoPO::getMsgDescEn)
                                .filter(desc -> desc != null && !desc.trim().isEmpty())
                                .distinct()
                                .sorted()
                                .collect(java.util.stream.Collectors.toList());
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("获取自动补全建议失败: " + e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * 清除自动补全建议缓存
     */
    private void clearSuggestionsCache() {
        cachedSuggestions = null;
        lastCacheTime = 0;
    }
    
    /**
     * 处理翻译按钮点击事件
     * @param msgKeyField 消息键输入框
     * @param msgDescCnField 中文描述输入框
     * @param msgDescEnField 英文描述输入框
     * @param tipLabel 提示标签
     */
    private void handleTranslateButtonClick(TextField msgKeyField, TextField msgDescCnField, TextField msgDescEnField, Label tipLabel) {
        try {
            // 获取中文描述和英文描述
            String msgDescCn = msgDescCnField.getText().trim();
            String msgDescEn = msgDescEnField.getText().trim();
            
            // 检查中文描述、英文描述二者必须输入其中的一个
            if (msgDescCn.isEmpty() && msgDescEn.isEmpty()) {
                tipLabel.setText("中文描述、英文描述二者必须输入其中的一个");
                return;
            }

            if (!msgDescCn.isEmpty() && !msgDescEn.isEmpty()&& StringUtils.isNotBlank(msgKeyField.getText())) {
                tipLabel.setText("中文描述、英文描述都不为空，无需翻译");
                return;
            }

            // 仅输入中文
            if (StringUtils.isBlank(msgDescEn) && StringUtils.isNotBlank(msgDescCn)) {
                String descEn = translationService.translateZhToEn(msgDescCn);
                msgDescEnField.setText(descEn);
            // 仅输入英文
            } else if (StringUtils.isNotBlank(msgDescEn) && StringUtils.isBlank(msgDescCn)) {
                String descCn = translationService.translateEnToZh(msgDescEn);
                msgDescCnField.setText(descCn);
            } else if (StringUtils.isBlank(msgKeyField.getText())) {
                msgKeyField.setText(msgCodeService.toMsgKey(msgDescEnField.getText()));
            } else {
                tipLabel.setText("中文描述和英文描述不可以全部为空");
                return;
            }
            if(StringUtils.isBlank(msgKeyField.getText())) {
                msgKeyField.setText(msgCodeService.toMsgKey(msgDescEnField.getText()));
            }
        } catch (Exception e) {
            tipLabel.setText("翻译失败: " + e.getMessage());
        }
    }


} 