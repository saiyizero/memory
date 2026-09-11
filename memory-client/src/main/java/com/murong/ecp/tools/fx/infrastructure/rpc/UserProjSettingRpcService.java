package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户项目设置 HTTP 门面，当前用户由请求头注入，SQL 在 memory-service 执行。
 */
@Service
public class UserProjSettingRpcService extends HttpDaoSupport<UserProjSettingPO> {

    public void save(UserProjSettingPO po) {
        invokeVoid("save", po);
    }

    public List<UserProjSettingPO> queryForList(UserProjSettingPO po) {
        return invokeList("queryForList", UserProjSettingPO.class, po);
    }

    public List<UserProjSettingPO> queryByUserId(String userId) {
        return invokeList("queryByUserId", UserProjSettingPO.class, userId);
    }

    public List<UserProjSettingPO> queryByUsername(String username) {
        return invokeList("queryByUsername", UserProjSettingPO.class, username);
    }

    public List<UserProjSettingPO> queryByGroupName(String groupName) {
        return invokeList("queryByGroupName", UserProjSettingPO.class, groupName);
    }

    public List<UserProjSettingPO> queryByProjectName(String projectName) {
        return invokeList("queryByProjectName", UserProjSettingPO.class, projectName);
    }

    public List<UserProjSettingPO> queryByAppName(String appName) {
        return invokeList("queryByAppName", UserProjSettingPO.class, appName);
    }

    public List<UserProjSettingPO> queryCurrentProjects(String userId, String username) {
        return invokeList("queryCurrentProjects", UserProjSettingPO.class, userId, username);
    }

    public UserProjSettingPO queryCurProject(String groupName) {
        return invoke("queryCurProject", UserProjSettingPO.class, groupName);
    }

    public UserProjSettingPO queryCurProject(String groupName, LocalSettingPO localSettingPO) {
        return invoke("queryCurProject", UserProjSettingPO.class, groupName, localSettingPO);
    }

    public List<UserProjSettingPO> queryShowProjects(String userId, String username) {
        return invokeList("queryShowProjects", UserProjSettingPO.class, userId, username);
    }

    public boolean isProjectSettingExists(String groupName, String projectName, String appName, String userId, String username) {
        Boolean exists = invoke("isProjectSettingExists", Boolean.class, groupName, projectName, appName, userId, username);
        return Boolean.TRUE.equals(exists);
    }

    public void deleteByProject(String groupName, String projectName, String appName, String userId, String username) {
        invokeVoid("deleteByProject", groupName, projectName, appName, userId, username);
    }

    public void update(UserProjSettingPO updatePo, UserProjSettingPO wherePo) {
        invokeVoid("update", updatePo, wherePo);
    }

    public void batchDeleteByUser(String userId, String username) {
        invokeVoid("batchDeleteByUser", userId, username);
    }
}
