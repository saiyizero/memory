package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户信息 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class UserInfoDao extends HttpDaoSupport<UserInfoPO> {

    public List<UserInfoPO> queryAllUsers() {
        return invokeList("queryAllUsers", UserInfoPO.class);
    }

    public UserInfoPO queryByUsername(String username) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setUsername(username);
        return super.queryOne(queryPo);
    }

    public UserInfoPO queryByUserId(String userId) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setUserId(userId);
        return super.queryOne(queryPo);
    }

    public List<UserInfoPO> queryByRole(String role) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setRoles(role);
        return super.queryForList(queryPo, "create_time desc");
    }

    public List<UserInfoPO> queryByStatus(String status) {
        UserInfoPO queryPo = new UserInfoPO();
        queryPo.setStatus(status);
        return super.queryForList(queryPo, "create_time desc");
    }

    public boolean isUsernameExists(String username) {
        Boolean exists = invoke("isUsernameExists", Boolean.class, username);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isEmailExists(String email) {
        Boolean exists = invoke("isEmailExists", Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    public void save(UserInfoPO user) {
        super.insert(user);
    }

    public void deleteByUserId(String userId) {
        invokeVoid("deleteByUserId", userId);
    }

    public void batchDeleteByUserIds(List<String> userIds) {
        invokeVoid("batchDeleteByUserIds", userIds);
    }
}
