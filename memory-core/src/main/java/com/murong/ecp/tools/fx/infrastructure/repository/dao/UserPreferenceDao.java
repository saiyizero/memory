package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.LocalSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserPreferencePO;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPreferenceDao extends LocalSupport<UserPreferencePO> {

    /**
     * 保存用户偏好设置
     */
    public void save(UserPreferencePO po) {
        super.insert(po);
    }

    /**
     * 查询用户偏好设置列表
     */
    public List<UserPreferencePO> queryForList(UserPreferencePO po) {
        return super.queryForList(po);
    }

    /**
     * 查询单个用户偏好设置
     */
    public UserPreferencePO queryOne(UserPreferencePO po) {
        return super.queryOne(po);
    }

    /**
     * 根据用户ID和偏好键查询偏好设置
     */
    public UserPreferencePO queryByUserIdAndKey(String userId, String preferenceKey) {
        UserPreferencePO queryPo = new UserPreferencePO();
        queryPo.setUserId(userId);
        queryPo.setPreferenceKey(preferenceKey);
        return super.queryOne(queryPo);
    }

    /**
     * 更新用户偏好设置
     */
    public void updateByOne(UserPreferencePO updatePo, UserPreferencePO wherePo) {
        super.updateByOne(updatePo, wherePo);
    }

    /**
     * 删除用户偏好设置
     */
    public void delete(UserPreferencePO po) {
        super.delete(po);
    }
} 