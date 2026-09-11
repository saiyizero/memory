package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProjectSettingDao extends DaoSupport<ProjectSettingPO> {

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
        String sql = "update project_setting set cur_flag='N' where group_name='"+groupName+"'";
        super.updateBySql(sql);
        String updateSql = "update project_setting set cur_flag='Y' " +
                "where group_name='"+groupName+"' and project_name='"+projectName+"'";
        super.updateBySql(updateSql);
    }

    public void deleteProjectSetting(String groupName, String projectName) {
        String sql = "delete from project_setting where group_name=? and project_name=?";
        super.updateBySql(sql, groupName, projectName);
    }

    public void updateShowFlag(String groupName, String projectName, String showFlag) {
        String sql = "update project_setting set show_flag=? where group_name=? and project_name=?";
        super.updateBySql(sql, showFlag, groupName, projectName);
    }
} 