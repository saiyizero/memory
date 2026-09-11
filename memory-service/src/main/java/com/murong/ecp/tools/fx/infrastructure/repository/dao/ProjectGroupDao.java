package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectGroupPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProjectGroupDao extends DaoSupport<ProjectGroupPO> {

    public ProjectGroupPO queryCurGroup() {
        String sql = "select * from project_group where cur_flag='Y'";
        return super.queryOneBySql(sql);
    }

    public List<ProjectGroupPO> queryAllGroups() {
        String sql = "select * from project_group order by group_name";
        return super.queryListBySql(sql, ProjectGroupPO.class);
    }
    
    /**
     * 保存项目组
     */
    public void save(ProjectGroupPO projectGroup) {
        super.insert(projectGroup);
    }
    
    /**
     * 删除项目组
     */
    public void deleteProjectGroup(String groupName) {
        String sql = "delete from project_group where group_name=?";
        super.updateBySql(sql, groupName);
    }
    
    /**
     * 根据项目组名称查询项目组
     */
    public ProjectGroupPO queryByGroupName(String groupName) {
        ProjectGroupPO queryPo = new ProjectGroupPO();
        queryPo.setGroupName(groupName);
        return super.queryOne(queryPo);
    }

} 