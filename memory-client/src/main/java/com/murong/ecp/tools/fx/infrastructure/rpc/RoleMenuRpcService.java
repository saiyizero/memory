package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.RoleMenuPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色菜单 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class RoleMenuRpcService extends HttpDaoSupport<RoleMenuPO> {

    public List<RoleMenuPO> queryByRole(String roleCode) {
        return invokeList("queryByRole", RoleMenuPO.class, roleCode);
    }

    public List<RoleMenuPO> queryAllByRole(String roleCode) {
        return invokeList("queryAllByRole", RoleMenuPO.class, roleCode);
    }

    public void replaceByRole(String roleCode, List<RoleMenuPO> menus) {
        invokeVoid("replaceByRole", roleCode, menus);
    }
}
