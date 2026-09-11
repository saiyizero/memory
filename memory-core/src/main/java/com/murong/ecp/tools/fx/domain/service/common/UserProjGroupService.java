package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjGroupDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProjGroupService {
    
    @Autowired
    private UserProjGroupDao userProjGroupDao;

    /**
     * 切换项目群
     */
    public void switchProjectGroup(String groupName) {
        userProjGroupDao.switchGroup(groupName);
    }
    
    /**
     * 保存用户项目组权限
     */
    public void saveUserProjGroup(UserProjGroupPO userProjGroup) {
        userProjGroupDao.save(userProjGroup);
    }
    
    /**
     * 查询用户项目组权限列表
     */
    public List<UserProjGroupPO> queryUserProjGroups(UserProjGroupPO queryPo) {
        return userProjGroupDao.queryForList(queryPo);
    }
    
    /**
     * 根据用户ID查询项目组权限
     */
    public List<UserProjGroupPO> queryByUserId(String userId) {
        return userProjGroupDao.queryByUserId(userId);
    }
    
    /**
     * 根据用户名查询项目组权限
     */
    public List<UserProjGroupPO> queryByUsername(String username) {
        return userProjGroupDao.queryByUsername(username);
    }
    
    /**
     * 根据组名查询用户项目组权限
     */
    public UserProjGroupPO queryByGroupName(String groupName) {
        return userProjGroupDao.queryByGroupName(groupName);
    }
    
    /**
     * 检查用户是否有指定项目组权限
     */
    public boolean hasGroupPermission(String userId, String username, String groupName) {
        return userProjGroupDao.isGroupNameExists(groupName, userId, username);
    }
    
    /**
     * 删除用户项目组权限
     */
    public void deleteUserProjGroup(String groupName, String userId, String username) {
        userProjGroupDao.deleteByGroupName(groupName, userId, username);
    }
    
    /**
     * 更新用户项目组权限
     */
    public void updateUserProjGroup(UserProjGroupPO updatePo, UserProjGroupPO wherePo) {
        userProjGroupDao.update(updatePo, wherePo);
    }
    
    /**
     * 批量删除用户的所有项目组权限
     */
    public void batchDeleteByUser(String userId, String username) {
        List<UserProjGroupPO> userGroups = queryByUserId(userId);
        for (UserProjGroupPO group : userGroups) {
            deleteUserProjGroup(group.getGroupName(), userId, username);
        }
    }
    
    /**
     * 获取用户的项目组权限列表（用于界面显示）
     */
    public List<UserProjGroupPO> getUserGroupPermissions(String userId, String username) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setUserId(userId);
        queryPo.setUsername(username);
        return queryUserProjGroups(queryPo);
    }
}
