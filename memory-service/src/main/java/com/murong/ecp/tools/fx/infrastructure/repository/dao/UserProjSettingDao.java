package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Repository
public class UserProjSettingDao extends DaoSupport<UserProjSettingPO> {

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

    /**
     * 保存用户项目设置
     */
    public void save(UserProjSettingPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        super.insert(po);
    }

    /**
     * 查询用户项目设置列表
     */
    public List<UserProjSettingPO> queryForList(UserProjSettingPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        return super.queryForList(po, "update_time desc");
    }

    /**
     * 根据用户ID查询项目设置
     */
    public List<UserProjSettingPO> queryByUserId(String userId) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        return super.queryForList(queryPo, "update_time desc");
    }

    /**
     * 根据用户名查询项目设置
     */
    public List<UserProjSettingPO> queryByUsername(String username) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUsername(username);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    /**
     * 根据组名查询项目设置
     */
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

    /**
     * 根据项目名查询项目设置
     */
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

    /**
     * 根据应用名查询项目设置
     */
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

    /**
     * 查询用户当前项目设置
     */
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

    /**
     * 查询显示的项目设置
     */
    public List<UserProjSettingPO> queryShowProjects(String userId, String username) {
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setUserId(userId);
        queryPo.setUsername(username);
        queryPo.setShowFlag("Y");
        return super.queryForList(queryPo, "update_time desc");
    }

    /**
     * 检查项目设置是否存在
     */
    public boolean isProjectSettingExists(String groupName, String projectName, String appName, String userId, String username) {
        String sql = "select count(*) from user_proj_setting where group_name = ? and project_name = ? and app_name = ? and user_id = ? and username = ?";
        try {
            Integer count = queryCount(sql, groupName, projectName, appName, userId, username);
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除用户项目设置
     */
    public void deleteByProject(String groupName, String projectName, String appName, String userId, String username) {
        String sql = "delete from user_proj_setting where group_name = ? and project_name = ? and app_name = ? and user_id = ? and username = ?";
        super.updateBySql(sql, groupName, projectName, appName, userId, username);
    }

    /**
     * 更新用户项目设置
     */
    public void update(UserProjSettingPO updatePo, UserProjSettingPO wherePo) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            wherePo.setUserId(currentUser.getUserId());
            wherePo.setUsername(currentUser.getUsername());
        }
        super.updateByOne(updatePo, wherePo);
    }

    /**
     * 批量删除用户项目设置
     */
    public void batchDeleteByUser(String userId, String username) {
        String sql = "delete from user_proj_setting where user_id = ? and username = ?";
        super.updateBySql(sql, userId, username);
    }
}
