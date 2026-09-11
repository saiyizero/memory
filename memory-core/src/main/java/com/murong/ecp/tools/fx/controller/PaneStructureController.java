package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.common.EnvironmentService;
import com.murong.ecp.tools.fx.domain.service.common.TranslationService;
import com.murong.ecp.tools.fx.domain.service.structure.StructureService;
import com.murong.ecp.tools.fx.enums.DirTypeEnum;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.TranslationResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.ProjectFolderDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PaneStructureController implements Initializable {
    
    @FXML private Label projectTitleLabel;
    @FXML private Button editTitleBtn;
    @FXML private TextField projectDescTextField;
    
    // 项目结构树相关组件
    @FXML private TreeView<ProjectStructureNode> projectTreeView;
    @FXML private TableView<ProjectFolderPO> pathTableView;
    @FXML private Button translationNoteBtn;
    @FXML private Button scanDirectoryBtn;
    
    // 表格列
    @FXML private TableColumn<ProjectFolderPO, String> dirTypeColumn;
    @FXML private TableColumn<ProjectFolderPO, String> dirPathColumn;
    @FXML private TableColumn<ProjectFolderPO, String> mainFlgColumn;
    @FXML private TableColumn<ProjectFolderPO, String> actionColumn;
    
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private UserProjSettingDao projectSettingDao;
    @Autowired
    private ProjectFolderDao projectFolderDao;
    @Autowired
    private StructureService structureService;
    @Autowired
    private EnvironmentService environService;
    @Autowired
    private TranslationService translationService;

    private UserProjSettingPO currentProject;
    private String currentProjectName;
    private Map<String, List<ProjectFolderPO>> projectConfigs = new HashMap<>();
    private ObservableList<ProjectFolderPO> pathTableData = FXCollections.observableArrayList();
    
    // 项目结构节点类
    public static class ProjectStructureNode {
        private String name;
        private String path;
        private String type; // project, module, directory
        private String description;
        private String moduleName; // 模块名称
        
        public ProjectStructureNode(String name, String path, String type, String description) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.description = description;
        }
        
        public ProjectStructureNode(String name, String path, String type, String description, String moduleName) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.description = description;
            this.moduleName = moduleName;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化项目参数
        loadProjectParams();
        
        // 初始化项目结构树
        initializeProjectTreeView();
        
        // 初始化路径表格
        initializePathTableView();
        
        // 绑定事件处理
        bindEvents();
        
        // 加载默认项目结构
        loadDefaultProjectStructure();
    }
    
    /**
     * 初始化项目结构树
     */
    private void initializeProjectTreeView() {
        // 设置树形视图的单元格工厂
        projectTreeView.setCellFactory(tv -> new TreeCell<ProjectStructureNode>() {
            @Override
            protected void updateItem(ProjectStructureNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    
                    // 根据节点类型设置不同的图标
                    ImageView icon = new ImageView();
                    icon.setFitWidth(16);
                    icon.setFitHeight(16);
                    
                    switch (item.getType()) {
                        case "project":
                            icon.setImage(new Image(getClass().getResourceAsStream("/image/remixB/folder-2-line.png")));
                            break;
                        case "module":
                            icon.setImage(new Image(getClass().getResourceAsStream("/image/remixB/folder-2-line.png")));
                            break;
                        case "directory":
                            icon.setImage(new Image(getClass().getResourceAsStream("/image/remixB/folder-2-line.png")));
                            break;
                        default:
                            icon.setImage(new Image(getClass().getResourceAsStream("/image/remixB/folder-2-line.png")));
                    }
                    
                    setGraphic(icon);
                }
            }
        });
        
        // 添加选择监听器
        projectTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getValue() != null) {
                ProjectStructureNode node = newValue.getValue();
                updatePathTableData(node);
            }
        });
    }
    
    /**
     * 初始化路径表格
     */
    private void initializePathTableView() {
        // 设置表格列
        dirTypeColumn.setCellValueFactory(new PropertyValueFactory<>("dirType"));
        dirPathColumn.setCellValueFactory(new PropertyValueFactory<>("dirPath"));
        mainFlgColumn.setCellValueFactory(new PropertyValueFactory<>("mainFlg"));
        
        // 设置主标识列文字居中
        mainFlgColumn.setCellFactory(col -> new TableCell<ProjectFolderPO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setStyle("-fx-alignment: center;");
            }
        });
        
        // 设置操作列
        actionColumn.setCellFactory(param -> new TableCell<ProjectFolderPO, String>() {
            private final Button deleteButton = new Button();
            private final HBox buttonBox = new HBox(5);
            
            {
                // 设置删除按钮样式 - 使用与PaneTransactionController相同的SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                deleteButton.setGraphic(trashIcon);
                deleteButton.setTooltip(new Tooltip("删除"));
                deleteButton.setMinWidth(28);
                deleteButton.setPrefWidth(28);
                deleteButton.setMaxWidth(28);
                deleteButton.setMinHeight(28);
                deleteButton.setPrefHeight(28);
                deleteButton.setMaxHeight(28);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                
                // 添加鼠标悬停效果
                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteButton.setOnMouseExited(e -> deleteButton.setStyle("-fx-background-color: transparent;"));
                deleteButton.setOnMousePressed(e -> deleteButton.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                deleteButton.setOnMouseReleased(e -> deleteButton.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                
                buttonBox.getChildren().add(deleteButton);
                
                deleteButton.setOnAction(event -> {
                    ProjectFolderPO item = getTableView().getItems().get(getIndex());
                    deleteProjectFolder(item);
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
            }
        });
        
        // 设置表格数据
        pathTableView.setItems(pathTableData);
        
        // 添加双击编辑功能
        pathTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ProjectFolderPO selectedItem = pathTableView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    editProjectFolder(selectedItem);
                }
            }
        });
    }
    
    /**
     * 更新路径表格数据
     */
    private void updatePathTableData(ProjectStructureNode node) {
        pathTableData.clear();
        
        if (node == null) return;
        
        try {
            List<ProjectFolderPO> folderData = new ArrayList<>();
            
            if ("project".equals(node.getType())) {
                // 项目根节点，显示所有模块的数据
                folderData = projectFolderDao.searchByProjectName(currentProjectName);
            } else if ("module".equals(node.getType())) {
                // 模块节点，显示该模块下所有目录的数据
                folderData = projectFolderDao.searchByModuleName(currentProjectName, node.getName());
            } else if ("directory".equals(node.getType())) {
                // 目录节点，显示该目录下的数据
                String dirBase = node.getPath();
                folderData = projectFolderDao.searchByDirBase(currentProjectName, node.getModuleName(), dirBase);
            }
            
            pathTableData.addAll(folderData);
            
        } catch (Exception e) {
            ViewUtils.alertForFail("查询路径数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 编辑项目文件夹配置
     */
    private void editProjectFolder(ProjectFolderPO folder) {
        // 创建编辑对话框
        Dialog<ProjectFolderPO> dialog = new Dialog<>();
        dialog.setTitle("编辑路径配置");
        dialog.setHeaderText("编辑路径类型和路径信息");
        dialog.getDialogPane().getStyleClass().addAll("modern-dialog", "structure-dialog");
        
        // 设置对话框样式
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
        dialog.getDialogPane().setPrefSize(500, 300);
        
        // 设置按钮类型
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // 设置按钮样式
        dialog.getDialogPane().lookupButton(saveButtonType).getStyleClass().addAll("modern-button", "modern-button-primary");
        dialog.getDialogPane().lookupButton(cancelButtonType).getStyleClass().addAll("modern-button", "modern-button-secondary");
        
        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new javafx.geometry.Insets(24, 24, 24, 24));
        grid.setStyle("-fx-background-color: transparent;");
        
        // 路径类型下拉框
        ComboBox<DirTypeEnum> dirTypeCombo = new ComboBox<>();
        dirTypeCombo.getItems().addAll(DirTypeEnum.values());
        dirTypeCombo.setCellFactory(param -> new ListCell<DirTypeEnum>() {
            @Override
            protected void updateItem(DirTypeEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getType() + " - " + item.getDesc());
                }
            }
        });
        dirTypeCombo.setButtonCell(dirTypeCombo.getCellFactory().call(null));
        dirTypeCombo.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12;");
        dirTypeCombo.setPrefWidth(300);
        
        // 添加焦点样式
        dirTypeCombo.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                dirTypeCombo.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 2;");
            } else {
                dirTypeCombo.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 1;");
            }
        });
        
        // 设置当前选中的值
        if (folder.getDirType() != null && !folder.getDirType().isEmpty()) {
            DirTypeEnum currentType = DirTypeEnum.getByType(folder.getDirType());
            if (currentType != null) {
                dirTypeCombo.setValue(currentType);
            }
        }
        
        // 路径输入框
        TextField dirPathField = new TextField(folder.getDirPath());
        dirPathField.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 1;");
        dirPathField.setPrefWidth(300);
        
        // 添加焦点样式
        dirPathField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                dirPathField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 2;");
            } else {
                dirPathField.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 1;");
            }
        });
        
        // 主路径下拉框
        ComboBox<FlgEnum> mainFlgCombo = new ComboBox<>();
        mainFlgCombo.getItems().addAll(FlgEnum.values());
        mainFlgCombo.setCellFactory(param -> new ListCell<FlgEnum>() {
            @Override
            protected void updateItem(FlgEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getValue() + " - " + item.getDesc());
                }
            }
        });
        mainFlgCombo.setButtonCell(mainFlgCombo.getCellFactory().call(null));
        mainFlgCombo.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 1;");
        mainFlgCombo.setPrefWidth(300);
        
        // 添加焦点样式
        mainFlgCombo.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mainFlgCombo.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 2;");
            } else {
                mainFlgCombo.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px; -fx-pref-height: 36px; -fx-padding: 8 12; -fx-border-width: 1;");
            }
        });
        
        // 设置当前选中的值
        if (folder.getMainFlg() != null && !folder.getMainFlg().isEmpty()) {
            for (FlgEnum flg : FlgEnum.values()) {
                if (flg.getValue().equals(folder.getMainFlg())) {
                    mainFlgCombo.setValue(flg);
                    break;
                }
            }
        } else {
            // 默认选择 "否"
            mainFlgCombo.setValue(FlgEnum.NO);
        }
        
        // 创建标签
        Label dirTypeLabel = new Label("路径类型:");
        dirTypeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-pref-width: 80px;");
        
        Label dirPathLabel = new Label("路径:");
        dirPathLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-pref-width: 80px;");
        
        Label mainFlgLabel = new Label("主路径:");
        mainFlgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-pref-width: 80px;");
        
        grid.add(dirTypeLabel, 0, 0);
        grid.add(dirTypeCombo, 1, 0);
        grid.add(dirPathLabel, 0, 1);
        grid.add(dirPathField, 1, 1);
        grid.add(mainFlgLabel, 0, 2);
        grid.add(mainFlgCombo, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                DirTypeEnum selectedType = dirTypeCombo.getValue();
                folder.setDirType(selectedType != null ? selectedType.getType() : "");
                folder.setDirPath(dirPathField.getText());
                FlgEnum selectedFlg = mainFlgCombo.getValue();
                folder.setMainFlg(selectedFlg != null ? selectedFlg.getValue() : "N");
                folder.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                folder.setUpdateBy("system");
                return folder;
            }
            return null;
        });
        
        // 显示对话框
        Optional<ProjectFolderPO> result = dialog.showAndWait();
        result.ifPresent(updatedFolder -> {
            try {
                projectFolderDao.save(updatedFolder);
                ViewUtils.alertForSucess("路径配置更新成功！");
                // 刷新当前表格数据
                TreeItem<ProjectStructureNode> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    updatePathTableData(selectedItem.getValue());
                }
            } catch (Exception e) {
                ViewUtils.alertForFail("更新失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 删除项目文件夹配置
     */
    private void deleteProjectFolder(ProjectFolderPO folder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除路径配置");
        alert.setContentText("确定要删除路径类型为 '" + folder.getDirType() + "' 的配置吗？");
        alert.getDialogPane().getStyleClass().add("modern-dialog");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                projectFolderDao.delete(folder);
                ViewUtils.alertForSucess("路径配置删除成功！");
                // 刷新当前表格数据
                TreeItem<ProjectStructureNode> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    updatePathTableData(selectedItem.getValue());
                }
            } catch (Exception e) {
                ViewUtils.alertForFail("删除失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 添加新的路径配置
     */
    @FXML
    private void addNewPathConfig() {
        TreeItem<ProjectStructureNode> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            ViewUtils.alertForFail("请先选择一个模块或目录节点");
            return;
        }
        
        ProjectStructureNode node = selectedItem.getValue();
        if (!"module".equals(node.getType()) && !"directory".equals(node.getType())) {
            ViewUtils.alertForFail("只能在模块或目录节点下添加路径配置");
            return;
        }
        
        // 创建新配置
        ProjectFolderPO newFolder = new ProjectFolderPO();
        newFolder.setGroupName(globalPropes.getGroupName());
        newFolder.setProjectName(currentProjectName);
        newFolder.setAppName(globalPropes.getAppName());
        newFolder.setModuleName(node.getModuleName() != null ? node.getModuleName() : node.getName());
        newFolder.setDirBase(node.getPath());
        newFolder.setDirType("");
        newFolder.setDirPath("");
        newFolder.setMainFlg("N");
        newFolder.setUpdateBy("system");
        newFolder.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 显示编辑对话框
        editProjectFolder(newFolder);
    }
    
    /**
     * 根据参数名称加载项目结构
     */
    private void loadProjectStructure() {
        try {
            // 获取项目配置
            List<ProjectFolderPO> configs = getProjectConfigs(currentProjectName);
            if (configs.isEmpty()) {
                ViewUtils.alertForSucess("未找到项目 '" + currentProjectName + "' 的配置数据，请先扫描目录");
                return;
            }
            
            // 构建项目结构树
            TreeItem<ProjectStructureNode> root = buildProjectStructureTree(configs);
            projectTreeView.setRoot(root);
            
            // 展开根节点
            root.setExpanded(true);
            
        } catch (Exception e) {
            ViewUtils.alertForFail("加载项目结构失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取项目配置
     */
    private List<ProjectFolderPO> getProjectConfigs(String projectName) {
        if (projectConfigs.containsKey(projectName)) {
            return projectConfigs.get(projectName);
        }
        
        List<ProjectFolderPO> configs = projectFolderDao.searchByProjectName(projectName);
        projectConfigs.put(projectName, configs);
        return configs;
    }
    
    /**
     * 构建项目结构树
     */
    private TreeItem<ProjectStructureNode> buildProjectStructureTree(List<ProjectFolderPO> configs) {
        // 创建项目根节点
        ProjectStructureNode rootNode = new ProjectStructureNode(
            currentProjectName, 
            "", 
            "project", 
            "项目根目录"
        );
        TreeItem<ProjectStructureNode> root = new TreeItem<>(rootNode);
        
        // 按模块分组
        Map<String, List<ProjectFolderPO>> moduleGroups = configs.stream()
            .collect(Collectors.groupingBy(ProjectFolderPO::getModuleName));
        
        // 构建模块结构
        for (Map.Entry<String, List<ProjectFolderPO>> entry : moduleGroups.entrySet()) {
            String moduleName = entry.getKey();
            List<ProjectFolderPO> moduleConfigs = entry.getValue();
            
            if (!moduleConfigs.isEmpty()) {
                // 创建模块节点
                ProjectStructureNode moduleNode = new ProjectStructureNode(
                    moduleName,
                    "",
                    "module",
                    moduleName + " 模块",
                    moduleName
                );
                
                TreeItem<ProjectStructureNode> moduleItem = new TreeItem<>(moduleNode);
                root.getChildren().add(moduleItem);
                
                // 按目录基础路径分组
                Map<String, List<ProjectFolderPO>> dirBaseGroups = moduleConfigs.stream()
                    .collect(Collectors.groupingBy(ProjectFolderPO::getDirBase));
                
                // 创建有序的目录列表，确保 src/main/java 在 src/main/resources 之前
                List<String> orderedDirBases = new ArrayList<>();
                
                // 首先添加 src/main/java
                if (dirBaseGroups.containsKey("src/main/java")) {
                    orderedDirBases.add("src/main/java");
                }
                
                // 然后添加 src/main/resources
                if (dirBaseGroups.containsKey("src/main/resources")) {
                    orderedDirBases.add("src/main/resources");
                }
                
                // 最后添加其他目录
                for (String dirBase : dirBaseGroups.keySet()) {
                    if (!"src/main/java".equals(dirBase) && !"src/main/resources".equals(dirBase)) {
                        orderedDirBases.add(dirBase);
                    }
                }
                
                // 按顺序添加目录节点
                for (String dirBase : orderedDirBases) {
                    List<ProjectFolderPO> dirConfigs = dirBaseGroups.get(dirBase);
                    
                    if (!dirConfigs.isEmpty()) {
                        ProjectStructureNode dirNode = new ProjectStructureNode(
                            dirBase,
                            dirBase,
                            "directory",
                            dirBase + " 目录",
                            moduleName
                        );
                        
                        TreeItem<ProjectStructureNode> dirItem = new TreeItem<>(dirNode);
                        moduleItem.getChildren().add(dirItem);
                    }
                }
            }
        }
        
        return root;
    }
    
    /**
     * 加载默认项目结构
     */
    private void loadDefaultProjectStructure() {
        loadProjectStructure();
    }
    
    /**
     * 加载项目参数
     */
    private void loadProjectParams() {
        try {
            // 从全局属性获取当前项目信息
            currentProjectName = globalPropes.getProjectName();
            UserProjSettingPO query = new UserProjSettingPO();
            query.setProjectName(currentProjectName);
            query.setGroupName(globalPropes.getGroupName());
            currentProject = projectSettingDao.queryOne(query);

            if (currentProject!=null) {
                updateUIWithProjectData(currentProject);
            }
        } catch (Exception e) {
            System.err.println("加载项目参数失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用项目数据更新UI
     */
    private void updateUIWithProjectData(UserProjSettingPO project) {
        if (project != null) {
            projectTitleLabel.setText(project.getProjectName() != null ? project.getProjectName() : "项目名称");
            
            // 设置扫描目录路径
            if (projectDescTextField != null) {
                if (project.getBasePath() != null && !project.getBasePath().isEmpty()) {
                    projectDescTextField.setText(project.getBasePath());
                } else {
                    // 如果没有设置basePath，使用默认路径
                    projectDescTextField.setText("");
                }
            }
        }
    }


    /**
     * 绑定事件
     */
    private void bindEvents() {
        // 选择目录按钮
        editTitleBtn.setOnAction(event -> showDirectoryChooser());
        
        // 扫描目录按钮
        scanDirectoryBtn.setOnAction(event -> scanDirectoryAndGenerateData());
        
        // 刷新结构按钮
        translationNoteBtn.setOnAction(event -> translateComments());
        
        // 扫描目录输入框失去焦点时保存路径
        if (projectDescTextField != null) {
            projectDescTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && currentProject != null) { // 失去焦点时
                    String newPath = projectDescTextField.getText();
                    if (newPath != null && !newPath.trim().isEmpty() && 
                        !newPath.trim().equals(currentProject.getBasePath())) {
                        saveScanDirectoryPath(newPath.trim());
                    }
                }
            });
        }
    }

    /**
     * 接口数据翻译功能
     */
    private void translateComments() {
        try {
            // 显示翻译进度对话框
            ProgressBar progressBar = showTranslationProgressDialog(0);

            // 在后台线程中执行翻译
            Task<TranslationResult> translationTask = new Task<TranslationResult>() {
                @Override
                protected TranslationResult call() throws Exception {
                    // 使用TranslationDomainService进行批量翻译
                    return translationService.translateAllData(progress -> {
                        // 在JavaFX线程中更新UI
                        javafx.application.Platform.runLater(() -> {
                            updateProgressDialog(progressBar, progress.getCurrentIndex(), progress.getTotalCount(),
                                    progress.getCurrentItem() + " (" + progress.getTableName() + ")");
                        });
                    });
                }
            };

            // 设置取消按钮功能
            Object[] userData = (Object[]) progressBar.getUserData();
            if (userData != null && userData.length >= 4) {
                Button cancelButton = (Button) userData[3];
                Alert progressAlert = (Alert) userData[2];
                cancelButton.setOnAction(e -> {
                    translationTask.cancel();
                    progressAlert.close();
                });
            }

            // 监听任务完成
            translationTask.setOnSucceeded(event -> {
                // 关闭进度对话框
                Object[] progressUserData = (Object[]) progressBar.getUserData();
                if (progressUserData != null && progressUserData.length >= 4) {
                    Alert alert = (Alert) progressUserData[2];
                    alert.close();
                }

                // 显示翻译结果
                TranslationResult result = translationTask.getValue();
                showTranslationSummary(result.getSuccessCount(), result.getFailCount(),
                        result.getTotalCount(), result.getFailedItems());
            });

            translationTask.setOnFailed(event -> {
                // 关闭进度对话框
                Object[] progressUserData = (Object[]) progressBar.getUserData();
                if (progressUserData != null && progressUserData.length >= 4) {
                    Alert alert = (Alert) progressUserData[2];
                    alert.close();
                }

                ViewUtils.alertForFail("翻译失败: " + translationTask.getException().getMessage());
            });

            // 启动翻译任务
            Thread translationThread = new Thread(translationTask);
            translationThread.setDaemon(true);
            translationThread.start();

        } catch (Exception e) {
            ViewUtils.alertForFail("翻译失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新进度对话框
     */
    private void updateProgressDialog(ProgressBar progressBar, int current, int total, String currentItem) {
        double progress = (double) current / total;
        progressBar.setProgress(progress);

        // 获取存储的标签引用
        Object[] userData = (Object[]) progressBar.getUserData();
        if (userData != null && userData.length >= 4) {
            Label progressLabel = (Label) userData[0];
            Label countLabel = (Label) userData[1];

            // 更新进度标签
            progressLabel.setText("正在翻译: " + currentItem);
            countLabel.setText(current + " / " + total + " (" + String.format("%.1f", progress * 100) + "%)");

            // 强制刷新UI
            progressLabel.requestLayout();
            countLabel.requestLayout();

            // 调试信息
            System.out.println("UI更新成功 - 进度: " + progress + ", 标签: " + currentItem);
        } else {
            System.err.println("无法获取用户数据，userData: " + (userData != null ? userData.length : "null"));
        }

        // 控制台输出
        System.out.println("翻译进度: " + current + "/" + total + " (" + String.format("%.1f", progress * 100) + "%) - " + currentItem);
    }

    /**
     * 显示翻译进度对话框
     */
    private ProgressBar showTranslationProgressDialog(int totalCount) {
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);

        Label progressLabel = new Label("准备开始翻译...");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label countLabel = new Label("0 / " + totalCount);
        countLabel.setStyle("-fx-font-size: 12px;");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(20));
        content.getChildren().addAll(
                new Label("正在翻译接口数据..."),
                progressBar,
                progressLabel,
                countLabel
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("翻译进度");
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(content);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 200);

        // 添加取消按钮
        Button cancelButton = new Button("取消翻译");
        cancelButton.setOnAction(e -> {
            alert.close();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(cancelButton);

        content.getChildren().add(buttonBox);

        // 存储进度条和标签的引用
        progressBar.setUserData(new Object[]{progressLabel, countLabel, alert, cancelButton});

        alert.show();

        return progressBar;
    }

    /**
     * 显示翻译结果摘要
     */
    private void showTranslationSummary(int successCount, int failCount, int totalCount, List<String> failedItems) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("翻译完成");
        alert.setHeaderText("多表数据翻译结果");

        StringBuilder content = new StringBuilder();
        content.append(String.format("翻译完成！\n\n"));
        content.append(String.format("✅ 成功: %d 条\n", successCount));
        content.append(String.format("❌ 失败: %d 条\n", failCount));
        content.append(String.format("📊 总计: %d 条\n", totalCount));
        content.append(String.format("📈 成功率: %.1f%%\n\n", (double) successCount / totalCount * 100));

        // 显示支持的表信息
        content.append("支持的表:\n");
        content.append("• interface_data (接口数据表)\n");
        content.append("• enum_dict (枚举字典表)\n\n");

        if (!failedItems.isEmpty()) {
            content.append("失败项目详情:\n");
            for (String failedItem : failedItems) {
                content.append("• ").append(failedItem).append("\n");
            }
        }

        // 创建文本区域显示详细结果
        TextArea textArea = new TextArea(content.toString());
        textArea.setEditable(false);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(60);
        textArea.setWrapText(true);

        // 创建按钮容器
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> alert.close());

        buttonBox.getChildren().addAll(closeButton);

        // 创建主容器
        VBox mainContent = new VBox(10);
        mainContent.getChildren().addAll(textArea, buttonBox);

        alert.getDialogPane().setContent(mainContent);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(600, 400);

        alert.showAndWait();
    }
    
    /**
     * 扫描目录并生成数据
     */
    private void scanDirectoryAndGenerateData() {
        try {
            if (currentProjectName == null || currentProjectName.isEmpty()) {
                ViewUtils.alertForFail("项目名称未设置，请先配置项目参数");
                return;
            }
            
            // 从输入框获取扫描目录路径
            String basePath = projectDescTextField.getText();
            if (basePath == null || basePath.trim().isEmpty()) {
                ViewUtils.alertForFail("请先设置扫描目录路径");
                return;
            }
            
            File baseDir = new File(basePath.trim());
            
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                ViewUtils.alertForFail("目录不存在: " + basePath);
                return;
            }

            // 扫描目录并生成数据 - 转换为ProjectSettingPO以兼容synchronousPath方法
            ProjectSettingPO projectSettingPO = new ProjectSettingPO();
            projectSettingPO.setGroupName(currentProject.getGroupName());
            projectSettingPO.setProjectName(currentProject.getProjectName());
            projectSettingPO.setProjectType(currentProject.getProjectType());
            projectSettingPO.setProjectDesc(currentProject.getProjectDesc());
            projectSettingPO.setAppName(currentProject.getAppName());
            projectSettingPO.setAppPort(currentProject.getAppPort());
            projectSettingPO.setSchemaNm(currentProject.getSchemaNm());
            projectSettingPO.setBasePath(currentProject.getBasePath());
            projectSettingPO.setPropPath(currentProject.getPropPath());
            projectSettingPO.setEnumPath(currentProject.getEnumPath());
            projectSettingPO.setMsgcdPath(currentProject.getMsgcdPath());
            projectSettingPO.setCurFlag(currentProject.getCurFlag());
            projectSettingPO.setUpdateBy(currentProject.getUpdateBy());
            projectSettingPO.setUpdateTime(currentProject.getUpdateTime());
            projectSettingPO.setShowFlag(currentProject.getShowFlag());
            List<ProjectFolderPO> directoryData = structureService.synchronousPath(projectSettingPO);
            
            // 清除缓存
            projectConfigs.remove(currentProjectName);
            ViewUtils.alertForSucess("目录扫描完成，共生成 " + directoryData.size() + " 条配置数据");
            
            // 重新加载项目结构
            loadProjectStructure();

            //刷新缓存参数 - 使用转换后的ProjectSettingPO
            environService.rebuildGlobalPropes(globalPropes, projectSettingPO);

        } catch (Exception e) {
            ViewUtils.alertForFail("扫描目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存扫描目录路径到数据库
     */
    private void saveScanDirectoryPath(String basePath) {
        try {
            if (currentProject != null) {
                UserProjSettingPO updateEntity = new UserProjSettingPO();
                updateEntity.setBasePath(basePath);
                updateEntity.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                updateEntity.setUpdateBy("system");
                
                projectSettingDao.updateByOne(updateEntity, currentProject);
                currentProject.setBasePath(basePath);
            }
        } catch (Exception e) {
            System.err.println("保存扫描目录路径失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示文件夹选择对话框
     */
    private void showDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择项目根目录");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        // 如果当前有路径，设置为初始目录
        if (currentProject != null && currentProject.getBasePath() != null && !currentProject.getBasePath().isEmpty()) {
            File currentDir = new File(currentProject.getBasePath());
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(projectTitleLabel.getScene().getWindow());
        
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            
            // 更新数据库
            if (currentProject != null) {
                try {
                    UserProjSettingPO updateEntity = new UserProjSettingPO();
                    updateEntity.setBasePath(selectedPath);
                    updateEntity.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    updateEntity.setUpdateBy("system");
                    
                    projectSettingDao.updateByOne(updateEntity, currentProject);
                    currentProject.setBasePath(selectedPath);
                    
                    // 刷新界面上的扫描目录
                    if (projectDescTextField != null) {
                        projectDescTextField.setText(selectedPath);
                    }
                    
                    ViewUtils.alertForSucess("扫描目录更新成功！");
                } catch (Exception ex) {
                    ViewUtils.alertForFail("更新失败: " + ex.getMessage());
                }
            } else {
                // 如果当前项目为空，先创建项目
                createProjectWithBasePath(selectedPath);
            }
        }
    }
    
    /**
     * 创建项目并设置基础路径
     */
    private void createProjectWithBasePath(String basePath) {
        try {
            currentProject = new UserProjSettingPO();
            currentProject.setProjectName(currentProjectName != null ? currentProjectName : "customer");
            currentProject.setProjectDesc("DBS-数字银行");
            currentProject.setProjectType("核心项目");
            currentProject.setAppName("dbs");
            currentProject.setSchemaNm("dbs");
            currentProject.setBasePath(basePath);
            currentProject.setUpdateBy("system");
            currentProject.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            projectSettingDao.insert(currentProject);
            
            // 刷新界面
            if (projectDescTextField != null) {
                projectDescTextField.setText(basePath);
            }
            
            ViewUtils.alertForSucess("项目创建成功，扫描目录已设置！");
        } catch (Exception e) {
            ViewUtils.alertForFail("创建项目失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

} 