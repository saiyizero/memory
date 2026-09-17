package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.MenuCatalog;
import com.murong.ecp.tools.fx.domain.service.common.RoleMenuService;
import com.murong.ecp.tools.fx.enums.UserRoleEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.RoleMenuPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

@Component
public class PaneMenuMngController implements Initializable {

    @Autowired
    private RoleMenuService roleMenuService;

    @Autowired
    private GlobalProperties globalProperties;

    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private Button selectAllBtn;
    @FXML
    private Button clearAllBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Label hintLabel;
    @FXML
    private VBox menuContainer;

    private final Map<String, CheckBox> menuCheckBoxes = new LinkedHashMap<>();
    private boolean loadingRoleMenus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildMenuCheckBoxes();
        initRoleCombo();
        initButtons();
        applyAdminGuard();
        if (isCurrentUserManager()) {
            loadRoleMenus(currentSelectedRoleCode());
        }
    }

    private void buildMenuCheckBoxes() {
        menuContainer.getChildren().clear();
        menuCheckBoxes.clear();
        for (MenuCatalog.Group group : MenuCatalog.groups()) {
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(16);
            flowPane.setVgap(8);
            flowPane.setPadding(new Insets(8, 4, 8, 4));
            for (MenuCatalog.Item item : group.items()) {
                if (item.alwaysVisible()) {
                    continue;
                }
                CheckBox checkBox = new CheckBox(item.text());
                checkBox.setUserData(item);
                menuCheckBoxes.put(item.key(), checkBox);
                flowPane.getChildren().add(checkBox);
            }
            TitledPane titledPane = new TitledPane(group.groupName(), flowPane);
            titledPane.setExpanded(true);
            titledPane.setCollapsible(true);
            menuContainer.getChildren().add(titledPane);
        }
    }

    private void initRoleCombo() {
        roleCombo.getItems().setAll(UserRoleEnum.displayValues());
        roleCombo.getSelectionModel().select(UserRoleEnum.toCodeDesc(UserRoleEnum.MANAGER.getCode()));
        roleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && isCurrentUserManager()) {
                loadRoleMenus(extractRoleCode(newVal));
            }
        });
    }

    private void initButtons() {
        selectAllBtn.setOnAction(e -> setAssignableSelected(true));
        clearAllBtn.setOnAction(e -> setAssignableSelected(false));
        saveBtn.setOnAction(e -> saveRoleMenus());
    }

    private void applyAdminGuard() {
        boolean manager = isCurrentUserManager();
        roleCombo.setDisable(!manager);
        selectAllBtn.setDisable(!manager);
        clearAllBtn.setDisable(!manager);
        saveBtn.setDisable(!manager);
        menuCheckBoxes.values().forEach(box -> box.setDisable(!manager));
        if (!manager) {
            hintLabel.setText("当前账号不是管理员，无法配置角色菜单");
        }
    }

    private void loadRoleMenus(String roleCode) {
        loadingRoleMenus = true;
        try {
            List<RoleMenuPO> roleMenus = roleMenuService.queryAllByRole(roleCode);
            Set<String> selectedKeys = roleMenuService.toMenuKeySet(roleMenus);
            boolean managerRole = UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(roleCode);
            boolean emptyConfig = selectedKeys.isEmpty();
            for (Map.Entry<String, CheckBox> entry : menuCheckBoxes.entrySet()) {
                String menuKey = entry.getKey();
                CheckBox checkBox = entry.getValue();
                MenuCatalog.Item item = (MenuCatalog.Item) checkBox.getUserData();
                boolean adminOnly = item != null && item.adminOnly();
                if (adminOnly && !managerRole) {
                    checkBox.setSelected(false);
                    checkBox.setDisable(true);
                    checkBox.setVisible(false);
                    checkBox.setManaged(false);
                    continue;
                }
                checkBox.setVisible(true);
                checkBox.setManaged(true);
                if (adminOnly) {
                    checkBox.setSelected(true);
                    checkBox.setDisable(true);
                    continue;
                }
                checkBox.setDisable(!isCurrentUserManager());
                if (emptyConfig) {
                    checkBox.setSelected(managerRole);
                } else {
                    checkBox.setSelected(selectedKeys.contains(menuKey));
                }
            }
            if (!isCurrentUserManager()) {
                hintLabel.setText("当前账号不是管理员，无法配置角色菜单");
            } else if (emptyConfig) {
                hintLabel.setText(managerRole ? "该角色尚未配置，默认勾选全部菜单" : "该角色尚未配置，当前未勾选任何菜单");
            } else {
                hintLabel.setText("已加载该角色当前菜单配置");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("加载角色菜单失败: " + e.getMessage());
        } finally {
            loadingRoleMenus = false;
        }
    }

    private void setAssignableSelected(boolean selected) {
        if (loadingRoleMenus || !isCurrentUserManager()) {
            return;
        }
        String roleCode = currentSelectedRoleCode();
        boolean managerRole = UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(roleCode);
        for (CheckBox checkBox : menuCheckBoxes.values()) {
            MenuCatalog.Item item = (MenuCatalog.Item) checkBox.getUserData();
            if (item != null && item.adminOnly()) {
                checkBox.setSelected(managerRole);
                continue;
            }
            if (checkBox.isVisible() && !checkBox.isDisable()) {
                checkBox.setSelected(selected);
            }
        }
    }

    private void saveRoleMenus() {
        if (!isCurrentUserManager()) {
            ViewUtils.alertForFail("仅管理员可以保存角色菜单");
            return;
        }
        String roleCode = currentSelectedRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            ViewUtils.alertForFail("请先选择角色");
            return;
        }
        List<String> selectedKeys = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : menuCheckBoxes.entrySet()) {
            CheckBox checkBox = entry.getValue();
            if (checkBox.isSelected()) {
                selectedKeys.add(entry.getKey());
            }
        }
        try {
            roleMenuService.saveRoleMenus(roleCode, selectedKeys);
            ViewUtils.alertForSucess("保存成功，该角色用户重新登录后菜单将更新");
            loadRoleMenus(roleCode);
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("保存角色菜单失败: " + e.getMessage());
        }
    }

    private String currentSelectedRoleCode() {
        return extractRoleCode(roleCombo.getSelectionModel().getSelectedItem());
    }

    private String extractRoleCode(String display) {
        if (display == null || display.isBlank()) {
            return null;
        }
        return display.substring(0, 1);
    }

    private boolean isCurrentUserManager() {
        if (globalProperties == null || globalProperties.getOperator() == null) {
            return false;
        }
        return UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(globalProperties.getOperator().getRoles());
    }
}
