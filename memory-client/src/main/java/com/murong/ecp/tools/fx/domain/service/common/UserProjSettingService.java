package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProjSettingService {
    
    @Autowired
    private UserProjSettingRpcService userProjSettingRpcService;
    
    /**
     * 保存用户项目设置权限
     */
    public void saveUserProjSetting(UserProjSettingPO userProjSetting) {
        userProjSettingRpcService.save(userProjSetting);
    }
    
    /**
     * 查询用户项目设置权限列表
     */
    public List<UserProjSettingPO> queryUserProjSettings(UserProjSettingPO queryPo) {
        return userProjSettingRpcService.queryForList(queryPo);
    }
    
    /**
     * 根据用户ID查询项目设置权限
     */
    public List<UserProjSettingPO> queryByUserId(String userId) {
        return userProjSettingRpcService.queryByUserId(userId);
    }
    
    /**
     * 根据用户名查询项目设置权限
     */
    public List<UserProjSettingPO> queryByUsername(String username) {
        return userProjSettingRpcService.queryByUsername(username);
    }
    
    /**
     * 根据组名查询用户项目设置权限
     */
    public List<UserProjSettingPO> queryByGroupName(String groupName) {
        return userProjSettingRpcService.queryByGroupName(groupName);
    }
    
    /**
     * 根据项目名查询用户项目设置权限
     */
    public List<UserProjSettingPO> queryByProjectName(String projectName) {
        return userProjSettingRpcService.queryByProjectName(projectName);
    }
    
    /**
     * 根据应用名查询用户项目设置权限
     */
    public List<UserProjSettingPO> queryByAppName(String appName) {
        return userProjSettingRpcService.queryByAppName(appName);
    }
    
    /**
     * 查询用户当前项目权限
     */
    public List<UserProjSettingPO> queryCurrentProjects(String userId, String username) {
        return userProjSettingRpcService.queryCurrentProjects(userId, username);
    }
    
    /**
     * 查询用户显示的项目权限
     */
    public List<UserProjSettingPO> queryShowProjects(String userId, String username) {
        return userProjSettingRpcService.queryShowProjects(userId, username);
    }
    
    /**
     * 检查用户是否有指定项目权限
     */
    public boolean hasProjectPermission(String userId, String username, String groupName, String projectName, String appName) {
        return userProjSettingRpcService.isProjectSettingExists(groupName, projectName, appName, userId, username);
    }
    
    /**
     * 删除用户项目设置权限
     */
    public void deleteUserProjSetting(String groupName, String projectName, String appName, String userId, String username) {
        userProjSettingRpcService.deleteByProject(groupName, projectName, appName, userId, username);
    }
    
    /**
     * 更新用户项目设置权限
     */
    public void updateUserProjSetting(UserProjSettingPO updatePo, UserProjSettingPO wherePo) {
        userProjSettingRpcService.update(updatePo, wherePo);
    }
    
    /**
     * 批量删除用户的所有项目设置权限
     */
    public void batchDeleteByUser(String userId, String username) {
        userProjSettingRpcService.batchDeleteByUser(userId, username);
    }
    
    /**
     * 根据项目组获取用户的项目权限列表（用于界面显示）
     */
    public List<UserProjSettingPO> getUserProjectPermissionsByGroup(String userId, String username, String groupName) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        queryPo.setUsername(username);
        queryPo.setGroupName(groupName);
        return queryUserProjSettings(queryPo);
    }
}
