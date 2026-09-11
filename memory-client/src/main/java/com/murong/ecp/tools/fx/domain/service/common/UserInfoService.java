package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.infrastructure.rpc.UserInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {
    
    @Autowired
    private UserInfoRpcService userInfoRpcService;
    
    /**
     * 查询所有用户
     */
    public List<UserInfoPO> queryAllUsers() {
        return userInfoRpcService.queryAllUsers();
    }
    
    /**
     * 根据用户ID查询用户
     */
    public UserInfoPO queryByUserId(String userId) {
        return userInfoRpcService.queryByUserId(userId);
    }
    
    /**
     * 根据用户名查询用户
     */
    public UserInfoPO queryByUsername(String username) {
        return userInfoRpcService.queryByUsername(username);
    }
    
    /**
     * 根据角色查询用户
     */
    public List<UserInfoPO> queryByRole(String role) {
        return userInfoRpcService.queryByRole(role);
    }
    
    /**
     * 根据状态查询用户
     */
    public List<UserInfoPO> queryByStatus(String status) {
        return userInfoRpcService.queryByStatus(status);
    }
    
    /**
     * 保存用户
     */
    public void saveUser(UserInfoPO user) {
        // 设置创建时间和更新时间
        user.setUpdateTime(MrDateUtils.getCurrentTime());
        
        // 如果没有设置用户ID，自动生成
        if (StringUtils.isBlank(user.getUserId())) {
            user.setUserId(MrDateUtils.getCurrentTimeLongStr());
        }
        
        // 设置默认状态
        if (StringUtils.isBlank(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        
        userInfoRpcService.save(user);
    }
    
    /**
     * 更新用户
     */
    public void updateUser(UserInfoPO user) {
        user.setUpdateTime(MrDateUtils.getCurrentTime());

        UserInfoPO whereUsr = new UserInfoPO();
        whereUsr.setUserId(user.getUserId());
        userInfoRpcService.updateByOne(user,whereUsr);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        userInfoRpcService.deleteByUserId(userId);
    }
    
    /**
     * 批量删除用户
     */
    public void batchDeleteUsers(List<String> userIds) {
        userInfoRpcService.batchDeleteByUserIds(userIds);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean isUsernameExists(String username) {
        return userInfoRpcService.isUsernameExists(username);
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean isEmailExists(String email) {
        return userInfoRpcService.isEmailExists(email);
    }
    
    /**
     * 验证用户登录
     */
    public UserInfoPO validateLogin(String username, String password) {
        UserInfoPO user = userInfoRpcService.queryByUsername(username);
        if (user != null && StringUtils.equals(user.getPassword(), password) 
            && "ACTIVE".equals(user.getStatus())) {
            return user;
        }
        return null;
    }
    
    /**
     * 修改用户状态
     */
    public void updateUserStatus(String userId, String status) {
        UserInfoPO updUser = new UserInfoPO();
        updUser.setStatus(status);
        updUser.setUpdateTime(MrDateUtils.getCurrentTime());

        UserInfoPO whereUsr = new UserInfoPO();
        whereUsr.setUserId(userId);
        userInfoRpcService.updateByOne(updUser,whereUsr);
    }
    
    /**
     * 修改用户密码
     */
    public void updateUserPassword(String userId, String newPassword) {
        UserInfoPO updUser = new UserInfoPO();
        updUser.setPassword(newPassword);
        updUser.setUpdateTime(MrDateUtils.getCurrentTime());

        UserInfoPO whereUsr = new UserInfoPO();
        whereUsr.setUserId(userId);
        userInfoRpcService.updateByOne(updUser,whereUsr);
    }
    
    /**
     * 修改用户角色
     */
    public void updateUserRole(String userId, String newRole) {
        UserInfoPO updUser = new UserInfoPO();
        updUser.setRoles(newRole);
        updUser.setUpdateTime(MrDateUtils.getCurrentTime());

        UserInfoPO whereUsr = new UserInfoPO();
        whereUsr.setUserId(userId);
        userInfoRpcService.updateByOne(updUser,whereUsr);
    }
}
