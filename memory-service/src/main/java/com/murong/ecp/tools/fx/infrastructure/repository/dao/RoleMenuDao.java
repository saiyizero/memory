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
}
