package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.RoleMenuPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoleMenuDao extends DaoSupport<RoleMenuPO> {

    public List<RoleMenuPO> queryByRole(String roleCode) {
        RoleMenuPO query = new RoleMenuPO();
        query.setRoleCode(roleCode);
        query.setShowFlag("Y");
        return super.queryForList(query, "sort_no asc");
    }

    public List<RoleMenuPO> queryAllByRole(String roleCode) {
        RoleMenuPO query = new RoleMenuPO();
        query.setRoleCode(roleCode);
        return super.queryForList(query, "sort_no asc");
    }

    public void replaceByRole(String roleCode, List<RoleMenuPO> menus) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        RoleMenuPO deleteQuery = new RoleMenuPO();
        deleteQuery.setRoleCode(roleCode);
        super.delete(deleteQuery);
        if (menus == null || menus.isEmpty()) {
            return;
        }
        int sortNo = 1;
        for (RoleMenuPO menu : menus) {
            if (menu == null || menu.getMenuKey() == null || menu.getMenuKey().isBlank()) {
                continue;
            }
            menu.setRoleCode(roleCode);
            if (menu.getShowFlag() == null || menu.getShowFlag().isBlank()) {
                menu.setShowFlag("Y");
            }
            if (menu.getSortNo() == null) {
                menu.setSortNo(sortNo);
            }
            sortNo++;
            super.insert(menu);
        }
    }
}
