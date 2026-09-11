package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectGroupRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServerInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
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
     * 删除项目组
     */
    public void deleteProjectGroup(String groupName) {
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