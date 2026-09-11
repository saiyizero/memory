package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.LocalSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LoginInfoPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LoginInfoDao extends LocalSupport<LoginInfoPO> {

    /**
     * 根据用户名查询登录信息
     */
    public LoginInfoPO queryByUsername(String username) {
        LoginInfoPO query = new LoginInfoPO();
        query.setUsername(username);
        return super.queryOne(query);
    }

    /**
     * 查询所有有效的登录信息
     */
    public List<LoginInfoPO> queryAllActive() {
        LoginInfoPO query = new LoginInfoPO();
        query.setStatus("active");
        return super.queryForList(query);
    }

    /**
     * 检查是否存在登录信息
     */
    public boolean hasLoginInfo() {
        return !super.queryForList(new LoginInfoPO()).isEmpty();
    }

    /**
     * 验证用户名和密码
     */
    public LoginInfoPO validateLogin(String username, String password) {
        LoginInfoPO query = new LoginInfoPO();
        query.setUsername(username);
        query.setPassword(password);
        query.setStatus("active");
        return super.queryOne(query);
    }
}
