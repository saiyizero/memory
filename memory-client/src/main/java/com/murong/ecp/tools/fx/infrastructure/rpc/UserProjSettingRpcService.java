package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 用户项目设置 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class UserProjSettingRpcService extends HttpDaoSupport<UserProjSettingPO> {

    @Autowired
    private LocalSettingDao localSettingDao;
    @Autowired
    private UserInfoRpcService userInfoRpcService;

    private UserInfoPO getCurrentUser() {
        LocalSettingPO localSettingPO = new LocalSettingPO();
        localSettingPO.setStatus(FlgEnum.YES.getValue());
        LocalSettingPO currentSetting = localSettingDao.queryOne(localSettingPO);
        if (currentSetting != null && currentSetting.getLinkUsrName() != null) {
            return userInfoRpcService.queryByUsername(currentSetting.getLinkUsrName());
        }
        return null;
    }

    public void save(UserProjSettingPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        super.insert(po);
    }

    public List<UserProjSettingPO> queryForList(UserProjSettingPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        return super.queryForList(po, "update_time desc");
    }

    public List<UserProjSettingPO> queryByUserId(String userId) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        return super.queryForList(queryPo, "update_time desc");
    }

    public List<UserProjSettingPO> queryByUsername(String username) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUsername(username);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    public List<UserProjSettingPO> queryByGroupName(String groupName) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setGroupName(groupName);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    public List<UserProjSettingPO> queryByProjectName(String projectName) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setProjectName(projectName);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    public List<UserProjSettingPO> queryByAppName(String appName) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setAppName(appName);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    public List<UserProjSettingPO> queryCurrentProjects(String userId, String username) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        queryPo.setUsername(username);
        queryPo.setCurFlag("Y");
        return super.queryForList(queryPo, "update_time desc");
    }

    public UserProjSettingPO queryCurProject(String groupName){
        UserProjSettingPO userProjSetting = new UserProjSettingPO();
        userProjSetting.setGroupName(groupName);
        userProjSetting.setCurFlag("Y");
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            userProjSetting.setUserId(currentUser.getUserId());
            userProjSetting.setUsername(currentUser.getUsername());
        }
        return super.queryOne(userProjSetting);
    }

    public UserProjSettingPO queryCurProject(String groupName, LocalSettingPO localSettingPO){
        UserProjSettingPO userProjSetting = new UserProjSettingPO();
        userProjSetting.setUsername(localSettingPO.getLinkUsrName());
        userProjSetting.setGroupName(groupName);
        userProjSetting.setCurFlag("Y");
        return super.queryOne(userProjSetting);
    }

    public List<UserProjSettingPO> queryShowProjects(String userId, String username) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        queryPo.setUsername(username);
        queryPo.setShowFlag("Y");
        return super.queryForList(queryPo, "update_time desc");
    }

    public boolean isProjectSettingExists(String groupName, String projectName, String appName, String userId, String username) {
        Boolean exists = invoke("isProjectSettingExists", Boolean.class, groupName, projectName, appName, userId, username);
        return Boolean.TRUE.equals(exists);
    }

    public void deleteByProject(String groupName, String projectName, String appName, String userId, String username) {
        invokeVoid("deleteByProject", groupName, projectName, appName, userId, username);
    }

    public void update(UserProjSettingPO updatePo, UserProjSettingPO wherePo) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            wherePo.setUserId(currentUser.getUserId());
            wherePo.setUsername(currentUser.getUsername());
        }
        super.updateByOne(updatePo, wherePo);
    }

    public void batchDeleteByUser(String userId, String username) {
        invokeVoid("batchDeleteByUser", userId, username);
    }
}
