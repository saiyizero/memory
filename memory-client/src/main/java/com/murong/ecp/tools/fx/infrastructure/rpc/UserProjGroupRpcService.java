package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户项目组 HTTP 门面，当前用户由请求头注入，SQL 在 memory-service 执行。
 */
@Service
public class UserProjGroupRpcService extends HttpDaoSupport<UserProjGroupPO> {

    public UserProjGroupPO queryCurGroup() {
        return invoke("queryCurGroup", UserProjGroupPO.class);
    }

    public UserProjGroupPO queryCurGroup(LocalSettingPO localSettingPO) {
        return invoke("queryCurGroup", UserProjGroupPO.class, localSettingPO);
    }

    public void switchGroup(String groupName) {
        invokeVoid("switchGroup", groupName);
    }

    public void save(UserProjGroupPO po) {
        invokeVoid("save", po);
    }

    public List<UserProjGroupPO> queryForList(UserProjGroupPO po) {
        return invokeList("queryForList", UserProjGroupPO.class, po);
    }

    public List<UserProjGroupPO> queryByUserId(String userId) {
        return invokeList("queryByUserId", UserProjGroupPO.class, userId);
    }

    public List<UserProjGroupPO> queryByUsername(String username) {
        return invokeList("queryByUsername", UserProjGroupPO.class, username);
    }

    public UserProjGroupPO queryByGroupName(String groupName) {
        return invoke("queryByGroupName", UserProjGroupPO.class, groupName);
    }

    public boolean isGroupNameExists(String groupName, String userId, String username) {
        Boolean exists = invoke("isGroupNameExists", Boolean.class, groupName, userId, username);
        return Boolean.TRUE.equals(exists);
    }

    public void deleteByGroupName(String groupName, String userId, String username) {
        invokeVoid("deleteByGroupName", groupName, userId, username);
    }

    public void update(UserProjGroupPO updatePo, UserProjGroupPO wherePo) {
        invokeVoid("update", updatePo, wherePo);
    }
}
