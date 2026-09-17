package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserProjGroupDao extends DaoSupport<UserProjGroupPO> {

    /**
     * 获取当前用户信息
     */
    private UserInfoPO getCurrentUser() {
        com.murong.ecp.tools.fx.domain.entity.GlobalProperties gp =
                com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder.getBean(
                        com.murong.ecp.tools.fx.domain.entity.GlobalProperties.class);
        if (gp != null && gp.getOperator() != null && gp.getOperator().getUsername() != null) {
            UserInfoPO po = new UserInfoPO();
            po.setUserId(gp.getOperator().getUserId());
            po.setUsername(gp.getOperator().getUsername());
            return po;
        }
        return null;
    }

    public UserProjGroupPO queryCurGroup() {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        String sql = "select * from user_proj_group where cur_flag='Y' and user_id='"+
                currentUser.getUserId()+"' and username='"+currentUser.getUsername()+"'";
        return super.queryOneBySql(sql);
    }

    public UserProjGroupPO queryCurGroup(LocalSettingPO localSettingPO) {
        String sql = "select * from user_proj_group where cur_flag='Y' and username='"+localSettingPO.getLinkUsrName()+"'";
        return super.queryOneBySql(sql);
    }

    public void switchGroup(String groupName) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String sql = "update user_proj_group set cur_flag='N' where user_id='"+
        currentUser.getUserId()+"' and username='"+currentUser.getUsername()+"'";
        super.updateBySql(sql);
        String updateSql = "update user_proj_group set cur_flag='Y' where group_name='"+groupName+"' and user_id='"+
                currentUser.getUserId()+"' and username='"+currentUser.getUsername()+"'";
        super.updateBySql(updateSql);
    }

    /**
     * 保存用户项目组。
     * 管理员给其他用户分配时会带上目标 userId/username，此时不能覆盖成当前登录人。
     */
    public void save(UserProjGroupPO po) {
        fillCurrentUserIfAbsent(po);
        super.insert(po);
    }

    /**
     * 查询用户项目组列表。
     * 未指定用户时默认查当前登录人；已指定则按目标用户查询（供管理员查看/分配）。
     */
    public List<UserProjGroupPO> queryForList(UserProjGroupPO po) {
        fillCurrentUserIfQueryUnspecified(po);
        return super.queryForList(po, "group_name asc");
    }

    /**
     * 根据用户ID查询项目组
     */
    public List<UserProjGroupPO> queryByUserId(String userId) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setUserId(userId);
        return super.queryForList(queryPo, "group_name asc");
    }

    /**
     * 根据用户名查询项目组
     */
    public List<UserProjGroupPO> queryByUsername(String username) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setUsername(username);
        return super.queryForList(queryPo, "group_name asc");
    }

    /**
     * 根据组名查询项目组
     */
    public UserProjGroupPO queryByGroupName(String groupName) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setGroupName(groupName);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryOne(queryPo);
    }

    /**
     * 检查组名是否存在
     */
    public boolean isGroupNameExists(String groupName, String userId, String username) {
        String sql = "select count(*) from user_proj_group where group_name = ? and user_id = ? and username = ?";
        try {
            Integer count = queryCount(sql, groupName, userId, username);
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除用户项目组
     */
    public void deleteByGroupName(String groupName, String userId, String username) {
        String sql = "delete from user_proj_group where group_name = ? and user_id = ? and username = ?";
        super.updateBySql(sql, groupName, userId, username);
    }

    /**
     * 更新用户项目组
     */
    public void update(UserProjGroupPO updatePo, UserProjGroupPO wherePo) {
        fillCurrentUserIfQueryUnspecified(wherePo);
        super.updateByOne(updatePo, wherePo);
    }

    private void fillCurrentUserIfAbsent(UserProjGroupPO po) {
        if (po == null) {
            return;
        }
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }
        if (isBlank(po.getUserId())) {
            po.setUserId(currentUser.getUserId());
        }
        if (isBlank(po.getUsername())) {
            po.setUsername(currentUser.getUsername());
        }
    }

    private void fillCurrentUserIfQueryUnspecified(UserProjGroupPO po) {
        if (po == null) {
            return;
        }
        if (!isBlank(po.getUserId()) || !isBlank(po.getUsername())) {
            return;
        }
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }
        po.setUserId(currentUser.getUserId());
        po.setUsername(currentUser.getUsername());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
