package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.MenuCatalog;
import com.murong.ecp.tools.fx.domain.service.common.EnvironmentService;
import com.murong.ecp.tools.fx.domain.service.common.UserPreferenceService;
import com.murong.ecp.tools.fx.enums.MenuEnum;
import com.murong.ecp.tools.fx.enums.UserRoleEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.RoleMenuPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.RoleMenuRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.WorkspaceBootstrapVO;
import com.murong.ecp.tools.fx.infrastructure.rpc.WorkspaceRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.DoubleClickTabPane;
import com.murong.ecp.tools.fx.infrastructure.view.LoginDialog;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MainController {
    @FXML private VBox menuBar;
    @FXML private VBox menuGroupContainer;
    @FXML private StackPane mainContent;
    @FXML private TabPane pageContainer;
    private DoubleClickTabPane doubleClickTabPane;
    @FXML private ImageView logoImage;
    @FXML private Button toggleMenuBtn;
    @FXML private ComboBox<String> envComboBox;
    @FXML private ComboBox<String> dsComboBox;
    @FXML private Label usernameLabel;
    @FXML private Button logoutBtn;
    private boolean menuCollapsed = false;
    
    // 防止刷新时触发项目切换的标志
    private boolean isRefreshingProjectComboBox = false;
    private boolean isReloadingWorkspace = false;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private WorkspaceRpcService workspaceRpcService;
    @Autowired
    private UserProjSettingRpcService userProjSettingRpcService;
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private LocalSettingDao localSettingDao;
    @Autowired
    private RoleMenuRpcService roleMenuRpcService;
    
    // 保存主窗口的Stage引用
    private Stage mainStage;

    // 二级菜单结构定义
    private static class MenuGroup {
        String groupKey;
        String groupName;
        String groupIcon;
        MenuItem[] children;
        MenuGroup(String groupKey, String groupName, String groupIcon, MenuItem[] children) {
            this.groupKey = groupKey;
            this.groupName = groupName;
            this.groupIcon = groupIcon;
            this.children = children;
        }
    }
    private static class MenuItem {
        String key;
        String text;
        String icon;
        String fxmlPath;
        MenuItem(String key, String text, String icon, String fxmlPath) {
            this.key = key;
            this.text = text;
            this.icon = icon;
            this.fxmlPath = fxmlPath;
        }
    }
    // 菜单分组与子菜单定义（与菜单管理共用 MenuCatalog）
    private final MenuGroup[] menuGroups = createAllMenuGroups();

    private static MenuGroup[] createAllMenuGroups() {
        List<MenuGroup> groups = new ArrayList<>();
        for (MenuCatalog.Group group : MenuCatalog.groups()) {
            MenuItem[] children = group.items().stream()
                    .map(item -> new MenuItem(item.key(), item.text(), item.icon(), item.fxmlPath()))
                    .toArray(MenuItem[]::new);
            groups.add(new MenuGroup(group.groupKey(), group.groupName(), group.groupIcon(), children));
        }
        return groups.toArray(new MenuGroup[0]);
    }
    private Button selectedBtn = null;

    // 支持的图标后缀
    private static final String[] ICON_SUFFIXES = {"-line.png", ".png"};

    @FXML
    public void initialize() {
        // 替换TabPane为DoubleClickTabPane
        doubleClickTabPane = new DoubleClickTabPane();
        doubleClickTabPane.setId("pageContainer");
        VBox.setVgrow(doubleClickTabPane, javafx.scene.layout.Priority.ALWAYS);
        
        // 找到pageContainer的父容器并替换
        if (pageContainer.getParent() instanceof VBox) {
            VBox parent = (VBox) pageContainer.getParent();
            int index = parent.getChildren().indexOf(pageContainer);
            if (index >= 0) {
                parent.getChildren().set(index, doubleClickTabPane);
            }
        }
        
        // LOGO图片
        logoImage.setImage(new Image(getClass().getResourceAsStream("/image/logo.png")));
        reloadWorkspaceSelectors();

        // 项目群下拉切换，刷新项目名称下拉
        envComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isReloadingWorkspace || newVal == null) {
                return;
            }
            WorkspaceBootstrapVO switched = workspaceRpcService.switchGroup(newVal);
            List<UserProjSettingPO> projectEnvLst = switched.getProjects();

            isRefreshingProjectComboBox = true;
            try {
                dsComboBox.getItems().clear();
                if (projectEnvLst != null) {
                    dsComboBox.getItems().addAll(projectEnvLst.stream().map(UserProjSettingPO::getProjectName).collect(Collectors.toList()));
                    int defaultIdx = 0;
                    for (int i = 0; i < projectEnvLst.size(); i++) {
                        if ("Y".equalsIgnoreCase(projectEnvLst.get(i).getCurFlag())) {
                            defaultIdx = i;
                            break;
                        }
                    }
                    if (!dsComboBox.getItems().isEmpty()) {
                        dsComboBox.getSelectionModel().select(defaultIdx);
                    }
                }
            } finally {
                isRefreshingProjectComboBox = false;
            }
            String selectedProject = dsComboBox.getSelectionModel().getSelectedItem();
            if (selectedProject != null) {
                applySelectedProject(newVal, selectedProject, false);
                environmentService.switchCurrentEnv(newVal, selectedProject);
            }
        });

        // 项目名称下拉切换，激活对应记录
        dsComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isRefreshingProjectComboBox || isReloadingWorkspace) {
                return;
            }
            String group = envComboBox.getSelectionModel().getSelectedItem();
            if (group != null && newVal != null) {
                applySelectedProject(group, newVal, true);
                environmentService.switchCurrentEnv(group, newVal);
                clearAllTabsAndMenuSelection();
                ViewUtils.alertForSucess("项目切换成功！");
            }
        });

        // 按当前登录角色动态生成菜单
        refreshMenuForCurrentRole();
        // 菜单收缩/展开按钮事件
        toggleMenuBtn.setOnAction(e -> toggleMenuBar());

        // 为已有Tab添加右键菜单
        for (Tab tab : doubleClickTabPane.getTabs()) {
            tab.setContextMenu(createTabContextMenu(tab));
        }
        
        // 监听Tab添加事件，自动为新Tab添加右键菜单
        doubleClickTabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        tab.setContextMenu(createTabContextMenu(tab));
                        // 手动为每个新标签页设置双击事件处理器（备用方案）
                        Platform.runLater(() -> {
                            if (tab.getContent() != null) {
                                tab.getContent().setOnMouseClicked(event -> {
                                    if (event.getClickCount() == 2) {
                                        // 调用分离逻辑
                                        doubleClickTabPane.startDoubleClickDetachment(tab, event);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });

        // 设置用户信息显示
        setupUserInfo();
        // 设置退出按钮事件
        setupLogoutButton();
    }

    /**
     * 创建Tab的右键菜单，包含关闭其他标签页功能
     */
    private javafx.scene.control.ContextMenu createTabContextMenu(Tab currentTab) {
        javafx.scene.control.MenuItem closeOthers = new javafx.scene.control.MenuItem("关闭其他标签页");
        closeOthers.setOnAction(e -> {
            doubleClickTabPane.getTabs().removeIf(tab -> tab != currentTab);
        });
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu(closeOthers);
        return menu;
    }

    // 新增：根据key选中菜单
    private void selectMenuByKey(String key) {
        System.out.println("[MainController] selectMenuByKey 开始查找菜单项: " + key);
        // 先找到对应的菜单项和其所属的分组
        Button targetButton = null;
        VBox targetSubMenuBox = null;
        HBox targetGroupButtonBox = null;
        
        // 遍历所有分组
        System.out.println("[MainController] menuGroupContainer 子节点数量: " + menuGroupContainer.getChildren().size());
        for (int i = 0; i < menuGroupContainer.getChildren().size(); i++) {
            Node node = menuGroupContainer.getChildren().get(i);
            System.out.println("[MainController] 节点 " + i + " 类型: " + node.getClass().getSimpleName() + ", 样式类: " + node.getStyleClass());
            
            // 跳过分割线，只处理分组按钮和子菜单
            if (node instanceof Pane && node.getStyleClass().contains("menu-divider")) {
                continue;
            }
            
            // 处理分组按钮
            if (node instanceof HBox) {
                HBox groupBtnBox = (HBox) node;
                // 获取分组按钮
                Button groupBtn = null;
                for (Node child : groupBtnBox.getChildren()) {
                    if (child instanceof Button) {
                        groupBtn = (Button) child;
                        break;
                    }
                }
                
                // 查找下一个子菜单容器
                if (i + 2 < menuGroupContainer.getChildren().size()) {
                    Node nextNode = menuGroupContainer.getChildren().get(i + 2); // 跳过分割线
                    if (nextNode instanceof VBox && nextNode.getStyleClass().contains("submenu-box")) {
                        VBox subMenuBox = (VBox) nextNode;
                        
                        // 在子菜单中查找目标菜单项
                        System.out.println("[MainController] 检查子菜单，子菜单项数量: " + subMenuBox.getChildren().size());
                        for (Node subNode : subMenuBox.getChildren()) {
                            if (subNode instanceof Button) {
                                Button btn = (Button) subNode;
                                MenuItem menuItem = (MenuItem) btn.getUserData();
                                System.out.println("[MainController] 检查菜单项: " + (menuItem != null ? menuItem.key : "null"));
                                if (menuItem != null && menuItem.key.equals(key)) {
                                    System.out.println("[MainController] 找到目标菜单项: " + key);
                                    targetButton = btn;
                                    targetSubMenuBox = subMenuBox;
                                    targetGroupButtonBox = groupBtnBox;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 如果找到了目标菜单项，先展开其所属的分组，然后选中该菜单项
        System.out.println("[MainController] 是否找到目标菜单项: " + (targetButton != null));
        if (targetButton != null && targetSubMenuBox != null && targetGroupButtonBox != null) {
            final Button finalTargetButton = targetButton;
            
            // 先展开分组
            if (!targetSubMenuBox.isVisible()) {
                // 找到分组按钮并触发点击事件来展开
                Button groupBtn = null;
                for (Node child : targetGroupButtonBox.getChildren()) {
                    if (child instanceof Button) {
                        groupBtn = (Button) child;
                        break;
                    }
                }
                if (groupBtn != null) {
                    groupBtn.fire(); // 触发分组展开
                }
            }
            
            // 等待一小段时间确保展开动画完成，然后选中菜单项
            PauseTransition pause = new PauseTransition(javafx.util.Duration.millis(100));
            pause.setOnFinished(e -> {
                finalTargetButton.fire(); // 触发菜单项选中
            });
            pause.play();
        }
    }

    // 动态渲染分组和二级菜单
    private void buildMenu(VBox container, MenuGroup[] groups) {
        container.getChildren().clear();
        // 用于只展开一个分组
        final VBox[] lastOpenSubMenu = {null};
        for (MenuGroup group : groups) {
            // 分组按钮右侧加+/-号
            HBox groupBtnBox = new HBox();
            groupBtnBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            groupBtnBox.setSpacing(8);
            groupBtnBox.getStyleClass().add("menu-btn");
            Button groupBtn = new Button(group.groupName);
            groupBtn.getStyleClass().add("menu-btn");
            groupBtn.setMaxWidth(Double.MAX_VALUE);
            // 图标
            InputStream iconStream = getClass().getResourceAsStream("/image/" + group.groupIcon);
            if (iconStream != null) {
                ImageView groupIcon = new ImageView(new Image(iconStream));
                groupIcon.setFitWidth(20);
                groupIcon.setFitHeight(20);
                groupIcon.setStyle("-fx-translate-x: 0; -fx-translate-y: 2;");
                groupBtn.setGraphic(groupIcon);
                groupBtn.setGraphicTextGap(10); // 调整图标与文字间距
            }
            // 一级菜单按钮左侧留白
            groupBtn.setStyle("-fx-padding: 0 0 0 20; -fx-background-radius: 10; -fx-alignment: center-left;");
            // +号Label
            Label expandLabel = new Label("+");
            expandLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 16 0 0;");
            groupBtnBox.getChildren().addAll(groupBtn, expandLabel);
            HBox.setHgrow(groupBtn, javafx.scene.layout.Priority.ALWAYS);
            // 分组展开/收起逻辑，带动画和只展开一个分组
            VBox subMenuBox = new VBox();
            subMenuBox.getStyleClass().add("submenu-box");
            subMenuBox.setVisible(false);
            subMenuBox.setManaged(false);
            subMenuBox.setMinHeight(0);
            subMenuBox.setPrefHeight(0);
            subMenuBox.setMaxHeight(0);
            groupBtn.setOnAction(e -> {
                boolean show = !subMenuBox.isVisible();
                // 收起上一个已展开的分组
                if (lastOpenSubMenu[0] != null && lastOpenSubMenu[0] != subMenuBox) {
                    collapseSubMenu(lastOpenSubMenu[0]);
                    // 同步上一个分组的+号
                    if (lastOpenSubMenu[0].getUserData() instanceof Label) {
                        ((Label)lastOpenSubMenu[0].getUserData()).setText("+");
                    }
                }
                if (show) {
                    expandSubMenu(subMenuBox);
                    lastOpenSubMenu[0] = subMenuBox;
                    expandLabel.setText("-");
                } else {
                    collapseSubMenu(subMenuBox);
                    lastOpenSubMenu[0] = null;
                    expandLabel.setText("+");
                }
            });
            // 让subMenuBox能反查到自己的+/-号Label
            subMenuBox.setUserData(expandLabel);
            // 渲染子菜单
            for (MenuItem item : group.children) {
                Button itemBtn = new Button(item.text);
                itemBtn.getStyleClass().add("menu-btn");
                itemBtn.getStyleClass().add("submenu-btn");
                itemBtn.setMaxWidth(Double.MAX_VALUE);
                // 图标加载用getMenuIconImage，确保和切换一致
                ImageView itemIcon = new ImageView();
                Image remixBImg = getMenuIconImage(item.icon, "remixB");
                if (remixBImg != null) {
                    itemIcon.setImage(remixBImg);
                }
                itemIcon.getStyleClass().add("menu-icon");
                // 如果是submenu-btn，图标调小一号
                if (itemBtn.getStyleClass().contains("submenu-btn")) {
                    itemIcon.setFitWidth(16);
                    itemIcon.setFitHeight(16);
                }
                itemBtn.setGraphic(itemIcon);
                itemBtn.setGraphicTextGap(10);
                itemBtn.setOnAction(e -> selectSubMenu(itemBtn, item));
                // 绑定MenuItem到userData，便于后续查找和切换图标
                itemBtn.setUserData(item);
                // 彻底覆盖系统样式
                itemBtn.setStyle("-fx-background-insets: 0; -fx-border-insets: 0; -fx-background-radius: 10; -fx-border-radius: 10; -fx-font-family: 'Microsoft YaHei', Arial, sans-serif; -fx-pref-height: 44px; -fx-alignment: center-left; -fx-border-width: 0 0 0 5; -fx-border-color: transparent; -fx-effect: none; -fx-cursor: hand; -fx-box-shadow: none; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-shadow-highlight-color: transparent;");
                subMenuBox.getChildren().add(itemBtn);
            }
            // 分割线
            Pane divider = new Pane();
            divider.getStyleClass().add("menu-divider");
            divider.setMinHeight(1);
            divider.setPrefHeight(1);
            divider.setMaxHeight(1);
            container.getChildren().addAll(groupBtnBox, divider, subMenuBox);
            // 默认展开第一个可见分组
            if (lastOpenSubMenu[0] == null) {
                expandSubMenu(subMenuBox);
                lastOpenSubMenu[0] = subMenuBox;
                expandLabel.setText("-");
            }
        }
    }

    private void refreshMenuForCurrentRole() {
        MenuGroup[] visibleGroups = resolveVisibleMenuGroups();
        buildMenu(menuGroupContainer, visibleGroups);
        selectInitialMenu(visibleGroups);
    }

    private MenuGroup[] resolveVisibleMenuGroups() {
        Set<String> allowedKeys = loadAllowedMenuKeys();
        boolean manager = UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(currentRoleCode());
        List<MenuGroup> visible = new ArrayList<>();
        for (MenuGroup group : menuGroups) {
            MenuItem[] children = Arrays.stream(group.children)
                    .filter(item -> {
                        MenuCatalog.Item catalogItem = MenuCatalog.findItem(item.key);
                        if (catalogItem != null && catalogItem.adminOnly() && !manager) {
                            return false;
                        }
                        return allowedKeys == null || allowedKeys.contains(item.key);
                    })
                    .toArray(MenuItem[]::new);
            if (children.length > 0) {
                visible.add(new MenuGroup(group.groupKey, group.groupName, group.groupIcon, children));
            }
        }
        return visible.toArray(new MenuGroup[0]);
    }

    /**
     * @return 允许的菜单 key；null 表示不限制（管理员兜底显示全部）
     */
    private Set<String> loadAllowedMenuKeys() {
        String roleCode = currentRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            return Set.of();
        }
        boolean manager = UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(roleCode);
        try {
            List<RoleMenuPO> roleMenus = roleMenuRpcService.queryByRole(roleCode);
            if (roleMenus == null || roleMenus.isEmpty()) {
                if (manager) {
                    return null;
                }
                return Set.of();
            }
            Set<String> keys = new HashSet<>();
            for (RoleMenuPO po : roleMenus) {
                if (po != null && po.getMenuKey() != null && !po.getMenuKey().isBlank()) {
                    keys.add(po.getMenuKey());
                }
            }
            if (manager) {
                keys.add(MenuCatalog.MENU_MANAGEMENT_KEY);
            } else {
                keys.remove(MenuCatalog.MENU_MANAGEMENT_KEY);
            }
            return keys;
        } catch (Exception e) {
            e.printStackTrace();
            return manager ? null : Set.of();
        }
    }

    private String currentRoleCode() {
        if (globalPropes == null || globalPropes.getOperator() == null) {
            return null;
        }
        String roles = globalPropes.getOperator().getRoles();
        return roles == null ? null : roles.trim();
    }

    private void selectInitialMenu(MenuGroup[] visibleGroups) {
        String lastSelectedMenu = userPreferenceService.getMenuSelected();
        System.out.println("[MainController] 尝试恢复菜单选中状态: " + lastSelectedMenu);
        if (lastSelectedMenu != null && !lastSelectedMenu.trim().isEmpty()
                && containsMenuKey(visibleGroups, lastSelectedMenu)) {
            System.out.println("[MainController] 恢复菜单: " + lastSelectedMenu);
            selectMenuByKey(lastSelectedMenu);
            return;
        }
        String firstMenuKey = firstMenuKey(visibleGroups);
        if (firstMenuKey != null) {
            System.out.println("[MainController] 使用角色可见的首个菜单: " + firstMenuKey);
            selectMenuByKey(firstMenuKey);
        }
    }

    private boolean containsMenuKey(MenuGroup[] groups, String key) {
        for (MenuGroup group : groups) {
            for (MenuItem item : group.children) {
                if (key.equals(item.key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String firstMenuKey(MenuGroup[] groups) {
        for (MenuGroup group : groups) {
            if (group.children != null && group.children.length > 0) {
                return group.children[0].key;
            }
        }
        return null;
    }
    // 展开动画
    private void expandSubMenu(VBox subMenuBox) {
        subMenuBox.setVisible(true);
        subMenuBox.setManaged(true);
        double targetHeight = subMenuBox.getChildren().size() * 44; // 每个按钮高度
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(subMenuBox.minHeightProperty(), 0),
                        new KeyValue(subMenuBox.prefHeightProperty(), 0),
                        new KeyValue(subMenuBox.maxHeightProperty(), 0),
                        new KeyValue(subMenuBox.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(subMenuBox.minHeightProperty(), targetHeight),
                        new KeyValue(subMenuBox.prefHeightProperty(), targetHeight),
                        new KeyValue(subMenuBox.maxHeightProperty(), targetHeight),
                        new KeyValue(subMenuBox.opacityProperty(), 1)
                )
        );
        timeline.play();
    }
    // 收起动画
    private void collapseSubMenu(VBox subMenuBox) {
        double startHeight = subMenuBox.getHeight();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0),
                        new KeyValue(subMenuBox.minHeightProperty(), startHeight),
                        new KeyValue(subMenuBox.prefHeightProperty(), startHeight),
                        new KeyValue(subMenuBox.maxHeightProperty(), startHeight),
                        new KeyValue(subMenuBox.opacityProperty(), 1)
                ),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(subMenuBox.minHeightProperty(), 0),
                        new KeyValue(subMenuBox.prefHeightProperty(), 0),
                        new KeyValue(subMenuBox.maxHeightProperty(), 0),
                        new KeyValue(subMenuBox.opacityProperty(), 0)
                )
        );
        timeline.setOnFinished(ev -> {
            subMenuBox.setVisible(false);
            subMenuBox.setManaged(false);
        });
        timeline.play();
    }
    // 子菜单点击逻辑
    private void selectSubMenu(Button btn, MenuItem item) {
        // 取消上一个选中项的样式和图标
        if (selectedBtn != null) {
            selectedBtn.getStyleClass().remove("selected");
            // 切换图标为 remixB
            if (selectedBtn.getGraphic() instanceof ImageView) {
                ImageView icon = (ImageView) selectedBtn.getGraphic();
                MenuItem selectedMenuItem = (MenuItem) selectedBtn.getUserData();
                if (selectedMenuItem != null) {
                    String iconKey = selectedMenuItem.icon;
                    Image remixBImg = getMenuIconImage(iconKey, "remixB");
                    if (remixBImg != null) icon.setImage(remixBImg);
                }
            }
        }
        // 当前按钮加选中样式
        btn.getStyleClass().add("selected");
        selectedBtn = btn;
        // 切换当前按钮图标为 remixA
        if (btn.getGraphic() instanceof ImageView) {
            ImageView icon = (ImageView) btn.getGraphic();
            MenuItem thisMenuItem = (MenuItem) btn.getUserData();
            if (thisMenuItem != null) {
                String iconKey = thisMenuItem.icon;
                Image remixAImg = getMenuIconImage(iconKey, "remixA");
                if (remixAImg != null) icon.setImage(remixAImg);
            }
        }
        
        // 保存菜单选中状态
        System.out.println("[MainController] 保存菜单选中状态: " + item.key);
        userPreferenceService.saveMenuSelected(item.key);
        
        showPage(item.key, item.fxmlPath);
    }

    // 新增：根据key和remix目录获取Image，并加日志
    private Image getMenuIconImage(String key, String remixDir) {
        // 自动清洗key，去除目录和后缀，只保留图片名主体
        if (key == null) return null;
        String cleanKey = key;
        // 去掉目录前缀
        int lastSlash = cleanKey.lastIndexOf("/");
        if (lastSlash >= 0) {
            cleanKey = cleanKey.substring(lastSlash + 1);
        }
        // 去掉后缀
        int dotIdx = cleanKey.indexOf(".");
        if (dotIdx > 0) {
            cleanKey = cleanKey.substring(0, dotIdx);
        }
        for (String suffix : ICON_SUFFIXES) {
            String path = "/image/" + remixDir + "/" + cleanKey + suffix;
            URL url = getClass().getResource(path);
            if (url != null) {
                return new Image(url.toExternalForm());
            }
        }
        return null;
    }

    public void showPage(String key, String fxmlPath) {
        // 检查TabPane中是否已存在该key的Tab
        for (Tab tab : doubleClickTabPane.getTabs()) {
            if (key.equals(tab.getId())) {
                doubleClickTabPane.getSelectionModel().select(tab);
                return;
            }
        }
        
        Node page = loadFXMLPage(key, fxmlPath);
        
        Tab newTab = new Tab();
        newTab.setId(key);
        newTab.setText(MenuEnum.getTitleByKey(key));
        newTab.setContent(page);
        newTab.setClosable(true);
        newTab.setContextMenu(createTabContextMenu(newTab)); // 新建Tab时设置右键菜单
        doubleClickTabPane.getTabs().add(newTab);
        doubleClickTabPane.getSelectionModel().select(newTab);
    }

    /**
     * 加载FXML页面的工具方法，包含完整的错误处理和ClassLoader管理
     */
    private Node loadFXMLPage(String key, String fxmlPath) {
        try {
            System.out.println("[DEBUG] 开始加载FXML: " + fxmlPath);
            System.out.println("[DEBUG] 当前类加载器: " + getClass().getClassLoader());
            
            // 检查资源是否存在
            java.net.URL resourceUrl = getClass().getResource(fxmlPath);
            if (resourceUrl == null) {
                throw new IOException("FXML资源未找到: " + fxmlPath);
            }
            System.out.println("[DEBUG] 资源URL: " + resourceUrl);
            
            // 创建FXMLLoader并设置必要的属性
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            
            // 确保ClassLoader不为null
            ClassLoader classLoader = getClass().getClassLoader();
            if (classLoader == null) {
                // 尝试使用系统类加载器作为备选
                classLoader = ClassLoader.getSystemClassLoader();
                System.out.println("[WARN] 使用系统类加载器作为备选");
            }
            if (classLoader == null) {
                throw new IllegalStateException("无法获取有效的ClassLoader");
            }
            
            loader.setClassLoader(classLoader);
            loader.setControllerFactory(applicationContext::getBean);
            
            System.out.println("[DEBUG] FXMLLoader ClassLoader: " + loader.getClassLoader());
            System.out.println("[DEBUG] 开始加载FXML内容...");
            
            Node page = loader.load();
            System.out.println("[DEBUG] FXML加载成功");
            
            // 如果是项目群管理页面，设置MainController引用
            if ("projectDef".equals(key)) {
                try {
                    PaneProjectGroupController projectGroupController = loader.getController();
                    if (projectGroupController != null) {
                        projectGroupController.setMainController(this);
                        System.out.println("[DEBUG] 成功设置项目群控制器引用");
                    } else {
                        System.err.println("[ERROR] 项目群控制器为null");
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] 设置项目群控制器引用失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return page;
            
        } catch (IOException e) {
            System.err.println("[ERROR] FXML加载IO异常: " + e.getMessage());
            e.printStackTrace();
            return createErrorPage(key, fxmlPath, e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] FXML加载异常: " + e.getMessage());
            e.printStackTrace();
            return createErrorPage(key, fxmlPath, e.getMessage());
        }
    }

    /**
     * 创建错误页面
     */
    private Node createErrorPage(String key, String fxmlPath, String errorMessage) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(javafx.geometry.Pos.CENTER);
        errorBox.setPadding(new javafx.geometry.Insets(20));
        
        Label titleLabel = new Label("页面加载失败");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #dc3545;");
        
        Label keyLabel = new Label("页面: " + key);
        Label pathLabel = new Label("路径: " + fxmlPath);
        Label errorLabel = new Label("错误: " + errorMessage);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);
        
        Button retryButton = new Button("重试");
        retryButton.setOnAction(e -> {
            // 重新加载页面
            showPage(key, fxmlPath);
        });
        
        errorBox.getChildren().addAll(titleLabel, keyLabel, pathLabel, errorLabel, retryButton);
        return errorBox;
    }

    public TabPane getPageContainer() {
        return doubleClickTabPane;
    }

    public Tab getTabByKey(String key) {
        for (Tab tab : doubleClickTabPane.getTabs()) {
            if (key.equals(tab.getId())) {
                return tab;
            }
        }
        return null;
    }

    // 菜单收缩/展开时，隐藏/显示分组和子菜单文字
    private void setMenuText(VBox container, boolean visible, MenuGroup[] groups) {
        int idx = 0;
        for (Node node : container.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                // 分组按钮
                if (btn.getStyleClass().contains("menu-btn") && !btn.getStyleClass().contains("submenu-btn")) {
                    btn.setText(visible ? groups[idx / 2].groupName : "");
                } else {
                    // 子菜单按钮
                    int groupIdx = idx / 2;
                    int subIdx = (idx % 2 == 1) ? (idx - 1) / 2 : 0;
                    if (groupIdx < groups.length && subIdx < groups[groupIdx].children.length) {
                        btn.setText(visible ? groups[groupIdx].children[subIdx].text : "");
                    }
                }
            }
            idx++;
        }
    }

    private void setMenuCollapsedStyle(VBox group, boolean collapsed) {
        for (Node node : group.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (collapsed) {
                    if (!btn.getStyleClass().contains("menu-collapsed")) {
                        btn.getStyleClass().add("menu-collapsed");
                    }
                    // 一级菜单图标居中并正方形
                    if (btn.getStyleClass().contains("menu-btn") && !btn.getStyleClass().contains("submenu-btn")) {
                        btn.setAlignment(javafx.geometry.Pos.CENTER);
                        btn.setMinWidth(60);
                        btn.setMaxWidth(60);
                        btn.setPrefWidth(60);
                        btn.setMinHeight(60);
                        btn.setMaxHeight(60);
                        btn.setPrefHeight(60);
                        btn.setPadding(javafx.geometry.Insets.EMPTY);
                        btn.setGraphicTextGap(0);
                        btn.setContentDisplay(ContentDisplay.CENTER);
                    }
                } else {
                    btn.getStyleClass().remove("menu-collapsed");
                    // 一级菜单图标靠左并恢复宽度
                    if (btn.getStyleClass().contains("menu-btn") && !btn.getStyleClass().contains("submenu-btn")) {
                        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        btn.setMinWidth(0);
                        btn.setMaxWidth(Double.MAX_VALUE);
                        btn.setPrefWidth(Double.MAX_VALUE);
                        btn.setMinHeight(44);
                        btn.setMaxHeight(44);
                        btn.setPrefHeight(44);
                        btn.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
                        btn.setGraphicTextGap(10);
                        btn.setContentDisplay(ContentDisplay.LEFT);
                    }
                }
            }
        }
    }

    private void toggleMenuBar() {
        menuCollapsed = !menuCollapsed;
        if (menuCollapsed) {
            // 添加collapsed样式类
            if (!menuBar.getStyleClass().contains("collapsed")) {
                menuBar.getStyleClass().add("collapsed");
            }
            // 隐藏LOGO文字
            for (Node node : ((VBox)menuBar.getChildren().get(0)).getChildren()) {
                if (node instanceof HBox) {
                    for (Node n : ((HBox)node).getChildren()) {
                        if (n instanceof Label && ((Label)n).getStyleClass().contains("menu-logo-text")) {
                            n.setVisible(false);
                        }
                    }
                }
            }
            // 隐藏菜单文字
            setMenuText(menuGroupContainer, false, menuGroups);
            setMenuCollapsedStyle(menuGroupContainer, true);
        } else {
            // 移除collapsed样式类
            menuBar.getStyleClass().remove("collapsed");
            // 显示LOGO文字
            for (Node node : ((VBox)menuBar.getChildren().get(0)).getChildren()) {
                if (node instanceof HBox) {
                    for (Node n : ((HBox)node).getChildren()) {
                        if (n instanceof Label && ((Label)n).getStyleClass().contains("menu-logo-text")) {
                            n.setVisible(true);
                        }
                    }
                }
            }
            // 显示菜单文字
            setMenuText(menuGroupContainer, true, menuGroups);
            setMenuCollapsedStyle(menuGroupContainer, false);
        }
    }

    /**
     * 清除所有标签页和菜单选中状态
     */
    private void clearAllTabsAndMenuSelection() {
        // 关闭所有分离的窗口
        doubleClickTabPane.closeAllDetachedWindows();
        
        // 关闭所有标签页
        doubleClickTabPane.getTabs().clear();
        
        // 清除菜单选中状态
        if (selectedBtn != null) {
            selectedBtn.getStyleClass().remove("selected");
            // 切换图标为 remixB（未选中状态）
            if (selectedBtn.getGraphic() instanceof ImageView) {
                ImageView icon = (ImageView) selectedBtn.getGraphic();
                MenuItem selectedMenuItem = (MenuItem) selectedBtn.getUserData();
                if (selectedMenuItem != null) {
                    String iconKey = selectedMenuItem.icon;
                    Image remixBImg = getMenuIconImage(iconKey, "remixB");
                    if (remixBImg != null) icon.setImage(remixBImg);
                }
            }
            selectedBtn = null;
        }
        
        // 清除保存的菜单选中状态
        userPreferenceService.clearMenuSelected();
    }
    
    /**
     * 按当前登录用户重新加载顶部项目群/项目下拉框。
     */
    public void reloadWorkspaceSelectors() {
        isReloadingWorkspace = true;
        isRefreshingProjectComboBox = true;
        try {
            WorkspaceBootstrapVO workspace = workspaceRpcService.bootstrap();
            List<UserProjGroupPO> groupLst = workspace.getGroups() == null ? List.of() : workspace.getGroups();
            envComboBox.getItems().clear();
            envComboBox.getItems().addAll(groupLst.stream()
                    .map(UserProjGroupPO::getGroupName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList()));
            if (workspace.getCurrentGroupName() != null && envComboBox.getItems().contains(workspace.getCurrentGroupName())) {
                envComboBox.getSelectionModel().select(workspace.getCurrentGroupName());
            } else if (!envComboBox.getItems().isEmpty()) {
                envComboBox.getSelectionModel().selectFirst();
            } else {
                envComboBox.getSelectionModel().clearSelection();
            }

            List<UserProjSettingPO> projectLst = workspace.getProjects() == null ? List.of() : workspace.getProjects();
            dsComboBox.getItems().clear();
            dsComboBox.getItems().addAll(projectLst.stream()
                    .map(UserProjSettingPO::getProjectName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList()));
            int defaultEnvIdx = 0;
            for (int i = 0; i < projectLst.size(); i++) {
                if ("Y".equalsIgnoreCase(projectLst.get(i).getCurFlag())) {
                    defaultEnvIdx = i;
                    break;
                }
            }
            if (!dsComboBox.getItems().isEmpty()) {
                dsComboBox.getSelectionModel().select(defaultEnvIdx);
            } else {
                dsComboBox.getSelectionModel().clearSelection();
            }

            String group = envComboBox.getValue();
            String project = dsComboBox.getValue();
            if (group != null && project != null) {
                applySelectedProject(group, project, false);
            }
        } finally {
            isRefreshingProjectComboBox = false;
            isReloadingWorkspace = false;
        }
    }

    private void applySelectedProject(String group, String projectName, boolean queryIfMissing) {
        UserProjSettingPO query = new UserProjSettingPO();
        query.setGroupName(group);
        query.setProjectName(projectName);
        if (globalPropes.getOperator() != null) {
            query.setUserId(globalPropes.getOperator().getUserId());
            if (query.getUserId() == null || query.getUserId().isBlank()) {
                query.setUsername(globalPropes.getOperator().getUsername());
            }
        }
        UserProjSettingPO po = userProjSettingRpcService.queryOne(query);
        if (po == null && queryIfMissing) {
            return;
        }
        if (po == null) {
            return;
        }
        ProjectSettingPO convertedProjectSetting = new ProjectSettingPO();
        convertedProjectSetting.setGroupName(po.getGroupName());
        convertedProjectSetting.setProjectName(po.getProjectName());
        convertedProjectSetting.setProjectType(po.getProjectType());
        convertedProjectSetting.setProjectDesc(po.getProjectDesc());
        convertedProjectSetting.setAppName(po.getAppName());
        convertedProjectSetting.setAppPort(po.getAppPort());
        convertedProjectSetting.setSchemaNm(po.getSchemaNm());
        convertedProjectSetting.setBasePath(po.getBasePath());
        convertedProjectSetting.setPropPath(po.getPropPath());
        convertedProjectSetting.setEnumPath(po.getEnumPath());
        convertedProjectSetting.setMsgcdPath(po.getMsgcdPath());
        convertedProjectSetting.setCurFlag(po.getCurFlag());
        convertedProjectSetting.setUpdateBy(po.getUpdateBy());
        convertedProjectSetting.setUpdateTime(po.getUpdateTime());
        convertedProjectSetting.setShowFlag(po.getShowFlag());
        environmentService.rebuildGlobalPropes(globalPropes, convertedProjectSetting);
    }

    /**
     * 刷新项目下拉列表
     */
    public void refreshProjectComboBox() {
        System.out.println("[DEBUG] 开始刷新项目下拉列表");
        
        // 设置刷新标志，防止触发项目切换
        isRefreshingProjectComboBox = true;
        
        try {
            String currentGroup = envComboBox.getValue();
            String currentProject = dsComboBox.getValue();
            
            System.out.println("[DEBUG] 当前项目群: " + currentGroup + ", 当前项目: " + currentProject);
            
            if (currentGroup != null) {
                // 查询该项目群下所有显示的项目
                UserProjSettingPO projectSettingPO = new UserProjSettingPO();
                projectSettingPO.setGroupName(currentGroup);
                projectSettingPO.setShowFlag("Y");
                List<UserProjSettingPO> projectLst = userProjSettingRpcService.queryForList(projectSettingPO);
                
                System.out.println("[DEBUG] 查询到 " + projectLst.size() + " 个显示的项目");
                
                dsComboBox.getItems().clear();
                dsComboBox.getItems().addAll(projectLst.stream().map(UserProjSettingPO::getProjectName).collect(Collectors.toList()));
                
                // 尝试保持当前选中的项目，如果不存在则选择第一个
                if (currentProject != null && dsComboBox.getItems().contains(currentProject)) {
                    dsComboBox.setValue(currentProject);
                    System.out.println("[DEBUG] 保持当前项目: " + currentProject);
                } else if (!dsComboBox.getItems().isEmpty()) {
                    dsComboBox.getSelectionModel().selectFirst();
                    System.out.println("[DEBUG] 选择第一个项目: " + dsComboBox.getValue());
                }
                
                System.out.println("[DEBUG] 项目下拉列表刷新完成，当前选中: " + dsComboBox.getValue());
            } else {
                System.out.println("[DEBUG] 当前项目群为空，跳过刷新");
            }
        } finally {
            // 恢复刷新标志
            isRefreshingProjectComboBox = false;
        }
    }
    
    /**
     * 获取当前选中的项目群
     */
    public String getCurrentGroup() {
        return envComboBox.getValue();
    }
    
    /**
     * 获取当前选中的项目
     */
    public String getCurrentProject() {
        return dsComboBox.getValue();
    }
    
    /**
     * 设置用户信息显示
     */
    private void setupUserInfo() {
        if (globalPropes != null && globalPropes.getOperator() != null) {
            String username = globalPropes.getOperator().getUsername();
            if (username != null && !username.trim().isEmpty()) {
                usernameLabel.setText(username);
            } else {
                usernameLabel.setText("用户");
            }
        } else {
            usernameLabel.setText("用户");
        }
    }
    
    /**
     * 设置主窗口Stage引用
     */
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }
    
    /**
     * 设置退出按钮事件
     */
    private void setupLogoutButton() {
        logoutBtn.setOnAction(e -> {
            // 使用ViewUtils.alertForAsk显示确认对话框
            ViewUtils.alertForAsk("确认退出", "确定要退出到登录界面吗？").ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // 用户确认退出，显示登录界面
                    localSettingDao.clearLinkInfo();
                    showLoginDialog();
                }
            });
        });
    }
    
    /**
     * 显示登录对话框
     */
    private void showLoginDialog() {
        if (mainStage != null) {
            // 隐藏主窗口
            mainStage.hide();
            
            // 显示登录对话框
            LoginDialog loginDialog = applicationContext.getBean(LoginDialog.class);
            boolean loginSuccess = loginDialog.showLoginDialog(mainStage);
            
            if (loginSuccess) {
                mainStage.show();
                setupUserInfo();
                clearAllTabsAndMenuSelection();
                refreshMenuForCurrentRole();
                reloadWorkspaceSelectors();
            } else {
                // 登录失败或取消，退出应用程序
                Platform.exit();
            }
        } else {
            // 如果没有Stage引用，直接退出
            Platform.exit();
        }
    }
} 