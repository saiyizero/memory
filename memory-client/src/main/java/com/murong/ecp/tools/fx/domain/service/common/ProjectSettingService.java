package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectSettingService {
    
    @Autowired
    private ProjectSettingRpcService projectSettingRpcService;
    
    /**
     * 根据项目组名称和项目名称查询项目设置
     */
    public ProjectSettingPO queryProjectSettingByGroupAndName(String groupName, String projectName) {
        ProjectSettingPO queryPo = new ProjectSettingPO();
        queryPo.setGroupName(groupName);
        queryPo.setProjectName(projectName);
        return projectSettingRpcService.queryOne(queryPo);
    }
    
    /**
     * 保存项目设置
     */
    public void saveProjectSetting(ProjectSettingPO projectSetting) {
        projectSettingRpcService.save(projectSetting);
    }
    
    /**
     * 查询所有项目设置
     */
    public List<ProjectSettingPO> queryAllProjectSettings() {
        return projectSettingRpcService.queryForList(new ProjectSettingPO());
    }
    
    /**
     * 根据项目组名称查询项目设置列表
     */
    public List<ProjectSettingPO> queryProjectSettingsByGroupName(String groupName) {
        ProjectSettingPO queryPo = new ProjectSettingPO();
        queryPo.setGroupName(groupName);
        return projectSettingRpcService.queryForList(queryPo);
    }
}
