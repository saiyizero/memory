package com.murong.ecp.tools.fx.domain.service.auth;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.enums.UserStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.*;
import com.murong.ecp.tools.fx.infrastructure.repository.po.*;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 登录验证服务
 */
@Service
public class LoginService {

    @Autowired
    private UserInfoDao userInfoDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LocalSettingDao localSettingDao;

    @Autowired
    private GlobalProperties globalProperties;


    public void absGlobalPropes() {
        // 延迟初始化模块信息，避免循环依赖
        UserProjGroupDao userProjGroupDao = applicationContext.getBean(UserProjGroupDao.class);
        UserProjGroupPO userProjGrp = userProjGroupDao.queryCurGroup();
        if(userProjGrp!=null){
            globalProperties.setGroupName(userProjGrp.getGroupName());
            UserProjSettingDao userProjSettingDao = applicationContext.getBean(UserProjSettingDao.class);
            UserProjSettingPO userProjSetting = userProjSettingDao.queryCurProject(userProjGrp.getGroupName());
            if(userProjSetting!=null){
                globalProperties.setProjectName(userProjSetting.getProjectName());
                globalProperties.setAppPort(userProjSetting.getAppPort());
                globalProperties.setAppName(userProjSetting.getAppName());
                globalProperties.setBasePath(userProjSetting.getBasePath());

                //初始化数据库
                DbConnectionPO dbConnReqPO = new DbConnectionPO();
                dbConnReqPO.setProjectName(userProjSetting.getProjectName());
                dbConnReqPO.setGroupName(userProjSetting.getGroupName());
                dbConnReqPO.setMainFlg("1");
                DbConnectionDao dbConnectionDao = applicationContext.getBean(DbConnectionDao.class);
                DbConnectionPO mainDbConn = dbConnectionDao.queryOne(dbConnReqPO);

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
                ProjectFolderDao projectFolderDao = applicationContext.getBean(ProjectFolderDao.class);
                List<ProjectFolderPO> projectDirLst = projectFolderDao.queryForList(projectFolderPO);
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
     * 检查是否需要登录
     * 如果登录信息表为空，则需要登录
     */
    public boolean requiresLogin() {
        LocalSettingPO settingPO = new LocalSettingPO();
        settingPO.setStatus(FlgEnum.YES.getValue());
        LocalSettingPO localSettingPO = localSettingDao.queryOne(settingPO);
        UserInfoPO userInfoPO = userInfoDao.queryByUsername(localSettingPO.getLinkUsrName());
        if(userInfoPO!=null){
            if(StringUtils.equals(userInfoPO.getPassword(),localSettingPO.getLinkPassWord())){
                globalProperties.logIn(userInfoPO.getUserId(),userInfoPO.getUsername(),userInfoPO.getRealName(),userInfoPO.getRoles());
                absGlobalPropes();
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    /**
     * 验证登录信息
     */
    public CrResult validateLogin(String username, String password) {

        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            if(StringUtils.isBlank(username)) {
                crResult.setMsgInf("登录用户名称不允许为空");
            }
            if(StringUtils.isBlank(password)) {
                crResult.setMsgInf("登录密码不允许为空");
            }
            return crResult;
        }

        UserInfoPO userInfoPO = userInfoDao.queryByUsername(username);
        if (userInfoPO != null) {
            if(!StringUtils.equals(password,userInfoPO.getPassword())) {
                CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                crResult.setMsgInf("密码验证失败");
                return crResult;
            }

            if(!StringUtils.equals(userInfoPO.getStatus(), UserStatusEnum.ONLINE.getCode())) {
                CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                crResult.setMsgInf("用户状态错误请联系管理员");
                return crResult;
            }

            localSettingDao.updateLinkInfo(username,password);
            globalProperties.logIn(userInfoPO.getUserId(),userInfoPO.getUsername(), userInfoPO.getRealName(),userInfoPO.getRoles());
            absGlobalPropes();
        }else {
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("用户不存在");
        }

        CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        return crResult;
    }
}
