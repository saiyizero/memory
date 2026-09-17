package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectGroupRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServerInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectGroupService {
    
    @Autowired
    private ProjectGroupRpcService projectGroupRpcService;
    @Autowired
    private ServerInfoRpcService serverInfoRpcService;
    
    @Autowired
    private ProjectSettingRpcService projectSettingRpcService;
    @Autowired
    private UserProjSettingRpcService userProjSettingRpcService;
    
    /**
     * 查询所有项目群
     */
    public List<ProjectGroupPO> queryAllProjectGroups() {
        return projectGroupRpcService.queryAllGroups();
    }
    
    /**
     * 查询当前选中的项目群
     */
    public ProjectGroupPO queryCurrentProjectGroup() {
        return projectGroupRpcService.queryCurGroup();
    }
    
    /**
     * 根据项目群名称查询项目设置列表
     */
    public List<ProjectSettingPO> queryProjectSettingsByGroupName(String groupName) {
        ProjectSettingPO queryPo = new ProjectSettingPO();
        queryPo.setGroupName(groupName);
        return projectSettingRpcService.queryForList(queryPo);
    }
    
    /**
     * 保存项目设置
     */
    public void saveProjectSetting(ProjectSettingPO projectSetting) {
        projectSettingRpcService.save(projectSetting);
    }

    /**
     * 更新项目设置（项目名称作为主键不允许修改）
     */
    public void updateProjectSetting(ProjectSettingPO projectSetting) {
        ProjectSettingPO where = new ProjectSettingPO();
        where.setGroupName(projectSetting.getGroupName());
        where.setProjectName(projectSetting.getProjectName());

        ProjectSettingPO update = new ProjectSettingPO();
        update.setProjectType(projectSetting.getProjectType());
        update.setProjectDesc(projectSetting.getProjectDesc());
        update.setAppName(projectSetting.getAppName());
        update.setAppPort(projectSetting.getAppPort());
        update.setSchemaNm(projectSetting.getSchemaNm());
        projectSettingRpcService.updateByOne(update, where);
    }

    /**
     * 查询当前用户在该项目下的扫描路径
     */
    public String queryCurrentUserBasePath(String groupName, String projectName) {
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(projectName)) {
            return "";
        }
        UserProjSettingPO query = new UserProjSettingPO();
        query.setGroupName(groupName);
        query.setProjectName(projectName);
        List<UserProjSettingPO> list = userProjSettingRpcService.queryForList(query);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        }
        return StringUtils.defaultString(list.get(0).getBasePath());
    }

    /**
     * 更新当前用户在该项目下的扫描路径
     */
    public void updateCurrentUserBasePath(String groupName, String projectName, String appName, String basePath) {
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(projectName)) {
            return;
        }
        UserProjSettingPO where = new UserProjSettingPO();
        where.setGroupName(groupName);
        where.setProjectName(projectName);
        if (StringUtils.isNotBlank(appName)) {
            where.setAppName(appName);
        }
        List<UserProjSettingPO> existing = userProjSettingRpcService.queryForList(where);
        if (existing == null || existing.isEmpty()) {
            return;
        }
        UserProjSettingPO update = new UserProjSettingPO();
        update.setBasePath(StringUtils.defaultString(basePath));
        userProjSettingRpcService.update(update, where);
    }
    
    /**
     * 切换项目
     */
    public void switchProject(String groupName, String projectName) {
        projectSettingRpcService.switchProject(groupName, projectName);
    }
    
    /**
     * 删除项目设置
     */
    public void deleteProjectSetting(String groupName, String projectName) {
        projectSettingRpcService.deleteProjectSetting(groupName, projectName);
    }
    
    /**
     * 更新项目显示标志
     */
    public void updateProjectShowFlag(ProjectSettingPO setting, String showFlag) {
        String scanFlg = "1";
        if (!StringUtils.equals(showFlag, FlgEnum.YES.getValue())) {
            scanFlg="0";
        }
        serverInfoRpcService.updateScanFlg(setting.getGroupName(), setting.getAppName(), scanFlg);
        projectSettingRpcService.updateShowFlag(setting.getGroupName(), setting.getProjectName(), showFlag);
    }
    
    /**
     * 保存项目组
     */
    public void saveProjectGroup(ProjectGroupPO projectGroup) {
        projectGroupRpcService.save(projectGroup);
    }
    
    /**
     * 根据项目组名称查询项目组
     */
    public ProjectGroupPO queryProjectGroupByGroupName(String groupName) {
        return projectGroupRpcService.queryByGroupName(groupName);
    }
    
    /**
     * 保存或更新项目组（根据groupName查询，存在则更新，不存在则新增）
     */
    public void saveOrUpdateProjectGroup(ProjectGroupPO projectGroup) {
        if (StringUtils.isBlank(projectGroup.getGroupName())) {
            return; // 项目组名称为空时不处理
        }
        // 根据groupName查询是否存在
        ProjectGroupPO existingGroup = projectGroupRpcService.queryByGroupName(projectGroup.getGroupName());
        
        if (existingGroup != null) {
            ProjectGroupPO updateGroupPO = new ProjectGroupPO();
            updateGroupPO.setGroupDesc(projectGroup.getGroupDesc());
            ProjectGroupPO whereGroupPO = new ProjectGroupPO();
            whereGroupPO.setGroupName(projectGroup.getGroupName());
            projectGroupRpcService.updateByOne(updateGroupPO, whereGroupPO);
        } else {
            // 不存在则新增
            projectGroupRpcService.save(projectGroup);
        }
    }
    
    /**
     * 删除项目组，同时删除该组下的项目设置
     */
    public void deleteProjectGroup(String groupName) {
        ProjectSettingPO settingWhere = new ProjectSettingPO();
        settingWhere.setGroupName(groupName);
        projectSettingRpcService.delete(settingWhere);
        projectGroupRpcService.deleteProjectGroup(groupName);
    }
    
    /**
     * 根据项目组名称和项目名称查询项目设置
     */
    public ProjectSettingPO queryProjectSettingByGroupAndName(String groupName, String projectName) {
        ProjectSettingPO queryPo = new ProjectSettingPO();
        queryPo.setGroupName(groupName);
        queryPo.setProjectName(projectName);
        return projectSettingRpcService.queryOne(queryPo);
    }
} 