package com.murong.ecp.tools.fx.domain.service.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.http.MemoryHttpClient;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.*;
import com.murong.ecp.tools.fx.infrastructure.rpc.DbConnectionRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.LoginRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.ProjectFolderRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjGroupRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 登录验证服务。用户校验走 memory-service /api/auth/login。
 */
@Service
public class LoginService {

    @Autowired
    private MemoryHttpClient memoryHttpClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LocalSettingDao localSettingDao;

    @Autowired
    private GlobalProperties globalProperties;


    public void absGlobalPropes() {
        UserProjGroupRpcService userProjGroupRpcService = applicationContext.getBean(UserProjGroupRpcService.class);
        UserProjGroupPO userProjGrp = userProjGroupRpcService.queryCurGroup();
        if (userProjGrp == null && globalProperties.getOperator() != null) {
            List<UserProjGroupPO> groups = null;
            String userId = globalProperties.getOperator().getUserId();
            String username = globalProperties.getOperator().getUsername();
            if (StringUtils.isNotBlank(userId)) {
                groups = userProjGroupRpcService.queryByUserId(userId);
            }
            if ((groups == null || groups.isEmpty()) && StringUtils.isNotBlank(username)) {
                groups = userProjGroupRpcService.queryByUsername(username);
            }
            if (groups != null && !groups.isEmpty()) {
                userProjGrp = groups.get(0);
                userProjGroupRpcService.switchGroup(userProjGrp.getGroupName());
            }
        }
        if(userProjGrp!=null){
            globalProperties.setGroupName(userProjGrp.getGroupName());
            UserProjSettingRpcService userProjSettingRpcService = applicationContext.getBean(UserProjSettingRpcService.class);
            UserProjSettingPO userProjSetting = userProjSettingRpcService.queryCurProject(userProjGrp.getGroupName());
            if(userProjSetting!=null){
                globalProperties.setProjectName(userProjSetting.getProjectName());
                globalProperties.setAppPort(userProjSetting.getAppPort());
                globalProperties.setAppName(userProjSetting.getAppName());
                globalProperties.setBasePath(userProjSetting.getBasePath());

                DbConnectionPO dbConnReqPO = new DbConnectionPO();
                dbConnReqPO.setProjectName(userProjSetting.getProjectName());
                dbConnReqPO.setGroupName(userProjSetting.getGroupName());
                dbConnReqPO.setMainFlg("1");
                DbConnectionRpcService dbConnectionRpcService = applicationContext.getBean(DbConnectionRpcService.class);
                DbConnectionPO mainDbConn = dbConnectionRpcService.queryOne(dbConnReqPO);

                if (mainDbConn != null) {
                    DbConfig dbConfig = new DbConfig(mainDbConn);
                    globalProperties.setDbConfig(dbConfig);
                }else {
                    globalProperties.setDbConfig(null);
                }


                ProjectFolderPO projectFolderPO = new ProjectFolderPO();
                projectFolderPO.setProjectName(userProjSetting.getProjectName());
                projectFolderPO.setGroupName(userProjSetting.getGroupName());
                projectFolderPO.setAppName(userProjSetting.getAppName());
                ProjectFolderRpcService projectFolderRpcService = applicationContext.getBean(ProjectFolderRpcService.class);
                List<ProjectFolderPO> projectDirLst = projectFolderRpcService.queryForList(projectFolderPO);
                if (!CollectionUtils.isEmpty(projectDirLst)) {
                    globalProperties.setModules(projectDirLst);
                }
            }else {
                globalProperties.setProjectName(null);
                globalProperties.setDbConfig(null);
            }
        }else {
            globalProperties.setGroupName(null);
            globalProperties.setProjectName(null);
            globalProperties.setDbConfig(null);
        }
    }

    /**
     * 检查是否已经登录。true 表示本地缓存凭证已通过服务端校验，可跳过登录框。
     */
    public boolean requiresLogin() {
        LocalSettingPO settingPO = new LocalSettingPO();
        settingPO.setStatus(FlgEnum.YES.getValue());
        LocalSettingPO localSettingPO = localSettingDao.queryOne(settingPO);
        if (localSettingPO == null || StringUtils.isBlank(localSettingPO.getLinkUsrName())
                || StringUtils.isBlank(localSettingPO.getLinkPassWord())) {
            return false;
        }
        CrResult<UserInfoPO> result = validateLogin(localSettingPO.getLinkUsrName(), localSettingPO.getLinkPassWord());
        return result != null && result.isSucess();
    }

    /**
     * 验证登录信息
     */
    public CrResult<UserInfoPO> validateLogin(String username, String password) {

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            CrResult<UserInfoPO> crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            if(StringUtils.isBlank(username)) {
                crResult.setMsgInf("登录用户名称不允许为空");
            }
            if(StringUtils.isBlank(password)) {
                crResult.setMsgInf("登录密码不允许为空");
            }
            return crResult;
        }

        LoginRequest request = new LoginRequest();
        request.setUsername(username.trim());
        request.setPassword(password);
        CrResult<UserInfoPO> remote = memoryHttpClient.post("/api/auth/login", request, new TypeReference<CrResult<UserInfoPO>>() {
        });
        if (remote == null) {
            CrResult<UserInfoPO> crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("memory-service 无响应");
            return crResult;
        }
        if (!remote.isSucess() || remote.getData() == null) {
            return remote;
        }

        UserInfoPO userInfoPO = remote.getData();
        localSettingDao.updateLinkInfo(username,password);
        globalProperties.logIn(userInfoPO.getUserId(),userInfoPO.getUsername(), userInfoPO.getRealName(),userInfoPO.getRoles());
        absGlobalPropes();
        CrResult<UserInfoPO> crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        crResult.setData(userInfoPO);
        return crResult;
    }
}
