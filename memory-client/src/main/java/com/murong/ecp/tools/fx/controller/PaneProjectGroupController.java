package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.service.common.ProjectGroupService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.SelectDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.ToggleSwitch;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PaneProjectGroupController implements Initializable {
    
    @Autowired
    private ProjectGroupService projectGroupService;
    
    // 引用MainController，用于刷新主界面的项目下拉列表
    private MainController mainController;
    
    // 标记是否真的修改了showFlag值
    private boolean showFlagChanged = false;
    
    // 用于存储每行的选中状态
    private ObservableList<SimpleBooleanProperty> selectedList;
    
    @FXML
    private TableView<ProjectSettingPO> projectSettingsTable;
    @FXML
    private TableColumn<ProjectSettingPO, String> projectNameColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> projectTypeColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> appNameColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> appPortColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> schemaColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> currentColumn;
    @FXML
    private TableColumn<ProjectSettingPO, String> actionColumn;
    
    @FXML
    private ComboBox<String> projectGroupCombo;
    @FXML
    private Button updGroupBtn;
    @FXML
    private Button addProjectBtn;
    @FXML
    private Button batchAddMicroserviceBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[DEBUG] PaneProjectGroupController: 开始初始化");
        
        // 初始化表格列
        initTableColumns();
        
        // 初始化项目群下拉列表
        initProjectGroupCombo();
        
        // 监听下拉框切换，自动刷新项目设置列表
        projectGroupCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                refreshProjectSettingsList(newVal);
            }
        });

        // 添加新项目按钮事件
        addProjectBtn.setOnAction(e -> handleAddNewProject());
        
        // 批量增加微服务按钮事件
        batchAddMicroserviceBtn.setOnAction(e -> handleBatchAddMicroservice());
        
        // 导入项目配置按钮事件
        updGroupBtn.setOnAction(e -> updProjectGroup());
        
        System.out.println("[DEBUG] PaneProjectGroupController: 初始化完成，MainController引用: " + (mainController != null));
    }

    /**
     * 初始化表格列
     */
    private void initTableColumns() {
        // 创建复选框列
        TableColumn<ProjectSettingPO, Boolean> selectColumn = new TableColumn<>();
        selectColumn.setPrefWidth(50);
        
        // 设置列的数据绑定
        projectNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getProjectName()));
        
        projectTypeColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getProjectType()));
        projectTypeColumn.setCellFactory(createCenteredCellFactory());
        
        appNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getAppName()));
        appNameColumn.setCellFactory(createCenteredCellFactory());
        
        appPortColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getAppPort()));
        appPortColumn.setCellFactory(createCenteredCellFactory());
        
        schemaColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSchemaNm()));
        schemaColumn.setCellFactory(createCenteredCellFactory());
        
        // 当前项目列使用CheckBox
        currentColumn.setCellFactory(createCheckBoxCellFactory());
        
        // 操作列使用按钮
        actionColumn.setCellFactory(createActionCellFactory());
        
        // 设置表格可编辑
        projectSettingsTable.setEditable(true);
        
        // 设置双击编辑
        setupDoubleClickEditing();
        
        // 创建全选复选框
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setTooltip(new Tooltip("全选/全不选"));
        selectColumn.setGraphic(selectAllCheckBox);
        
        // 用于存储每行的选中状态
        selectedList = javafx.collections.FXCollections.observableArrayList();
        
        // 全选/全不选功能
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (javafx.beans.property.SimpleBooleanProperty prop : selectedList) {
                prop.set(newVal);
            }
            projectSettingsTable.refresh();
        });
        
        // 复选框列绑定
        selectColumn.setCellValueFactory(param -> {
            int index = projectSettingsTable.getItems().indexOf(param.getValue());
            if (index >= 0 && index < selectedList.size()) {
                return selectedList.get(index);
            } else {
                return new javafx.beans.property.SimpleBooleanProperty(false);
            }
        });
        
        selectColumn.setCellFactory(col -> new TableCell<ProjectSettingPO, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0 && index < selectedList.size()) {
                        selectedList.get(index).set(checkBox.isSelected());
                    }
                    // 更新全选框状态
                    updateSelectAllCheckBox(selectAllCheckBox);
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
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
        
        // 添加复选框列到表格
        projectSettingsTable.getColumns().add(0, selectColumn);
        
        // 初始化选中状态列表
        refreshSelectedList();
    }

    /**
     * 创建居中文本单元格工厂
     */
    private Callback<TableColumn<ProjectSettingPO, String>, TableCell<ProjectSettingPO, String>> createCenteredCellFactory() {
        return column -> new TableCell<ProjectSettingPO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    /**
     * 创建ToggleSwitch单元格工厂
     */
    private Callback<TableColumn<ProjectSettingPO, String>, TableCell<ProjectSettingPO, String>> createCheckBoxCellFactory() {
        return column -> new TableCell<ProjectSettingPO, String>() {
            private final ToggleSwitch toggleSwitch = new ToggleSwitch();
            
            {
                // 监听ToggleSwitch状态变化
                toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    ProjectSettingPO setting = getTableView().getItems().get(getIndex());
                    
                    // 检查是否真的改变了showFlag值
                    String currentShowFlag = setting.getShowFlag();
                    String newShowFlag = newVal ? "Y" : "N";
                    
                    // 如果值没有改变，直接返回
                    if (currentShowFlag.equals(newShowFlag)) {
                        return;
                    }
                    
                    // 如果要关闭项目，检查是否是当前正在使用的项目
                    if (!newVal) {
                        if (isCurrentUsingProject(setting)) {
                            // 恢复开关状态
                            toggleSwitch.setSelected(true);
                            ViewUtils.alertForFail("无法关闭当前正在使用的项目！");
                            return;
                        }
                    }
                    
                    // 标记showFlag已改变
                    showFlagChanged = true;
                    
                    // 更新数据库
                    projectGroupService.updateProjectShowFlag(setting, newShowFlag);
                    refreshProjectSettingsList(setting.getGroupName());
                    
                    // 如果项目群相同，刷新主界面的项目下拉列表
                    if (mainController != null && projectGroupCombo.getValue().equals(mainController.getCurrentGroup())) {
                        System.out.println("[DEBUG] showFlag已改变，准备刷新主界面项目下拉列表");
                        Platform.runLater(() -> {
                            try {
                                mainController.refreshProjectComboBox();
                                System.out.println("[DEBUG] 主界面项目下拉列表刷新完成");
                            } catch (Exception e) {
                                System.err.println("[ERROR] 刷新主界面项目下拉列表失败: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    } else {
                        System.out.println("[DEBUG] 跳过刷新主界面项目下拉列表 - mainController: " + (mainController != null) + 
                                         ", projectGroup: " + projectGroupCombo.getValue() + 
                                         ", mainGroup: " + (mainController != null ? mainController.getCurrentGroup() : "null"));
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ProjectSettingPO setting = getTableView().getItems().get(getIndex());
                    // 临时禁用监听器，避免初始化时触发
                    boolean wasSelected = toggleSwitch.isSelected();
                    boolean shouldBeSelected = "Y".equals(setting.getShowFlag());
                    
                    // 只有当值真的不同时才设置，避免触发监听器
                    if (wasSelected != shouldBeSelected) {
                        toggleSwitch.setSelected(shouldBeSelected);
                    }
                    setGraphic(toggleSwitch);
                }
                setStyle("-fx-alignment: center;");
            }
        };
    }

    /**
     * 创建操作按钮单元格工厂
     */
    private Callback<TableColumn<ProjectSettingPO, String>, TableCell<ProjectSettingPO, String>> createActionCellFactory() {
        return column -> new TableCell<ProjectSettingPO, String>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox buttonBox = new HBox(8, editBtn, deleteBtn);
            
            {
                // 设置编辑按钮样式 - 使用SVG图标样式
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
                    ProjectSettingPO setting = getTableView().getItems().get(getIndex());
                    showEditDialog(setting);
                });
                
                // 设置删除按钮样式 - 使用SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setTooltip(new Tooltip("删除"));
                deleteBtn.setMinWidth(28);
                deleteBtn.setPrefWidth(28);
                deleteBtn.setMaxWidth(28);
                deleteBtn.setMinHeight(28);
                deleteBtn.setPrefHeight(28);
                deleteBtn.setMaxHeight(28);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnMousePressed(e -> deleteBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseReleased(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteBtn.setOnAction(e -> {
                    ProjectSettingPO setting = getTableView().getItems().get(getIndex());
                    handleDeleteProject(setting);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
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

    /**
     * 设置双击编辑
     */
    private void setupDoubleClickEditing() {
        projectSettingsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ProjectSettingPO selectedItem = projectSettingsTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    showEditDialog(selectedItem);
                }
            }
        });
    }

    /**
     * 显示编辑对话框
     */
    private void showEditDialog(ProjectSettingPO setting) {
        Dialog<ProjectSettingPO> dialog = new Dialog<>();
        dialog.setTitle("编辑项目设置");
        dialog.setHeaderText("编辑项目: " + setting.getProjectName());
        dialog.setResizable(true);

        // 设置按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // 创建输入字段
        TextField projectNameField = new TextField(setting.getProjectName());
        TextField projectTypeField = new TextField(setting.getProjectType());
        TextField appNameField = new TextField(setting.getAppName());
        TextField appPortField = new TextField(setting.getAppPort());
        TextField schemaField = new TextField(setting.getSchemaNm());
        TextField basePathField = new TextField(setting.getBasePath());
        TextArea projectDescArea = new TextArea(setting.getProjectDesc());
        projectDescArea.setPrefRowCount(3);

        // 添加到网格
        int row = 0;
        grid.add(new Label("项目名称:"), 0, row);
        grid.add(projectNameField, 1, row);
        row++;
        grid.add(new Label("项目类型:"), 0, row);
        grid.add(projectTypeField, 1, row);
        row++;
        grid.add(new Label("应用名称:"), 0, row);
        grid.add(appNameField, 1, row);
        row++;
        grid.add(new Label("应用端口:"), 0, row);
        grid.add(appPortField, 1, row);
        row++;
        grid.add(new Label("Schema名称:"), 0, row);
        grid.add(schemaField, 1, row);
        row++;
        grid.add(new Label("基础路径:"), 0, row);
        grid.add(basePathField, 1, row);
        row++;
        grid.add(new Label("项目描述:"), 0, row);
        grid.add(projectDescArea, 1, row);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 更新设置对象
                setting.setProjectName(projectNameField.getText());
                setting.setProjectType(projectTypeField.getText());
                setting.setAppName(appNameField.getText());
                setting.setAppPort(appPortField.getText());
                setting.setSchemaNm(schemaField.getText());
                setting.setBasePath(basePathField.getText());
                setting.setProjectDesc(projectDescArea.getText());
                
                // 保存到数据库
                projectGroupService.saveProjectSetting(setting);
                
                // 刷新列表
                refreshProjectSettingsList(setting.getGroupName());
                
                return setting;
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * 处理删除项目
     */
    private void handleDeleteProject(ProjectSettingPO setting) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除项目 '" + setting.getProjectName() + "' 吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                projectGroupService.deleteProjectSetting(setting.getGroupName(), setting.getProjectName());
                ViewUtils.alertForSucess("删除成功！");
                refreshProjectSettingsList(setting.getGroupName());
            }
        });
    }

    /**
     * 初始化项目群下拉列表
     */
    private void initProjectGroupCombo() {
        projectGroupCombo.getItems().clear();
        
        // 查询所有项目群
        List<ProjectGroupPO> projectGroups = projectGroupService.queryAllProjectGroups();
        for (ProjectGroupPO group : projectGroups) {
            projectGroupCombo.getItems().add(group.getGroupName());
        }
        
        // 设置当前选中的项目群
        ProjectGroupPO currentGroup = projectGroupService.queryCurrentProjectGroup();
        if (currentGroup != null) {
            projectGroupCombo.setValue(currentGroup.getGroupName());
            refreshProjectSettingsList(currentGroup.getGroupName());
        } else if (!projectGroupCombo.getItems().isEmpty()) {
            projectGroupCombo.getSelectionModel().selectFirst();
            refreshProjectSettingsList(projectGroupCombo.getValue());
        }
    }

    /**
     * 刷新项目设置列表
     */
    private void refreshProjectSettingsList(String groupName) {
        projectSettingsTable.getItems().clear();
        List<ProjectSettingPO> projectSettings = projectGroupService.queryProjectSettingsByGroupName(groupName);
        projectSettingsTable.getItems().addAll(projectSettings);
        
        // 刷新选中状态列表
        refreshSelectedList();
    }

    /**
     * 处理添加新项目
     */
    private void handleAddNewProject() {
        String currentGroupName = projectGroupCombo.getValue();
        if (currentGroupName == null || currentGroupName.trim().isEmpty()) {
            ViewUtils.alertForFail("请先选择一个项目群！");
            return;
        }
        
        // 创建新的项目设置对象
        ProjectSettingPO newSetting = new ProjectSettingPO();
        newSetting.setGroupName(currentGroupName);
        newSetting.setProjectName("新项目");
        newSetting.setProjectType("");
        newSetting.setAppName("");
        newSetting.setAppPort("");
        newSetting.setSchemaNm("");
        newSetting.setBasePath("");
        newSetting.setProjectDesc("");
        newSetting.setCurFlag("N");
        newSetting.setShowFlag("Y");
        
        // 保存到数据库
        projectGroupService.saveProjectSetting(newSetting);
        
        // 刷新列表
        refreshProjectSettingsList(currentGroupName);
        ViewUtils.alertForSucess("新项目添加成功！");
    }

    /**
     * 检查是否是当前正在使用的项目
     */
    private boolean isCurrentUsingProject(ProjectSettingPO setting) {
        if (mainController == null) return false;
        
        // 获取主界面当前选中的项目群和项目
        String mainGroup = mainController.getCurrentGroup();
        String mainProject = mainController.getCurrentProject();
        
        // 检查项目群和项目是否匹配
        return setting.getGroupName().equals(mainGroup) && setting.getProjectName().equals(mainProject);
    }
    
    /**
     * 设置MainController引用
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        System.out.println("[DEBUG] PaneProjectGroupController: MainController引用已设置");
    }
    
    /**
     * 维护项目组
     */
    private void updProjectGroup() {
        // 创建新窗口
        Stage stage = new Stage();
        stage.setTitle("维护项目组");
        stage.setWidth(600);
        stage.setHeight(500);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(projectGroupCombo.getScene().getWindow());
        
        // 创建表格
        TableView<ProjectGroupPO> groupTable = new TableView<>();
        groupTable.setEditable(true);
        
        // 创建列
        TableColumn<ProjectGroupPO, String> groupNameColumn = new TableColumn<>("项目组名称");
        TableColumn<ProjectGroupPO, String> groupDescColumn = new TableColumn<>("项目组描述");
        TableColumn<ProjectGroupPO, String> actionColumn = new TableColumn<>("操作");
        
        // 设置列宽
        groupNameColumn.setPrefWidth(200);
        groupDescColumn.setPrefWidth(250);
        actionColumn.setPrefWidth(100);
        
        // 设置数据绑定
        groupNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGroupName()));
        groupDescColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGroupDesc()));
        
        // 设置列为可编辑
        groupNameColumn.setEditable(true);
        groupDescColumn.setEditable(true);
        
        // 设置可编辑
        groupNameColumn.setCellFactory(createEditableCellFactory(groupTable, true));
        groupDescColumn.setCellFactory(createEditableCellFactory(groupTable, false));
        
        // 设置编辑提交事件
        groupNameColumn.setOnEditCommit(event -> {
            ProjectGroupPO group = event.getRowValue();
            group.setGroupName(event.getNewValue());
        });
        
        groupDescColumn.setOnEditCommit(event -> {
            ProjectGroupPO group = event.getRowValue();
            group.setGroupDesc(event.getNewValue());
        });
        
        // 设置操作列
        actionColumn.setCellFactory(createDeleteCellFactory(groupTable));
        
        // 添加列到表格
        groupTable.getColumns().addAll(groupNameColumn, groupDescColumn, actionColumn);
        
        // 加载数据
        refreshGroupTable(groupTable);
        
        // 设置双击编辑
        groupTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ProjectGroupPO selectedItem = groupTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    // 获取点击的列
                    TablePosition<ProjectGroupPO, ?> pos = groupTable.getFocusModel().getFocusedCell();
                    if (pos != null && pos.getColumn() < 2) { // 只允许前两列编辑
                        groupTable.edit(pos.getRow(), groupTable.getColumns().get(pos.getColumn()));
                    }
                }
            }
        });
        
        // 创建按钮
        Button refreshBtn = new Button("刷新");
        Button saveBtn = new Button("保存所有");
        
        // 按钮事件
        refreshBtn.setOnAction(e -> refreshGroupTable(groupTable));
        
        saveBtn.setOnAction(e -> {
            try {
                // 只保存有项目组名称的数据
                for (ProjectGroupPO group : groupTable.getItems()) {
                    if (StringUtils.isNotBlank(group.getGroupName())) {
                        projectGroupService.saveOrUpdateProjectGroup(group);
                    }
                }
                ViewUtils.alertForSucess("保存成功！");
                refreshGroupTable(groupTable);
                // 刷新主界面的项目群下拉列表
                initProjectGroupCombo();
            } catch (Exception ex) {
                ViewUtils.alertForFail("保存失败：" + ex.getMessage());
            }
        });
        
        // 创建按钮容器
        HBox buttonBox = new HBox(10, refreshBtn, saveBtn);
        buttonBox.setPadding(new javafx.geometry.Insets(10));
        
        // 创建主容器
        VBox root = new VBox(10, groupTable, buttonBox);
        root.setPadding(new javafx.geometry.Insets(10));
        
        // 设置场景
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // 显示窗口
        stage.showAndWait();
    }
    
    /**
     * 创建可编辑单元格工厂
     */
    private Callback<TableColumn<ProjectGroupPO, String>, TableCell<ProjectGroupPO, String>> createEditableCellFactory(TableView<ProjectGroupPO> table, boolean isGroupNameColumn) {
        return col -> new TableCell<ProjectGroupPO, String>() {
            private TextField textField;
            
            @Override
            public void startEdit() {
                // 允许编辑，包括空白行
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }
            
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(getString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }
            
            private void createTextField() {
                textField = new TextField(getString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textField.focusedProperty().addListener((arg0, oldValue, newValue) -> {
                    if (!newValue) {
                        commitEdit(textField.getText());
                    }
                });
                textField.setOnAction(e -> {
                    commitEdit(textField.getText());
                    // 如果是项目组名称列，且输入了内容，则在底部添加新行
                    if (isGroupNameColumn && StringUtils.isNotBlank(textField.getText())) {
                        Platform.runLater(() -> {
                            if (table.getItems().isEmpty()) {
                                return;
                            }
                            int lastIndex = table.getItems().size() - 1;
                            ProjectGroupPO currentItem = table.getItems().get(lastIndex);
                            if (currentItem == getTableView().getItems().get(getIndex())) {
                                // 只有当项目组名称不为空时才添加新行
                                if (StringUtils.isNotBlank(textField.getText())) {
                                    ProjectGroupPO newGroup = new ProjectGroupPO();
                                    newGroup.setGroupName("");
                                    newGroup.setGroupDesc("");
                                    newGroup.setCurFlag("N");
                                    table.getItems().add(newGroup);
                                }
                            }
                        });
                    }
                });
            }
            
            private String getString() {
                return getItem() == null ? "" : getItem();
            }
        };
    }
    
    /**
     * 创建删除按钮单元格工厂
     */
    private Callback<TableColumn<ProjectGroupPO, String>, TableCell<ProjectGroupPO, String>> createDeleteCellFactory(TableView<ProjectGroupPO> table) {
        return column -> new TableCell<ProjectGroupPO, String>() {
            private final Button deleteBtn = new Button();
            
            {
                // 设置删除按钮样式 - 使用SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setTooltip(new Tooltip("删除"));
                deleteBtn.setMinWidth(28);
                deleteBtn.setPrefWidth(28);
                deleteBtn.setMaxWidth(28);
                deleteBtn.setMinHeight(28);
                deleteBtn.setPrefHeight(28);
                deleteBtn.setMaxHeight(28);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnMousePressed(e -> deleteBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseReleased(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteBtn.setOnAction(e -> {
                    ProjectGroupPO group = getTableView().getItems().get(getIndex());
                    // 只有有项目组名称的才能删除
                    if (StringUtils.isNotBlank(group.getGroupName())) {
                        handleDeleteGroup(group, table);
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null
                        || StringUtils.isBlank(getTableView().getItems().get(getIndex()).getGroupName())) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
                setStyle("-fx-alignment: center;");
            }
        };
    }
    
    /**
     * 刷新项目组表格
     */
    private void refreshGroupTable(TableView<ProjectGroupPO> table) {
        table.getItems().clear();
        List<ProjectGroupPO> groups = projectGroupService.queryAllProjectGroups();
        table.getItems().addAll(groups);
        // 添加一个空白行用于新增
        ProjectGroupPO emptyGroup = new ProjectGroupPO();
        emptyGroup.setGroupName("");
        emptyGroup.setGroupDesc("");
        emptyGroup.setCurFlag("N");
        table.getItems().add(emptyGroup);
    }
    
    /**
     * 处理删除项目组
     */
    private void handleDeleteGroup(ProjectGroupPO group, TableView<ProjectGroupPO> table) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除项目组 '" + group.getGroupName() + "' 吗？\n注意：删除项目组将同时删除该组下的所有项目设置！");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    projectGroupService.deleteProjectGroup(group.getGroupName());
                    ViewUtils.alertForSucess("删除成功！");
                    refreshGroupTable(table);
                    // 刷新主界面的项目群下拉列表
                    initProjectGroupCombo();
                } catch (Exception e) {
                    ViewUtils.alertForFail("删除失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 刷新选中状态列表
     */
    private void refreshSelectedList() {
        if (selectedList != null) {
            selectedList.clear();
            for (int i = 0; i < projectSettingsTable.getItems().size(); i++) {
                selectedList.add(new javafx.beans.property.SimpleBooleanProperty(false));
            }
        }
    }

    /**
     * 更新全选复选框状态
     */
    private void updateSelectAllCheckBox(CheckBox selectAllCheckBox) {
        if (selectedList == null) return;
        
        long selectedCount = selectedList.stream().filter(javafx.beans.property.SimpleBooleanProperty::get).count();
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
    
    /**
     * 处理批量增加微服务
     */
    private void handleBatchAddMicroservice() {
        // 检查是否有选中的项目
        if (selectedList == null || selectedList.isEmpty()) {
            ViewUtils.alertForFail("请先选择要添加微服务的项目！");
            return;
        }
        
        // 获取选中的项目
        List<ProjectSettingPO> selectedProjects = new ArrayList<>();
        for (int i = 0; i < projectSettingsTable.getItems().size(); i++) {
            if (i < selectedList.size() && selectedList.get(i).get()) {
                ProjectSettingPO project = projectSettingsTable.getItems().get(i);
                if (StringUtils.isNotBlank(project.getProjectName())) {
                    selectedProjects.add(project);
                }
            }
        }
        
        if (selectedProjects.isEmpty()) {
            ViewUtils.alertForFail("请先选择要添加微服务的项目！");
            return;
        }
        
        // 获取所有项目组供用户选择
        List<ProjectGroupPO> allGroups = projectGroupService.queryAllProjectGroups();
        List<String> groupNames = allGroups.stream()
                .map(ProjectGroupPO::getGroupName)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.toList());
        
        if (groupNames.isEmpty()) {
            ViewUtils.alertForFail("没有可用的项目组，请先创建项目组！");
            return;
        }
        
        // 使用SelectDialogUtil让用户选择项目组
        SelectDialogUtil.showSelectDialog(
            "选择目标项目组",
            groupNames,
            groupNames.get(0),
            selectedGroupName -> {
                // 用户确认选择后，执行批量添加微服务
                executeBatchAddMicroservice(selectedProjects, selectedGroupName);
            },
            () -> {
                // 用户取消操作
                System.out.println("用户取消了批量添加微服务操作");
            }
        );
    }
    
    /**
     * 执行批量添加微服务
     */
    private void executeBatchAddMicroservice(List<ProjectSettingPO> selectedProjects, String targetGroupName) {
        try {
            int successCount = 0;
            int skipCount = 0;
            
            for (ProjectSettingPO selectedProject : selectedProjects) {
                // 创建新的项目设置对象，复制原项目的所有属性
                ProjectSettingPO newProject = new ProjectSettingPO();
                newProject.setGroupName(targetGroupName); // 设置新的项目组
                newProject.setProjectName(selectedProject.getProjectName());
                newProject.setProjectType(selectedProject.getProjectType());
                newProject.setProjectDesc(selectedProject.getProjectDesc());
                newProject.setAppName(selectedProject.getAppName());
                newProject.setAppPort(selectedProject.getAppPort());
                newProject.setSchemaNm(selectedProject.getSchemaNm());
                newProject.setBasePath("");
                newProject.setPropPath(selectedProject.getPropPath());
                newProject.setEnumPath(selectedProject.getEnumPath());
                newProject.setMsgcdPath(selectedProject.getMsgcdPath());
                newProject.setCurFlag("N"); // 新复制的项目默认不是当前项目
                newProject.setUpdateBy(selectedProject.getUpdateBy());
                newProject.setUpdateTime(selectedProject.getUpdateTime());
                newProject.setShowFlag("Y"); // 新复制的项目默认显示
                
                // 查询目标项目组中是否已存在相同项目名称的项目
                ProjectSettingPO existingProject = projectGroupService.queryProjectSettingByGroupAndName(targetGroupName, selectedProject.getProjectName());
                
                if (existingProject == null) {
                    // 不存在则插入新记录
                    projectGroupService.saveProjectSetting(newProject);
                    successCount++;
                } else {
                    // 已存在则跳过
                    skipCount++;
                }
            }
            
            // 显示结果
            StringBuilder message = new StringBuilder();
            if (successCount > 0) {
                message.append("成功复制 ").append(successCount).append(" 个项目到项目组 '").append(targetGroupName).append("'");
            }
            if (skipCount > 0) {
                if (message.length() > 0) {
                    message.append("，");
                }
                message.append("跳过 ").append(skipCount).append(" 个已存在的项目");
            }
            
            ViewUtils.alertForSucess(message.toString());
            
            // 刷新当前列表
            String currentGroupName = projectGroupCombo.getValue();
            if (currentGroupName != null) {
                refreshProjectSettingsList(currentGroupName);
            }
            
            // 如果目标项目组是当前项目组，刷新主界面的项目下拉列表
            if (mainController != null && targetGroupName.equals(mainController.getCurrentGroup())) {
                Platform.runLater(() -> {
                    try {
                        mainController.refreshProjectComboBox();
                    } catch (Exception e) {
                        System.err.println("[ERROR] 刷新主界面项目下拉列表失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
            
        } catch (Exception e) {
            ViewUtils.alertForFail("复制项目失败：" + e.getMessage());
        }
    }

}
