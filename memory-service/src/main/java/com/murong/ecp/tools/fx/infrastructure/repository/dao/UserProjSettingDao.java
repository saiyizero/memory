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
        if (gp != null && gp.getOperator() != null
                && (gp.getOperator().getUserId() != null || gp.getOperator().getUsername() != null)) {
            UserInfoPO po = new UserInfoPO();
            po.setUserId(gp.getOperator().getUserId());
            po.setUsername(gp.getOperator().getUsername());
            return po;
        }
        return null;
    }

    /**
     * 保存用户项目设置。
     * 管理员给其他用户分配时会带上目标 userId/username，此时不能覆盖成当前登录人。
     */
    public void save(UserProjSettingPO po) {
        fillCurrentUserIfAbsent(po);
        super.insert(po);
    }

    /**
     * 查询用户项目设置列表。
     * 未指定用户时默认查当前登录人；已指定则按目标用户查询（供管理员查看/分配）。
     */
    public List<UserProjSettingPO> queryForList(UserProjSettingPO po) {
        fillCurrentUserIfQueryUnspecified(po);
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
        List<UserProjSettingPO> projects = queryMineByGroup(groupName);
        if (projects == null || projects.isEmpty()) {
            return null;
        }
        for (UserProjSettingPO project : projects) {
            if (project != null && "Y".equalsIgnoreCase(project.getCurFlag())) {
                return project;
            }
        }
        return projects.get(0);
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
        fillCurrentUserIfQueryUnspecified(wherePo);
        super.updateByOne(updatePo, wherePo);
    }

    private void fillCurrentUserIfAbsent(UserProjSettingPO po) {
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

    private void fillCurrentUserIfQueryUnspecified(UserProjSettingPO po) {
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
        if (!isBlank(currentUser.getUserId())) {
            po.setUserId(currentUser.getUserId());
        } else {
            po.setUsername(currentUser.getUsername());
        }
    }

    /**
     * 查询当前用户在指定项目组下可显示的项目。userId / username 任一匹配即可。
     */
    public List<UserProjSettingPO> queryMineByGroup(String groupName) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null || isBlank(groupName)) {
            return new java.util.ArrayList<>();
        }
        if (!isBlank(currentUser.getUserId()) && !isBlank(currentUser.getUsername())) {
            String sql = "select * from user_proj_setting where group_name = ? and (show_flag = 'Y' or show_flag is null) "
                    + "and (user_id = ? or username = ?) order by update_time desc";
            return distinctProjects(super.queryListBySql(sql, UserProjSettingPO.class,
                    groupName, currentUser.getUserId(), currentUser.getUsername()));
        }
        UserProjSettingPO queryPo = new UserProjSettingPO();
        queryPo.setGroupName(groupName);
        if (!isBlank(currentUser.getUserId())) {
            queryPo.setUserId(currentUser.getUserId());
        } else {
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryForList(queryPo, "update_time desc");
    }

    /**
     * 切换当前用户在该项目组下的当前项目。
     */
    public void switchProject(String groupName, String projectName) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser == null || isBlank(groupName) || isBlank(projectName)) {
            return;
        }
        if (!isBlank(currentUser.getUserId())) {
            super.updateBySql("update user_proj_setting set cur_flag='N' where group_name=? and user_id=?",
                    groupName, currentUser.getUserId());
            super.updateBySql("update user_proj_setting set cur_flag='Y' where group_name=? and project_name=? and user_id=?",
                    groupName, projectName, currentUser.getUserId());
        } else if (!isBlank(currentUser.getUsername())) {
            super.updateBySql("update user_proj_setting set cur_flag='N' where group_name=? and username=?",
                    groupName, currentUser.getUsername());
            super.updateBySql("update user_proj_setting set cur_flag='Y' where group_name=? and project_name=? and username=?",
                    groupName, projectName, currentUser.getUsername());
        }
    }

    private List<UserProjSettingPO> distinctProjects(List<UserProjSettingPO> projects) {
        if (projects == null || projects.isEmpty()) {
            return projects == null ? new java.util.ArrayList<>() : projects;
        }
        java.util.Map<String, UserProjSettingPO> unique = new java.util.LinkedHashMap<>();
        for (UserProjSettingPO project : projects) {
            if (project == null) {
                continue;
            }
            String key = String.valueOf(project.getGroupName()) + "|" + project.getProjectName() + "|" + project.getAppName();
            unique.putIfAbsent(key, project);
        }
        return new java.util.ArrayList<>(unique.values());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 批量删除用户项目设置
     */
    public void batchDeleteByUser(String userId, String username) {
        String sql = "delete from user_proj_setting where user_id = ? and username = ?";
        super.updateBySql(sql, userId, username);
    }
}
