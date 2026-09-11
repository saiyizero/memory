package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.common.UserInfoService;
import com.murong.ecp.tools.fx.domain.service.common.UserProjGroupService;
import com.murong.ecp.tools.fx.domain.service.common.UserProjSettingService;
import com.murong.ecp.tools.fx.enums.UserRoleEnum;
import com.murong.ecp.tools.fx.enums.UserStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.*;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;

@Component
public class PaneUserMngController implements Initializable {
    
    @Autowired
    private UserInfoService userInfoService;
    
    
    @Autowired
    private UserProjGroupService userProjGroupService;
    
    @Autowired
    private UserProjSettingService userProjSettingService;
    
    @Autowired
    private GlobalProperties globalProperties;
    
    // 用于存储每行的选中状态
    private ObservableList<SimpleBooleanProperty> selectedList;
    
    @FXML
    private TableView<UserInfoPO> userTable;
    @FXML
    private TableColumn<UserInfoPO, String> userIdColumn;
    @FXML
    private TableColumn<UserInfoPO, String> usernameColumn;
    @FXML
    private TableColumn<UserInfoPO, String> realNameColumn;
    @FXML
    private TableColumn<UserInfoPO, String> emailColumn;
    @FXML
    private TableColumn<UserInfoPO, String> phoneColumn;
    @FXML
    private TableColumn<UserInfoPO, String> roleColumn;
    @FXML
    private TableColumn<UserInfoPO, String> statusColumn;
    @FXML
    private TableColumn<UserInfoPO, String> createTimeColumn;
    @FXML
    private TableColumn<UserInfoPO, String> actionColumn;
    
    @FXML
    private ComboBox<String> roleFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private TextField searchField;
    @FXML
    private Button addUserBtn;
    @FXML
    private Button batchDeleteBtn;
    @FXML
    private Button refreshBtn;
    @FXML
    private Button assignPermissionBtn;
    @FXML
    private Button viewPermissionsBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[DEBUG] PaneUserManagementController: 开始初始化");
        
        // 初始化表格列
        initTableColumns();
        
        // 初始化过滤器
        initFilters();
        
        // 初始化按钮事件
        initButtonEvents();
        
        // 加载用户数据
        refreshUserTable();
        
