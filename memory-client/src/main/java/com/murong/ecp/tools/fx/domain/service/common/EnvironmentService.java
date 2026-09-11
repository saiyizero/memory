package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.rpc.DbConnectionRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectFolderRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class EnvironmentService {
    @Autowired
    private ProjectSettingRpcService projectSettingRpcService;
    @Autowired
    private ProjectFolderRpcService projectFolderRpcService;
    @Autowired
    private DbConnectionRpcService dbConnectionRpcService;

    /**
     * 切换当前激活项目，保证cur_flag唯一
     */
    public void switchCurrentEnv(String groupName, String projectName) {
        projectSettingRpcService.switchProject(groupName, projectName);
    }

    public void rebuildGlobalPropes(GlobalProperties globalPropes, ProjectSettingPO projectEnv){
        DbConnectionPO dbConnReqPO = new DbConnectionPO();
        dbConnReqPO.setProjectName(projectEnv.getProjectName());
        dbConnReqPO.setGroupName(projectEnv.getGroupName());
        dbConnReqPO.setMainFlg("1");
        DbConnectionPO mainDbConn = dbConnectionRpcService.queryOne(dbConnReqPO);
        if(mainDbConn != null){
            DbConfig dbConfig = new DbConfig(mainDbConn);
            globalPropes.setDbConfig(dbConfig);
        }else {
            globalPropes.setDbConfig(null);
        }

        //设置接口、项目简称
        globalPropes.setGroupName(projectEnv.getGroupName());
        globalPropes.setAppPort(projectEnv.getAppPort());
        globalPropes.setAppName(projectEnv.getAppName());
        globalPropes.setProjectName(projectEnv.getProjectName());
        globalPropes.setBasePath(projectEnv.getBasePath());

        //设置项目模块路径信息
        ProjectFolderPO projectFolderPO = new ProjectFolderPO();
        projectFolderPO.setProjectName(projectEnv.getProjectName());
        projectFolderPO.setGroupName(projectEnv.getGroupName());
        projectFolderPO.setAppName(projectEnv.getAppName());
        List<ProjectFolderPO> projectDirLst = projectFolderRpcService.queryForList(projectFolderPO);
        if (!CollectionUtils.isEmpty(projectDirLst)) {
            globalPropes.setModules(projectDirLst);
        }
    }
}
