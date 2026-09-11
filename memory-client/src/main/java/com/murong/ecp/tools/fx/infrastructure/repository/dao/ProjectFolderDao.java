package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProjectFolderDao extends HttpDaoSupport<ProjectFolderPO> {
    @Autowired
    private GlobalProperties globalPropes;

    public List<ProjectFolderPO> searchByName(String name) {
        if(StringUtils.isBlank(name)) {
            ProjectFolderPO projectFolderPO = new ProjectFolderPO();
            projectFolderPO.setGroupName(globalPropes.getGroupName());
            return queryForList(projectFolderPO);
        }else {
            String hql = "select * from project_folder where group_name ='" + globalPropes.getGroupName() + "'";
            return queryListBySql(hql, ProjectFolderPO.class);
        }
    }

    public List<ProjectFolderPO> searchByCondition(String moduleName, String dirType, String dirPath) {
        List<ProjectFolderPO> all = searchByName("");
        List<ProjectFolderPO> result = new ArrayList<>();
        for (ProjectFolderPO po : all) {
            boolean match = true;
            if (moduleName != null && !moduleName.trim().isEmpty() && (po.getModuleName() == null || !po.getModuleName().contains(moduleName))) {
                match = false;
            }
            if (dirType != null && !dirType.trim().isEmpty() && (po.getDirType() == null || !po.getDirType().contains(dirType))) {
                match = false;
            }
            if (dirPath != null && !dirPath.trim().isEmpty() && (po.getDirPath() == null || !po.getDirPath().contains(dirPath))) {
                match = false;
            }
            if (match) {
                result.add(po);
            }
        }
        return result;
    }

    public ProjectFolderPO queryOne(ProjectFolderPO po) {
        String sql = "select * from project_folder where group_name = '" + po.getGroupName() + 
                "' AND project_name = '" + po.getProjectName() + "' AND module_name = '" + po.getModuleName() + 
                "' AND dir_type = '" + po.getDirType() + "' LIMIT 1";
        return queryOneBySql(sql);
    }

    public void save(ProjectFolderPO po) {
        ProjectFolderPO orgProjectFolderPO = queryOne(po);
        if (orgProjectFolderPO == null) {
            insert(po);
        } else {
            // 更新现有记录
            ProjectFolderPO wherePO = new ProjectFolderPO();
            wherePO.setGroupName(orgProjectFolderPO.getGroupName());
            wherePO.setProjectName(orgProjectFolderPO.getProjectName());
            wherePO.setModuleName(orgProjectFolderPO.getModuleName());
            wherePO.setDirType(orgProjectFolderPO.getDirType());
            if(StringUtils.equals(po.getMainFlg(), FlgEnum.YES.getValue())) {
                ProjectFolderPO updPO = new ProjectFolderPO();
                updPO.setMainFlg(FlgEnum.NO.getValue());
                ProjectFolderPO whereAllPO = new ProjectFolderPO();
                whereAllPO.setGroupName(orgProjectFolderPO.getGroupName());
                whereAllPO.setProjectName(orgProjectFolderPO.getProjectName());
                whereAllPO.setDirType(orgProjectFolderPO.getDirType());
                updateByOne(updPO, whereAllPO);
            }
            updateByOne(po, wherePO);
        }
    }

    public List<ProjectFolderPO> searchByProjectName(String projectName) {
        ProjectFolderPO po = new ProjectFolderPO();
        po.setProjectName(projectName);
        po.setGroupName(globalPropes.getGroupName());
        return queryForList(po);
    }

    public List<ProjectFolderPO> searchByModuleName(String projectName, String moduleName) {
        ProjectFolderPO po = new ProjectFolderPO();
        po.setProjectName(projectName);
        po.setGroupName(globalPropes.getGroupName());
        po.setModuleName(moduleName);
        return queryForList(po);
    }

    public List<ProjectFolderPO> searchByDirBase(String projectName, String moduleName, String dirBase) {
        ProjectFolderPO po = new ProjectFolderPO();
        po.setProjectName(projectName);
        po.setGroupName(globalPropes.getGroupName());
        po.setModuleName(moduleName);
        po.setDirBase(dirBase);
        return queryForList(po);
    }
} 