        System.out.println("[DEBUG] PaneUserManagementController: 初始化完成");
    }

    /**
     * 初始化表格列
     */
    private void initTableColumns() {
        // 创建复选框列
        TableColumn<UserInfoPO, Boolean> selectColumn = new TableColumn<>();
        selectColumn.setPrefWidth(50);
        
        // 设置列的数据绑定
        userIdColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getUserId()));
        
        usernameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        
        realNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getRealName()));
        
        emailColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        
        phoneColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        
        roleColumn.setCellValueFactory(data -> {
            String role = data.getValue().getRoles();
            System.out.println("[DEBUG] 角色列数据绑定 - 用户: " + data.getValue().getUsername() + ", 角色: " + role);
            // 显示角色描述而不是代码
            String roleDesc = UserRoleEnum.getDescByCode(role);
            return new javafx.beans.property.SimpleStringProperty(roleDesc != null ? roleDesc : role);
        });
        
        // 创建角色选项列表，显示格式为 "代码-描述"
        ObservableList<String> roleOptions = FXCollections.observableArrayList("D-开发者", "M-管理者");
        roleColumn.setCellFactory(ComboBoxTableCell.forTableColumn(roleOptions));
        roleColumn.setEditable(true);
        
        // 设置角色列文字居中
        roleColumn.setStyle("-fx-alignment: center;");
        
        // 设置编辑提交事件
        roleColumn.setOnEditCommit(event -> {
            String newRoleDisplay = event.getNewValue();
            UserInfoPO user = event.getRowValue();
            if (user != null && newRoleDisplay != null) {
                // 从显示值中提取角色代码（取第一个字符）
                String newRoleCode = newRoleDisplay.substring(0, 1);
                // 更新用户角色
                user.setRoles(newRoleCode);
                // 调用服务更新数据库
                userInfoService.updateUserRole(user.getUserId(), newRoleCode);
                // 刷新表格显示
                userTable.refresh();
            }
        });
        
        statusColumn.setCellValueFactory(data -> {
            String status = data.getValue().getStatus();
            System.out.println("[DEBUG] 状态列数据绑定 - 用户: " + data.getValue().getUsername() + ", 状态: " + status);
            // 显示状态描述而不是代码
            String statusDesc = UserStatusEnum.getDescByCode(status);
            return new javafx.beans.property.SimpleStringProperty(statusDesc != null ? statusDesc : status);
        });
        
        // 创建状态选项列表，显示格式为 "代码-描述"
        ObservableList<String> statusOptions = FXCollections.observableArrayList("A-申请", "O-启用", "D-停止");
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(statusOptions));
        statusColumn.setEditable(true);
        
        // 设置状态列文字居中
        statusColumn.setStyle("-fx-alignment: center;");
        
        // 设置编辑提交事件
        statusColumn.setOnEditCommit(event -> {
            String newStatusDisplay = event.getNewValue();
            UserInfoPO user = event.getRowValue();
            if (user != null && newStatusDisplay != null) {
                // 从显示值中提取状态代码（取第一个字符）
                String newStatusCode = newStatusDisplay.substring(0, 1);
                // 更新用户状态
                user.setStatus(newStatusCode);
                // 调用服务更新数据库
                userInfoService.updateUserStatus(user.getUserId(), newStatusCode);
                // 刷新表格显示
                userTable.refresh();
            }
        });
        
        createTimeColumn.setCellValueFactory(data -> {
            String timeStr = "";
            if (data.getValue().getUpdateBy() != null) {
                timeStr = data.getValue().getUpdateTime();
            }
            return new SimpleStringProperty(timeStr);
        });
        createTimeColumn.setPrefWidth(150);
        createTimeColumn.setCellFactory(createCenteredCellFactory());
        
        // 操作列使用按钮
        actionColumn.setCellFactory(createActionCellFactory());
        
        // 设置表格可编辑
        userTable.setEditable(true);
        
        // 创建全选复选框
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setTooltip(new Tooltip("全选/全不选"));
        selectColumn.setGraphic(selectAllCheckBox);
        
        // 用于存储每行的选中状态
        selectedList = FXCollections.observableArrayList();
        
        // 全选/全不选功能
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (SimpleBooleanProperty prop : selectedList) {
                prop.set(newVal);
            }
            userTable.refresh();
        });
        
        // 复选框列绑定
        selectColumn.setCellValueFactory(param -> {
            int index = userTable.getItems().indexOf(param.getValue());
            if (index >= 0 && index < selectedList.size()) {
                return selectedList.get(index);
            } else {
                return new SimpleBooleanProperty(false);
            }
        });
        
        selectColumn.setCellFactory(col -> new TableCell<UserInfoPO, Boolean>() {
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
                setAlignment(Pos.CENTER);
            }
        });
        
        // 添加复选框列到表格
        userTable.getColumns().add(0, selectColumn);
        
        // 初始化选中状态列表
        refreshSelectedList();
    }

    /**
     * 创建居中文本单元格工厂
     */
    private Callback<TableColumn<UserInfoPO, String>, TableCell<UserInfoPO, String>> createCenteredCellFactory() {
        return column -> new TableCell<UserInfoPO, String>() {
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
     * 创建操作按钮单元格工厂
     */
    private Callback<TableColumn<UserInfoPO, String>, TableCell<UserInfoPO, String>> createActionCellFactory() {
        return column -> new TableCell<UserInfoPO, String>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final Button resetPwdBtn = new Button();
            private final HBox buttonBox = new HBox(4, editBtn, resetPwdBtn, deleteBtn);
            
            {
                // 设置编辑按钮样式
                javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
                editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                editIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                editIcon.setScaleX(0.6);
                editIcon.setScaleY(0.6);
                editBtn.setGraphic(editIcon);
                editBtn.setTooltip(new Tooltip("编辑"));
                editBtn.setMinWidth(24);
                editBtn.setPrefWidth(24);
                editBtn.setMaxWidth(24);
                editBtn.setMinHeight(24);
                editBtn.setPrefHeight(24);
                editBtn.setMaxHeight(24);
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent;"));
                editBtn.setOnAction(e -> {
                    UserInfoPO user = getTableView().getItems().get(getIndex());
                    showEditDialog(user);
                });
                
                // 设置重置密码按钮样式
                javafx.scene.shape.SVGPath pwdIcon = new javafx.scene.shape.SVGPath();
                pwdIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
                pwdIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                pwdIcon.setScaleX(0.6);
                pwdIcon.setScaleY(0.6);
                resetPwdBtn.setGraphic(pwdIcon);
                resetPwdBtn.setTooltip(new Tooltip("重置密码"));
                resetPwdBtn.setMinWidth(24);
                resetPwdBtn.setPrefWidth(24);
                resetPwdBtn.setMaxWidth(24);
                resetPwdBtn.setMinHeight(24);
                resetPwdBtn.setPrefHeight(24);
                resetPwdBtn.setMaxHeight(24);
                resetPwdBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                resetPwdBtn.setOnMouseEntered(e -> resetPwdBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;"));
                resetPwdBtn.setOnMouseExited(e -> resetPwdBtn.setStyle("-fx-background-color: transparent;"));
                resetPwdBtn.setOnAction(e -> {
                    UserInfoPO user = getTableView().getItems().get(getIndex());
                    handleResetPassword(user);
                });
                
                // 设置删除按钮样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.6);
                trashIcon.setScaleY(0.6);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setTooltip(new Tooltip("删除"));
                deleteBtn.setMinWidth(24);
                deleteBtn.setPrefWidth(24);
                deleteBtn.setMaxWidth(24);
                deleteBtn.setMinHeight(24);
                deleteBtn.setPrefHeight(24);
                deleteBtn.setMaxHeight(24);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnAction(e -> {
                    UserInfoPO user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
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
     * 初始化过滤器
     */
    private void initFilters() {
        // 角色过滤器
        roleFilterCombo.getItems().addAll("全部", "D", "M");
        roleFilterCombo.setValue("全部");
        roleFilterCombo.setOnAction(e -> applyFilters());
        
        // 状态过滤器
        statusFilterCombo.getItems().addAll("全部", "A", "O", "D");
        statusFilterCombo.setValue("全部");
        statusFilterCombo.setOnAction(e -> applyFilters());
        
        // 搜索框
        searchField.setPromptText("搜索用户名、真实姓名或邮箱...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(this::applyFilters);
        });
    }

    /**
     * 初始化按钮事件
     */
    private void initButtonEvents() {
        addUserBtn.setOnAction(e -> showAddUserDialog());
        batchDeleteBtn.setOnAction(e -> handleBatchDelete());
        refreshBtn.setOnAction(e -> refreshUserTable());
        assignPermissionBtn.setOnAction(e -> handleAssignPermission());
        viewPermissionsBtn.setOnAction(e -> handleViewPermissions());
    }

    /**
     * 应用过滤器
     */
    private void applyFilters() {
        String roleFilter = roleFilterCombo.getValue();
        String statusFilter = statusFilterCombo.getValue();
        String searchText = searchField.getText();
        
        List<UserInfoPO> allUsers = userInfoService.queryAllUsers();
        List<UserInfoPO> filteredUsers = new ArrayList<>();
        
        for (UserInfoPO user : allUsers) {
            boolean roleMatch = "全部".equals(roleFilter) || 
                              ("D".equals(roleFilter) && "D".equals(user.getRoles())) ||
                              ("M".equals(roleFilter) && "M".equals(user.getRoles()));
            
            boolean statusMatch = "全部".equals(statusFilter) ||
                                ("A".equals(statusFilter) && "A".equals(user.getStatus())) ||
                                ("O".equals(statusFilter) && "O".equals(user.getStatus())) ||
                                ("D".equals(statusFilter) && "D".equals(user.getStatus()));
            
            boolean searchMatch = StringUtils.isBlank(searchText) ||
                                StringUtils.containsIgnoreCase(user.getUsername(), searchText) ||
                                StringUtils.containsIgnoreCase(user.getRealName(), searchText) ||
                                StringUtils.containsIgnoreCase(user.getEmail(), searchText);
            
            if (roleMatch && statusMatch && searchMatch) {
                filteredUsers.add(user);
            }
        }
        
        userTable.getItems().clear();
        userTable.getItems().addAll(filteredUsers);
        refreshSelectedList();
    }

    /**
     * 刷新用户表格
     */
    private void refreshUserTable() {
        userTable.getItems().clear();
        List<UserInfoPO> users = userInfoService.queryAllUsers();
        userTable.getItems().addAll(users);
        refreshSelectedList();
        
        // 重置过滤器
        roleFilterCombo.setValue("全部");
        statusFilterCombo.setValue("全部");
        searchField.clear();
    }

    /**
     * 显示添加用户对话框
     */
    private void showAddUserDialog() {
        // 创建添加用户对话框
        Dialog<UserInfoPO> dialog = new Dialog<>();
        dialog.setTitle("添加用户");
        dialog.setHeaderText("请输入用户信息");
        dialog.setResizable(true);
        
        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // 创建表单内容
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
        
        // 设置列约束
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(80);
        labelColumn.setPrefWidth(100);
        
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(200);
        inputColumn.setPrefWidth(250);
        
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);
        
        // 创建输入字段
        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        
        TextField passwordField = new TextField();
        passwordField.setPromptText("请输入密码");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        
        TextField realNameField = new TextField();
        realNameField.setPromptText("请输入真实姓名");
        realNameField.setMaxWidth(Double.MAX_VALUE);
        
        TextField emailField = new TextField();
        emailField.setPromptText("请输入邮箱地址");
        emailField.setMaxWidth(Double.MAX_VALUE);
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("请输入电话号码");
        phoneField.setMaxWidth(Double.MAX_VALUE);
        
        // 角色下拉框
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("D-开发者", "M-管理者");
        roleComboBox.setValue("D-开发者"); // 默认选择开发者
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        
        // 状态下拉框
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("A-申请", "O-启用", "D-停止");
        statusComboBox.setValue("O-启用"); // 默认选择启用
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        
        TextField remarkField = new TextField();
        remarkField.setPromptText("请输入备注信息");
        remarkField.setMaxWidth(Double.MAX_VALUE);
        
        // 添加字段到网格
        grid.add(new Label("用户名:"), 0, 0);
        grid.add(usernameField, 1, 0);
        
        grid.add(new Label("密码:"), 0, 1);
        grid.add(passwordField, 1, 1);
        
        grid.add(new Label("真实姓名:"), 0, 2);
        grid.add(realNameField, 1, 2);
        
        grid.add(new Label("邮箱:"), 0, 3);
        grid.add(emailField, 1, 3);
        
        grid.add(new Label("电话:"), 0, 4);
        grid.add(phoneField, 1, 4);
        
        grid.add(new Label("角色:"), 0, 5);
        grid.add(roleComboBox, 1, 5);
        
        grid.add(new Label("状态:"), 0, 6);
        grid.add(statusComboBox, 1, 6);
        
        grid.add(new Label("备注:"), 0, 7);
        grid.add(remarkField, 1, 7);
        
        // 设置对话框内容
        dialog.getDialogPane().setContent(grid);
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 验证必填字段
                if (usernameField.getText().trim().isEmpty()) {
                    ViewUtils.alertForFail("用户名不能为空！");
                    return null;
                }
                if (passwordField.getText().trim().isEmpty()) {
                    ViewUtils.alertForFail("密码不能为空！");
                    return null;
                }
                if (realNameField.getText().trim().isEmpty()) {
                    ViewUtils.alertForFail("真实姓名不能为空！");
                    return null;
                }
                
                // 检查用户名是否已存在
                if (userInfoService.isUsernameExists(usernameField.getText().trim())) {
                    ViewUtils.alertForFail("用户名已存在，请选择其他用户名！");
                    return null;
                }
                
                // 创建新的用户对象
                UserInfoPO newUser = new UserInfoPO();
                newUser.setUsername(usernameField.getText().trim());
                newUser.setPassword(passwordField.getText().trim());
                newUser.setRealName(realNameField.getText().trim());
                newUser.setEmail(emailField.getText().trim());
                newUser.setPhone(phoneField.getText().trim());
                
                // 设置角色（提取代码部分）
                String selectedRole = roleComboBox.getValue();
                if (selectedRole != null) {
                    String roleCode = selectedRole.substring(0, 1);
                    newUser.setRoles(roleCode);
                }
                
                // 设置状态（提取代码部分）
                String selectedStatus = statusComboBox.getValue();
                if (selectedStatus != null) {
                    String statusCode = selectedStatus.substring(0, 1);
                    newUser.setStatus(statusCode);
                }
                
                newUser.setRemark(remarkField.getText().trim());
                
                return newUser;
            }
            return null;
        });
        
        // 显示对话框并处理结果
        Optional<UserInfoPO> result = dialog.showAndWait();
        result.ifPresent(newUser -> {
            try {
                // 保存到数据库
                userInfoService.saveUser(newUser);
                
                // 刷新用户表格
                refreshUserTable();
                
                // 显示成功消息
                ViewUtils.alertForSucess("用户已成功添加！");
                
            } catch (Exception e) {
                e.printStackTrace();
                ViewUtils.alertForFail("添加用户时发生错误: " + e.getMessage());
            }
        });
    }

    /**
     * 显示编辑用户对话框
     */
    private void showEditDialog(UserInfoPO user) {
        ViewUtils.alertForAsk("","编辑用户功能待实现");
    }

    /**
     * 处理删除用户
     */
    private void handleDeleteUser(UserInfoPO user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除用户 '" + user.getUsername() + "' 吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userInfoService.deleteUser(user.getUserId());
                ViewUtils.alertForSucess("删除成功！");
                refreshUserTable();
            }
        });
    }

    /**
     * 处理重置密码
     */
    private void handleResetPassword(UserInfoPO user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置密码");
        alert.setHeaderText(null);
        alert.setContentText("确定要重置用户 '" + user.getUsername() + "' 的密码吗？\n新密码将设置为: 123456");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userInfoService.updateUserPassword(user.getUserId(), "123456");
                ViewUtils.alertForSucess("密码重置成功！新密码为: 123456");
            }
        });
    }

    /**
     * 处理批量删除
     */
    private void handleBatchDelete() {
        // 获取选中的用户
        List<UserInfoPO> selectedUsers = new ArrayList<>();
        for (int i = 0; i < userTable.getItems().size(); i++) {
            if (i < selectedList.size() && selectedList.get(i).get()) {
                UserInfoPO user = userTable.getItems().get(i);
                selectedUsers.add(user);
            }
        }
        
        if (selectedUsers.isEmpty()) {
            ViewUtils.alertForFail("请先选择要删除的用户！");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认批量删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除选中的 " + selectedUsers.size() + " 个用户吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<String> userIds = new ArrayList<>();
                for (UserInfoPO user : selectedUsers) {
                    userIds.add(user.getUserId());
                }
                
                userInfoService.batchDeleteUsers(userIds);
                ViewUtils.alertForSucess("批量删除成功！");
                refreshUserTable();
            }
        });
    }

    /**
     * 刷新选中状态列表
     */
    private void refreshSelectedList() {
        if (selectedList != null) {
            selectedList.clear();
            for (int i = 0; i < userTable.getItems().size(); i++) {
                selectedList.add(new SimpleBooleanProperty(false));
            }
        }
    }

    /**
     * 更新全选复选框状态
     */
    private void updateSelectAllCheckBox(CheckBox selectAllCheckBox) {
        if (selectedList == null) return;
        
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

    /**
     * 获取角色显示名称
     */
    private String getRoleDisplayName(String role) {
        return UserRoleEnum.getDescByCode(role);
    }

    /**
     * 获取复选框选中的用户列表
     */
    private List<UserInfoPO> getSelectedUsersFromCheckbox() {
        List<UserInfoPO> selectedUsers = new ArrayList<>();
        for (int i = 0; i < userTable.getItems().size(); i++) {
            if (i < selectedList.size() && selectedList.get(i).get()) {
                UserInfoPO user = userTable.getItems().get(i);
                selectedUsers.add(user);
            }
        }
        return selectedUsers;
    }

    /**
     * 处理分配权限
     */
    private void handleAssignPermission() {
        // 获取复选框选中的用户
        List<UserInfoPO> selectedUsers = getSelectedUsersFromCheckbox();
        if (selectedUsers.isEmpty()) {
            ViewUtils.alertForFail("请先选择要分配权限的用户！");
            return;
        }
        if (selectedUsers.size() > 1) {
            ViewUtils.alertForFail("分配权限时只能选择一个用户！");
            return;
        }
        
        // 显示权限分配对话框
        showPermissionAssignDialog(selectedUsers.get(0));
    }

    /**
     * 处理查看权限
     */
    private void handleViewPermissions() {
        // 获取复选框选中的用户
        List<UserInfoPO> selectedUsers = getSelectedUsersFromCheckbox();
        if (selectedUsers.isEmpty()) {
            ViewUtils.alertForFail("请先选择要查看权限的用户！");
            return;
        }
        if (selectedUsers.size() > 1) {
            ViewUtils.alertForFail("查看权限时只能选择一个用户！");
            return;
        }
        
        // 显示权限查看对话框
        showPermissionViewDialog(selectedUsers.get(0));
    }

    /**
     * 显示权限分配对话框
     */
    private void showPermissionAssignDialog(UserInfoPO user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("用户权限分配");
        dialog.setHeaderText("为用户 '" + user.getUsername() + "' 分配项目权限");
        dialog.setResizable(true);

        // 设置对话框按钮
        ButtonType assignButtonType = new ButtonType("分配", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(assignButtonType, cancelButtonType);

        // 创建表单内容
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));

        // 设置列约束
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(80);
        labelColumn.setPrefWidth(100);
        
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setMinWidth(200);
        inputColumn.setPrefWidth(250);
        
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);

        // 创建项目组下拉框
        ComboBox<String> projectGroupCombo = new ComboBox<>();
        List<UserProjGroupPO> groups = userProjGroupService.queryUserProjGroups(new UserProjGroupPO());
        for (UserProjGroupPO group : groups) {
            projectGroupCombo.getItems().add(group.getGroupName());
        }
        projectGroupCombo.setMaxWidth(Double.MAX_VALUE);
        
        // 设置默认选择为 GlobalProperties.groupName
        if (globalProperties.getGroupName() != null && !globalProperties.getGroupName().trim().isEmpty()) {
            projectGroupCombo.setValue(globalProperties.getGroupName());
        }

        // 创建项目下拉框
        ComboBox<String> projectCombo = new ComboBox<>();
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        projectCombo.setDisable(true); // 初始禁用

        // 监听项目组选择变化，更新项目列表
        projectGroupCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            projectCombo.getItems().clear();
            projectCombo.setDisable(newVal == null);
            
            if (newVal != null) {
                List<UserProjSettingPO> projects = userProjSettingService.queryByGroupName(newVal);
                for (UserProjSettingPO project : projects) {
                    projectCombo.getItems().add(project.getProjectName() + " (" + project.getAppName() + ")");
                }
                if (!projectCombo.getItems().isEmpty()) {
                    projectCombo.getSelectionModel().selectFirst();
                }
            }
        });
        
        // 如果设置了默认项目组，手动触发项目列表更新
        if (globalProperties.getGroupName() != null && !globalProperties.getGroupName().trim().isEmpty()) {
            String selectedGroup = globalProperties.getGroupName();
            if (projectGroupCombo.getItems().contains(selectedGroup)) {
                projectCombo.getItems().clear();
                projectCombo.setDisable(false);
                
                List<UserProjSettingPO> projects = userProjSettingService.queryByGroupName(selectedGroup);
                for (UserProjSettingPO project : projects) {
                    projectCombo.getItems().add(project.getProjectName() + " (" + project.getAppName() + ")");
                }
                if (!projectCombo.getItems().isEmpty()) {
                    projectCombo.getSelectionModel().selectFirst();
                }
            }
        }

        // 添加字段到网格
        grid.add(new Label("用户:"), 0, 0);
        TextField userField = new TextField(user.getUsername() + " (" + user.getRealName() + ")");
        userField.setEditable(false);
        userField.setMaxWidth(Double.MAX_VALUE);
        grid.add(userField, 1, 0);
        
        grid.add(new Label("项目组:"), 0, 1);
        grid.add(projectGroupCombo, 1, 1);
        
        grid.add(new Label("项目:"), 0, 2);
        grid.add(projectCombo, 1, 2);

        // 设置对话框内容
        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == assignButtonType) {
                String selectedGroup = projectGroupCombo.getValue();
                String selectedProject = projectCombo.getValue();
                
                if (selectedGroup == null) {
                    ViewUtils.alertForFail("请选择项目组！");
                    return null;
                }
                
                if (selectedProject == null) {
                    ViewUtils.alertForFail("请选择项目！");
                    return null;
                }
                
                // 分配项目权限
                assignProjectPermission(user, selectedGroup, selectedProject);
            }
            return null;
        });

        // 显示对话框
        dialog.showAndWait();
    }

    /**
     * 分配项目组权限
     */
    private void assignGroupPermission(UserInfoPO user, String groupName) {
        try {
            // 检查是否已有权限
            if (userProjGroupService.hasGroupPermission(user.getUserId(), user.getUsername(), groupName)) {
                ViewUtils.alertForFail("该用户已有此项目组权限！");
                return;
            }
            
            // 获取项目组信息
            UserProjGroupPO projectGroup = userProjGroupService.queryByGroupName(groupName);
            if (projectGroup == null) {
                ViewUtils.alertForFail("项目组不存在！");
                return;
            }
            
            // 创建用户项目组权限
            UserProjGroupPO userProjGroup = new UserProjGroupPO();
            userProjGroup.setGroupName(groupName);
            userProjGroup.setGroupDesc(projectGroup.getGroupDesc());
            userProjGroup.setUserId(user.getUserId());
            userProjGroup.setUsername(user.getUsername());
            userProjGroup.setCurFlag("N");
            
            userProjGroupService.saveUserProjGroup(userProjGroup);
            ViewUtils.alertForSucess("项目组权限分配成功！");
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("分配项目组权限失败: " + e.getMessage());
        }
    }

    /**
     * 内部分配项目组权限（不显示提示信息）
     */
    private void assignGroupPermissionInternal(UserInfoPO user, String groupName) throws Exception {
        // 获取项目组信息
        UserProjGroupPO projectGroup = userProjGroupService.queryByGroupName(groupName);
        if (projectGroup == null) {
            throw new Exception("项目组不存在！");
        }
        
        // 创建用户项目组权限
        UserProjGroupPO userProjGroup = new UserProjGroupPO();
        userProjGroup.setGroupName(groupName);
        userProjGroup.setGroupDesc(projectGroup.getGroupDesc());
        userProjGroup.setUserId(user.getUserId());
        userProjGroup.setUsername(user.getUsername());
        userProjGroup.setCurFlag("N");
        
        userProjGroupService.saveUserProjGroup(userProjGroup);
    }

    /**
     * 分配项目权限
     */
    private void assignProjectPermission(UserInfoPO user, String groupName, String projectDisplay) {
        try {
            // 解析项目信息
            String projectName = projectDisplay.split(" \\(")[0];
            String appName = projectDisplay.split(" \\(")[1].replace(")", "");
            
            // 检查是否已有项目权限
            if (userProjSettingService.hasProjectPermission(user.getUserId(), user.getUsername(), groupName, projectName, appName)) {
                ViewUtils.alertForFail("该用户已有此项目权限！");
                return;
            }
            
            // 检查并确保用户有项目组权限
            if (!userProjGroupService.hasGroupPermission(user.getUserId(), user.getUsername(), groupName)) {
                // 用户没有该项目组权限，先分配项目组权限
                assignGroupPermissionInternal(user, groupName);
            }
            
            // 获取项目设置信息
            UserProjSettingPO queryPo = new UserProjSettingPO();
            queryPo.setGroupName(groupName);
            queryPo.setProjectName(projectName);
            List<UserProjSettingPO> projectSettings = userProjSettingService.queryUserProjSettings(queryPo);
            UserProjSettingPO projectSetting = projectSettings.isEmpty() ? null : projectSettings.get(0);
            if (projectSetting == null) {
                ViewUtils.alertForFail("项目不存在！");
                return;
            }
            
            // 创建用户项目权限
            UserProjSettingPO userProjSetting = new UserProjSettingPO();
            userProjSetting.setGroupName(groupName);
            userProjSetting.setProjectName(projectName);
            userProjSetting.setProjectType(projectSetting.getProjectType() != null ? projectSetting.getProjectType() : "");
            userProjSetting.setProjectDesc(projectSetting.getProjectDesc() != null ? projectSetting.getProjectDesc() : "");
            userProjSetting.setAppName(appName);
            userProjSetting.setAppPort(projectSetting.getAppPort() != null ? projectSetting.getAppPort() : "");
            userProjSetting.setUserId(user.getUserId());
            userProjSetting.setUsername(user.getUsername());
            userProjSetting.setSchemaNm(projectSetting.getSchemaNm() != null ? projectSetting.getSchemaNm() : "");
            userProjSetting.setBasePath(projectSetting.getBasePath() != null ? projectSetting.getBasePath() : "");
            userProjSetting.setPropPath(projectSetting.getPropPath() != null ? projectSetting.getPropPath() : "");
            userProjSetting.setEnumPath(projectSetting.getEnumPath() != null ? projectSetting.getEnumPath() : "");
            userProjSetting.setMsgcdPath(projectSetting.getMsgcdPath() != null ? projectSetting.getMsgcdPath() : "");
            userProjSetting.setCurFlag("N");
            userProjSetting.setShowFlag("Y");
            userProjSetting.setUpdateBy(user.getUsername());
            userProjSetting.setUpdateTime(java.time.LocalDateTime.now().toString());
            
            userProjSettingService.saveUserProjSetting(userProjSetting);
            ViewUtils.alertForSucess("项目权限分配成功！");
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("分配项目权限失败: " + e.getMessage());
        }
    }

    /**
     * 显示权限查看对话框
     */
    private void showPermissionViewDialog(UserInfoPO user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("用户权限查看");
        dialog.setHeaderText("用户 '" + user.getUsername() + "' 的权限详情");
        dialog.setResizable(true);
        // 设置对话框尺寸
        dialog.getDialogPane().setMinWidth(900);
        dialog.getDialogPane().setMinHeight(650);

        // 设置对话框按钮
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);

        // 创建左右分栏布局
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35); // 左侧占35%，右侧占65%
        
        // 创建左右面板的引用，用于交互
        VBox[] leftPaneRef = new VBox[1];
        VBox[] rightPaneRef = new VBox[1];
        
        // 左侧：项目组列表
        leftPaneRef[0] = createGroupListPane(user, rightPaneRef);
        
        // 右侧：项目权限列表
        rightPaneRef[0] = createProjectPermissionsPane(user);
        
        splitPane.getItems().addAll(leftPaneRef[0], rightPaneRef[0]);
        
        dialog.getDialogPane().setContent(splitPane);
        dialog.showAndWait();
    }

    /**
     * 创建左侧项目组列表面板
     */
    private VBox createGroupListPane(UserInfoPO user, VBox[] rightPaneRef) {
        VBox container = new VBox(10);
        container.setPadding(new javafx.geometry.Insets(15, 15, 15, 15));
        
        // 标题
        Label titleLabel = new Label("项目组权限");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        container.getChildren().add(titleLabel);
        
        // 用户信息
        VBox userInfoBox = new VBox(3);
        userInfoBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-background-radius: 4;");
        userInfoBox.getChildren().addAll(
            new Label("用户: " + user.getUsername()),
            new Label("姓名: " + user.getRealName())
        );
        container.getChildren().add(userInfoBox);
        
        // 创建项目组表格
        TableView<UserProjGroupPO> groupTable = new TableView<>();
        
        // 项目组名称列
        TableColumn<UserProjGroupPO, String> groupNameColumn = new TableColumn<>("项目组");
        groupNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGroupName()));
        groupNameColumn.setCellFactory(createCenteredCellFactoryForGroup());
        groupNameColumn.setPrefWidth(120);
        
        // 项目组描述列
        TableColumn<UserProjGroupPO, String> groupDescColumn = new TableColumn<>("描述");
        groupDescColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGroupDesc()));
        groupDescColumn.setCellFactory(createCenteredCellFactoryForGroup());
        groupDescColumn.setPrefWidth(100);
        
        groupTable.getColumns().addAll(groupNameColumn, groupDescColumn);
        
        // 加载数据
        List<UserProjGroupPO> groupPermissions = userProjGroupService.getUserGroupPermissions(user.getUserId(), user.getUsername());
        groupTable.getItems().setAll(groupPermissions);
        
        // 设置表格样式
        groupTable.setStyle("-fx-font-size: 12px;");
        groupTable.setPrefHeight(400);
        
        // 选中监听器
        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // 更新右侧面板中的项目权限列表
            updateProjectPermissionsPane(newVal, user, rightPaneRef[0]);
        });
        
        container.getChildren().add(groupTable);
        
        // 统计信息
        Label countLabel = new Label("共 " + groupPermissions.size() + " 个项目组");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        container.getChildren().add(countLabel);
        
        return container;
    }
    
    /**
     * 创建右侧项目权限面板
     */
    private VBox createProjectPermissionsPane(UserInfoPO user) {
        VBox container = new VBox(10);
        container.setPadding(new javafx.geometry.Insets(15, 15, 15, 15));
        
        // 标题
        Label titleLabel = new Label("项目权限");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        container.getChildren().add(titleLabel);
        
        // 提示信息
        Label hintLabel = new Label("请从左侧选择项目组查看项目权限");
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
        hintLabel.getStyleClass().add("hint");
        container.getChildren().add(hintLabel);
        
        // 创建项目权限表格
        TableView<UserProjSettingPO> projectTable = new TableView<>();
        
        // 项目名称列
        TableColumn<UserProjSettingPO, String> projectNameColumn = new TableColumn<>("项目名称");
        projectNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProjectName()));
        projectNameColumn.setCellFactory(createCenteredCellFactoryForProject());
        projectNameColumn.setPrefWidth(120);
        
        // 应用名称列
        TableColumn<UserProjSettingPO, String> appNameColumn = new TableColumn<>("应用名称");
        appNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAppName()));
        appNameColumn.setCellFactory(createCenteredCellFactoryForProject());
        appNameColumn.setPrefWidth(100);
        
        // 当前标志列
        TableColumn<UserProjSettingPO, String> curFlagColumn = new TableColumn<>("当前标志");
        curFlagColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCurFlag()));
        curFlagColumn.setCellFactory(createCenteredCellFactoryForProject());
        curFlagColumn.setPrefWidth(80);
        
        // 操作列
        TableColumn<UserProjSettingPO, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(80);
        actionColumn.setCellFactory(col -> {
            TableCell<UserProjSettingPO, Void> cell = new TableCell<UserProjSettingPO, Void>() {
                private final Button delBtn = new Button();
                {
                    // 设置删除按钮样式 - 使用SVG图标样式
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
                        UserProjSettingPO project = getTableView().getItems().get(getIndex());
                        handleDeleteProjectPermission(project, projectTable, user);
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
            };
            return cell;
        });
        
        projectTable.getColumns().addAll(projectNameColumn, appNameColumn, curFlagColumn, actionColumn);
        
        // 设置表格样式
        projectTable.setStyle("-fx-font-size: 12px;");
        projectTable.setPrefHeight(400);
        
        container.getChildren().add(projectTable);
        
        // 统计信息
        Label countLabel = new Label("共 0 个项目");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        countLabel.setId("projectCountLabel"); // 设置ID用于后续更新
        
        container.getChildren().add(countLabel);
        
        return container;
    }
    
    /**
     * 更新项目权限面板（当选中项目组时调用）
     */
    private void updateProjectPermissionsPane(UserProjGroupPO selectedGroup, UserInfoPO user, VBox rightPane) {
        if (selectedGroup == null) {
            return;
        }
        
        // 查找右侧面板中的表格和标签
        TableView<UserProjSettingPO> projectTable = null;
        Label countLabel = null;
        
        for (javafx.scene.Node node : rightPane.getChildren()) {
            if (node instanceof TableView) {
                @SuppressWarnings("unchecked")
                TableView<UserProjSettingPO> tableView = (TableView<UserProjSettingPO>) node;
                projectTable = tableView;
            } else if (node instanceof Label && node.getId() != null && node.getId().equals("projectCountLabel")) {
                countLabel = (Label) node;
            }
        }
        
        if (projectTable != null) {
            // 查询该项目组下的项目权限
            List<UserProjSettingPO> projectPermissions = userProjSettingService.getUserProjectPermissionsByGroup(
                user.getUserId(), user.getUsername(), selectedGroup.getGroupName());
            
            // 更新表格数据
            projectTable.getItems().setAll(projectPermissions);
            
            // 更新统计信息
            if (countLabel != null) {
                countLabel.setText("共 " + projectPermissions.size() + " 个项目");
            }
            
            // 更新提示信息
            for (javafx.scene.Node node : rightPane.getChildren()) {
                if (node instanceof Label && node.getStyleClass().contains("hint")) {
                    Label hintLabel = (Label) node;
                    hintLabel.setText("项目组 '" + selectedGroup.getGroupName() + "' 下的项目权限");
                    hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
                }
            }
        }
    }

    /**
     * 创建项目组权限视图
     */
    private VBox createGroupPermissionsView(UserInfoPO user) {
        VBox container = new VBox(10);
        
        // 获取用户的项目组权限
        List<UserProjGroupPO> userGroups = userProjGroupService.queryByUserId(user.getUserId());
        
        if (userGroups.isEmpty()) {
            Label noDataLabel = new Label("该用户暂无项目组权限");
            noDataLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            container.getChildren().add(noDataLabel);
            return container;
        }
        
        // 创建项目组权限表格
        TableView<UserProjGroupPO> groupTable = new TableView<>();
        groupTable.setPrefHeight(300);
        
        // 创建列
        TableColumn<UserProjGroupPO, String> groupNameColumn = new TableColumn<>("项目组名称");
        TableColumn<UserProjGroupPO, String> groupDescColumn = new TableColumn<>("项目组描述");
        TableColumn<UserProjGroupPO, String> curFlagColumn = new TableColumn<>("当前标志");
        TableColumn<UserProjGroupPO, String> actionColumn = new TableColumn<>("操作");
        
        // 设置列宽
        groupNameColumn.setPrefWidth(150);
        groupDescColumn.setPrefWidth(200);
        curFlagColumn.setPrefWidth(80);
        actionColumn.setPrefWidth(100);
        
        // 设置数据绑定
        groupNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGroupName()));
        groupDescColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGroupDesc()));
        curFlagColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty("Y".equals(data.getValue().getCurFlag()) ? "是" : "否"));
        curFlagColumn.setCellFactory(createCenteredCellFactoryForGroup());
        
        // 设置操作列
        actionColumn.setCellFactory(createGroupPermissionActionCellFactory(groupTable, user));
        
        // 添加列到表格
        groupTable.getColumns().addAll(groupNameColumn, groupDescColumn, curFlagColumn, actionColumn);
        
        // 加载数据
        groupTable.getItems().addAll(userGroups);
        
        // 添加统计信息
        Label statsLabel = new Label("共有 " + userGroups.size() + " 个项目组权限");
        statsLabel.setStyle("-fx-font-weight: bold;");
        
        container.getChildren().addAll(statsLabel, groupTable);
        
        return container;
    }

    /**
     * 创建项目权限视图
     */
    private VBox createProjectPermissionsView(UserInfoPO user) {
        VBox container = new VBox(10);
        
        // 获取用户的项目权限
        List<UserProjSettingPO> userProjects = userProjSettingService.queryByUserId(user.getUserId());
        
        if (userProjects.isEmpty()) {
            Label noDataLabel = new Label("该用户暂无项目权限");
            noDataLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            container.getChildren().add(noDataLabel);
            return container;
        }
        
        // 创建项目权限表格
        TableView<UserProjSettingPO> projectTable = new TableView<>();
        projectTable.setPrefHeight(300);
        
        // 创建列
        TableColumn<UserProjSettingPO, String> groupNameColumn = new TableColumn<>("项目组");
        TableColumn<UserProjSettingPO, String> projectNameColumn = new TableColumn<>("项目名称");
        TableColumn<UserProjSettingPO, String> appNameColumn = new TableColumn<>("应用名称");
        TableColumn<UserProjSettingPO, String> curFlagColumn = new TableColumn<>("当前项目");
        TableColumn<UserProjSettingPO, String> showFlagColumn = new TableColumn<>("显示");
        TableColumn<UserProjSettingPO, String> actionColumn = new TableColumn<>("操作");
        
        // 设置列宽
        groupNameColumn.setPrefWidth(120);
        projectNameColumn.setPrefWidth(150);
        appNameColumn.setPrefWidth(120);
        curFlagColumn.setPrefWidth(80);
        showFlagColumn.setPrefWidth(60);
        actionColumn.setPrefWidth(100);
        
        // 设置数据绑定
        groupNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGroupName()));
        projectNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getProjectName()));
        appNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getAppName()));
        curFlagColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty("Y".equals(data.getValue().getCurFlag()) ? "是" : "否"));
        curFlagColumn.setCellFactory(createCenteredCellFactoryForProject());
        showFlagColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty("Y".equals(data.getValue().getShowFlag()) ? "是" : "否"));
        showFlagColumn.setCellFactory(createCenteredCellFactoryForProject());
        
        // 设置操作列
        actionColumn.setCellFactory(createProjectPermissionActionCellFactory(projectTable, user));
        
        // 添加列到表格
        projectTable.getColumns().addAll(groupNameColumn, projectNameColumn, appNameColumn, 
                                       curFlagColumn, showFlagColumn, actionColumn);
        
        // 加载数据
        projectTable.getItems().addAll(userProjects);
        
        // 添加统计信息
        Label statsLabel = new Label("共有 " + userProjects.size() + " 个项目权限");
        statsLabel.setStyle("-fx-font-weight: bold;");
        
        // 按项目组统计
        Map<String, Long> groupStats = userProjects.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                UserProjSettingPO::getGroupName, 
                java.util.stream.Collectors.counting()
            ));
        
        VBox statsBox = new VBox(5);
        statsBox.getChildren().add(statsLabel);
        for (Map.Entry<String, Long> entry : groupStats.entrySet()) {
            Label groupStatLabel = new Label("  - " + entry.getKey() + ": " + entry.getValue() + " 个项目");
            groupStatLabel.setStyle("-fx-text-fill: #666;");
            statsBox.getChildren().add(groupStatLabel);
        }
        
        container.getChildren().addAll(statsBox, projectTable);
        
        return container;
    }

    /**
     * 创建项目组权限居中文本单元格工厂
     */
    private <T> Callback<TableColumn<UserProjGroupPO, String>, TableCell<UserProjGroupPO, String>> createCenteredCellFactoryForGroup() {
        return column -> new TableCell<UserProjGroupPO, String>() {
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
     * 创建项目权限居中文本单元格工厂
     */
    private <T> Callback<TableColumn<UserProjSettingPO, String>, TableCell<UserProjSettingPO, String>> createCenteredCellFactoryForProject() {
        return column -> new TableCell<UserProjSettingPO, String>() {
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
     * 创建项目组权限操作按钮单元格工厂
     */
    private Callback<TableColumn<UserProjGroupPO, String>, TableCell<UserProjGroupPO, String>> createGroupPermissionActionCellFactory(TableView<UserProjGroupPO> table, UserInfoPO user) {
        return column -> new TableCell<UserProjGroupPO, String>() {
            private final Button deleteBtn = new Button();
            private final HBox buttonBox = new HBox(4, deleteBtn);
            
            {
                // 设置删除按钮样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.6);
                trashIcon.setScaleY(0.6);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setTooltip(new Tooltip("删除权限"));
                deleteBtn.setMinWidth(24);
                deleteBtn.setPrefWidth(24);
                deleteBtn.setMaxWidth(24);
                deleteBtn.setMinHeight(24);
                deleteBtn.setPrefHeight(24);
                deleteBtn.setMaxHeight(24);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnAction(e -> {
                    UserProjGroupPO group = getTableView().getItems().get(getIndex());
                    handleDeleteGroupPermission(group, table, user);
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
     * 创建项目权限操作按钮单元格工厂
     */
    private Callback<TableColumn<UserProjSettingPO, String>, TableCell<UserProjSettingPO, String>> createProjectPermissionActionCellFactory(TableView<UserProjSettingPO> table, UserInfoPO user) {
        return column -> new TableCell<UserProjSettingPO, String>() {
            private final Button deleteBtn = new Button();
            private final HBox buttonBox = new HBox(4, deleteBtn);
            
            {
                // 设置删除按钮样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.6);
                trashIcon.setScaleY(0.6);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setTooltip(new Tooltip("删除权限"));
                deleteBtn.setMinWidth(24);
                deleteBtn.setPrefWidth(24);
                deleteBtn.setMaxWidth(24);
                deleteBtn.setMinHeight(24);
                deleteBtn.setPrefHeight(24);
                deleteBtn.setMaxHeight(24);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnAction(e -> {
                    UserProjSettingPO project = getTableView().getItems().get(getIndex());
                    handleDeleteProjectPermission(project, table, user);
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
     * 处理删除项目组权限（在权限查看对话框中）
     */
    private void handleDeleteGroupPermission(UserProjGroupPO group, TableView<UserProjGroupPO> table, UserInfoPO user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除用户 '" + user.getUsername() + "' 对项目组 '" + group.getGroupName() + "' 的权限吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userProjGroupService.deleteUserProjGroup(group.getGroupName(), group.getUserId(), group.getUsername());
                    ViewUtils.alertForSucess("删除成功！");
                    
                    // 刷新表格
                    table.getItems().clear();
                    List<UserProjGroupPO> userGroups = userProjGroupService.queryByUserId(user.getUserId());
                    table.getItems().addAll(userGroups);
                } catch (Exception e) {
                    e.printStackTrace();
                    ViewUtils.alertForFail("删除失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 处理删除项目权限（在权限查看对话框中）
     */
    private void handleDeleteProjectPermission(UserProjSettingPO project, TableView<UserProjSettingPO> table, UserInfoPO user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除用户 '" + user.getUsername() + "' 对项目 '" + project.getProjectName() + "' 的权限吗？");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userProjSettingService.deleteUserProjSetting(project.getGroupName(), project.getProjectName(), 
                                                                 project.getAppName(), project.getUserId(), project.getUsername());
                    ViewUtils.alertForSucess("删除成功！");
                    
                    // 刷新表格
                    table.getItems().clear();
                    List<UserProjSettingPO> userProjects = userProjSettingService.queryByUserId(user.getUserId());
                    table.getItems().addAll(userProjects);
                } catch (Exception e) {
                    e.printStackTrace();
                    ViewUtils.alertForFail("删除失败: " + e.getMessage());
                }
            }
        });
    }


}
