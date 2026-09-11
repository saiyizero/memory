package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserInfoDao extends DaoSupport<UserInfoPO> {

    /**
     * 查询所有用户
     */
    public List<UserInfoPO> queryAllUsers() {
        String sql = "select * from user_info order by create_time desc";
        return super.queryListBySql(sql, UserInfoPO.class);
    }

    /**
     * 根据用户名查询用户
     */
    public UserInfoPO queryByUsername(String username) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setUsername(username);
        return super.queryOne(queryPo);
    }

    /**
     * 根据用户ID查询用户
     */
    public UserInfoPO queryByUserId(String userId) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setUserId(userId);
        return super.queryOne(queryPo);
    }

    /**
     * 根据角色查询用户
     */
    public List<UserInfoPO> queryByRole(String role) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setRoles(role);
        return super.queryForList(queryPo, "create_time desc");
    }

    /**
     * 根据状态查询用户
     */
    public List<UserInfoPO> queryByStatus(String status) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setStatus(status);
        return super.queryForList(queryPo, "create_time desc");
    }

    /**
     * 检查用户名是否存在
     */
    public boolean isUsernameExists(String username) {
        String sql = "select count(*) from user_info where username = ?";
        try {
            Integer count = queryCount(sql, username);
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean isEmailExists(String email) {
        String sql = "select count(*) from user_info where email = ?";
        try {
            Integer count = queryCount(sql, email);
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存用户
     */
    public void save(UserInfoPO user) {
        super.insert(user);
    }

    /**
     * 删除用户
     */
    public void deleteByUserId(String userId) {
        String sql = "delete from user_info where user_id = ?";
        super.updateBySql(sql, userId);
    }

    /**
     * 批量删除用户
     */
    public void batchDeleteByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
        String sql = "delete from user_info where user_id in (" + placeholders + ")";
        super.updateBySql(sql, userIds.toArray());
    }
}
