package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.MenuCatalog;
import com.murong.ecp.tools.fx.enums.UserRoleEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.RoleMenuPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.RoleMenuRpcService;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleMenuService {

    @Autowired
    private RoleMenuRpcService roleMenuRpcService;

    @Autowired
    private GlobalProperties globalProperties;

    public List<RoleMenuPO> queryAllByRole(String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            return List.of();
        }
        List<RoleMenuPO> list = roleMenuRpcService.queryAllByRole(roleCode);
        return list == null ? List.of() : list;
    }

    public void saveRoleMenus(String roleCode, List<String> selectedKeys) {
        if (StringUtils.isBlank(roleCode)) {
            throw new IllegalArgumentException("角色不能为空");
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (selectedKeys != null) {
            for (String key : selectedKeys) {
                if (StringUtils.isNotBlank(key)) {
                    keys.add(key);
                }
            }
        }
        if (UserRoleEnum.MANAGER.getCode().equalsIgnoreCase(roleCode)) {
            keys.add(MenuCatalog.MENU_MANAGEMENT_KEY);
        } else {
            keys.remove(MenuCatalog.MENU_MANAGEMENT_KEY);
        }

        String updateBy = currentUsername();
        String updateTime = MrDateUtils.getCurrentTime();
        List<RoleMenuPO> menus = new ArrayList<>();
        int sortNo = 1;
        for (MenuCatalog.Group group : MenuCatalog.groups()) {
            for (MenuCatalog.Item item : group.items()) {
                if (!keys.contains(item.key())) {
                    continue;
                }
                RoleMenuPO po = new RoleMenuPO();
                po.setRoleCode(roleCode);
                po.setMenuKey(item.key());
                po.setMenuName(item.text());
                po.setGroupKey(group.groupKey());
                po.setGroupName(group.groupName());
                po.setSortNo(sortNo++);
                po.setShowFlag("Y");
                po.setUpdateBy(updateBy);
                po.setUpdateTime(updateTime);
                menus.add(po);
            }
        }
        roleMenuRpcService.replaceByRole(roleCode, menus);
    }

    public Set<String> toMenuKeySet(List<RoleMenuPO> roleMenus) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (roleMenus == null) {
            return keys;
        }
        for (RoleMenuPO po : roleMenus) {
            if (po != null && StringUtils.isNotBlank(po.getMenuKey())) {
                keys.add(po.getMenuKey());
            }
        }
        return keys;
    }

    private String currentUsername() {
        if (globalProperties == null || globalProperties.getOperator() == null) {
            return null;
        }
        return globalProperties.getOperator().getUsername();
    }
}
