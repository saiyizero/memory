package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目目录 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class ProjectFolderRpcService extends HttpDaoSupport<ProjectFolderPO> {
    @Autowired
    private GlobalProperties globalPropes;

    public List<ProjectFolderPO> searchByName(String name) {
        return invokeList("searchByName", ProjectFolderPO.class, name);
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
        return invoke("queryOne", ProjectFolderPO.class, po);
    }

    public void save(ProjectFolderPO po) {
        invokeVoid("save", po);
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
