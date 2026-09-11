package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目设置 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class ProjectSettingRpcService extends HttpDaoSupport<ProjectSettingPO> {

    public void save(ProjectSettingPO po) {
        super.insert(po);
    }

    public List<ProjectSettingPO> queryForList(ProjectSettingPO po) {
        return super.queryForList(po);
    }

    public ProjectSettingPO queryCurProject(String groupName){
        ProjectSettingPO projectSettingPO = new ProjectSettingPO();
        projectSettingPO.setGroupName(groupName);
        projectSettingPO.setCurFlag("Y");
        return super.queryOne(projectSettingPO);
    }

    public void switchProject(String groupName,String projectName) {
        invokeVoid("switchProject", groupName, projectName);
    }

    public void deleteProjectSetting(String groupName, String projectName) {
        invokeVoid("deleteProjectSetting", groupName, projectName);
    }

    public void updateShowFlag(String groupName, String projectName, String showFlag) {
        invokeVoid("updateShowFlag", groupName, projectName, showFlag);
    }
}
