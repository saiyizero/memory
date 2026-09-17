package com.murong.ecp.tools.fx.domain.entity;

import com.murong.ecp.tools.fx.domain.service.database.DataBaseHandler;
import com.murong.ecp.tools.fx.enums.DirTypeEnum;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GlobalProperties {
    private String groupName;
    private String projectName;
    private String appPort;
    private String appName;
    private String basePath;
    private String propPath;
    private DbConfig dbConfig;
    private Operator operator;
    private List<CrEnum> enums;
    private List<CrMsgCode> msgCodes;

    private List<ModuleDb> moduleDbs;
    private List<ModuleBiz> moduleBizs;
    private List<ModuleApi> moduleApis;

    public ModuleDb getMainModuleDb(){
        if(CollectionUtils.isNotEmpty(moduleDbs)){
            for (ModuleDb moduleDb : moduleDbs) {
                if(StringUtils.equals(moduleDb.getMainFlg(), FlgEnum.YES.getValue())){
                    return moduleDb;
                }
            }
            return null;
        }else {
            return null;
        }
    }

    public void logIn(String userId,String username,String realname, String roles) {
        if(this.operator == null) {
            this.operator =new Operator();
        }
        this.operator.setUserId(userId);
        this.operator.setUsername(username);
        this.operator.setRealname(realname);
        this.operator.setRoles(roles);
    }

    public String getGroupName() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && ctx.getGroupName() != null) {
            return ctx.getGroupName();
        }
        return this.groupName;
    }

    public String getProjectName() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && ctx.getProjectName() != null) {
            return ctx.getProjectName();
        }
        return this.projectName;
    }

    public String getAppName() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && ctx.getAppName() != null) {
            return ctx.getAppName();
        }
        return this.appName;
    }

    public Operator getOperator() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && (ctx.getUsername() != null || ctx.getUserId() != null)) {
            Operator requestOperator = new Operator();
            requestOperator.setUserId(ctx.getUserId());
            requestOperator.setUsername(ctx.getUsername());
            requestOperator.setRealname(ctx.getRealName());
            requestOperator.setRoles(ctx.getRoles());
            return requestOperator;
        }
        return this.operator;
    }

    public DbConfig getDbConfig() {
        var ctx = com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext.get();
        if (ctx != null && ctx.getDbConfig() != null) {
            return ctx.getDbConfig();
        }
        return this.dbConfig;
    }

    public void setModules(List<ProjectFolderPO> moduleLst) {
        if (moduleLst == null || moduleLst.isEmpty()) {
            return;
        }
        
        // 初始化模块列表
        this.moduleDbs = new ArrayList<>();
        this.moduleBizs = new ArrayList<>();
        this.moduleApis = new ArrayList<>();
        this.enums = new ArrayList<>();
        this.msgCodes = new ArrayList<>();

        // 按模块名称分组
        Map<String, List<ProjectFolderPO>> moduleGroups = new HashMap<>();
        for (ProjectFolderPO po : moduleLst) {
            moduleGroups.computeIfAbsent(po.getModuleName(), k -> new ArrayList<>()).add(po);
        }
        
        // 处理每个模块
        for (Map.Entry<String, List<ProjectFolderPO>> entry : moduleGroups.entrySet()) {
            List<ProjectFolderPO> moduleFolders = entry.getValue();

            // 创建模块配置对象（根据需要）
            ModuleApi moduleApi = null;
            ModuleBiz moduleBiz = null;
            ModuleDb moduleDb = null;

            // 处理模块中的每个目录类型
            for (ProjectFolderPO po : moduleFolders) {
                DirTypeEnum dirType = DirTypeEnum.getByType(po.getDirType());
                if (dirType != null) {
                    String moduleType = dirType.getModule();
                    
                    switch (dirType) {
                        case ENUMS:
                            CrEnum crEnum = new CrEnum();
                            crEnum.setUrl(po.getDirPath());
                            crEnum.setMainFlg(po.getMainFlg());
                            crEnum.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            enums.add(crEnum);
                            break;
                        case MSG_CODE:
                            CrMsgCode crMsgCode = new CrMsgCode();
                            crMsgCode.setUrl(po.getDirPath());
                            crMsgCode.setMainFlg(po.getMainFlg());
                            crMsgCode.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            msgCodes.add(crMsgCode);
                            break;
                        case INTERFACE:
                            if (moduleApi == null) {
                                moduleApi=new ModuleApi();
                            }
                            moduleApi.setInterfaces(po.getDirPath());
                            moduleApi.setMainFlg(po.getMainFlg());
                            moduleApi.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case REQUEST:
                            if (moduleApi == null) {
                                moduleApi=new ModuleApi();
                            }
                            moduleApi.setRequest(po.getDirPath());
                            moduleApi.setMainFlg(po.getMainFlg());
                            moduleApi.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case RESPONSE:
                            if (moduleApi == null) {
                                moduleApi=new ModuleApi();
                            }
                            moduleApi.setResponse(po.getDirPath());
                            moduleApi.setMainFlg(po.getMainFlg());
                            moduleApi.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case ACTION:
                            if (moduleBiz == null) {
                                moduleBiz=new ModuleBiz();
                            }
                            moduleBiz.setAction(po.getDirPath());
                            moduleBiz.setMainFlg(po.getMainFlg());
                            moduleBiz.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case CONTROLLER:
                            if (moduleBiz == null) {
                                moduleBiz=new ModuleBiz();
                            }
                            moduleBiz.setController(po.getDirPath());
                            moduleBiz.setMainFlg(po.getMainFlg());
                            moduleBiz.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case DOMAIN:
                            if (moduleBiz == null) {
                                moduleBiz=new ModuleBiz();
                            }
                            moduleBiz.setDomain(po.getDirPath());
                            moduleBiz.setMainFlg(po.getMainFlg());
                            moduleBiz.setBasePath(po.getModuleName() + File.separator + po.getDirBase());
                            break;
                        case ENTITY:
                            if (moduleDb == null) {
                                moduleDb=new ModuleDb();
                            }
                            moduleDb.setEntity(po.getDirPath());
                            moduleDb.setMainFlg(po.getMainFlg());
                            moduleDb.setBasePath(po.getModuleName());
                            break;
                        case MAPPER:
                            if (moduleDb == null) {
                                moduleDb=new ModuleDb();
                            }
                            moduleDb.setMapper(po.getDirPath());
                            moduleDb.setMainFlg(po.getMainFlg());
                            moduleDb.setBasePath(po.getModuleName());
                            break;
                        case XML:
                            if (moduleDb == null) {
                                moduleDb=new ModuleDb();
                            }
                            moduleDb.setXml(po.getDirPath());
                            moduleDb.setMainFlg(po.getMainFlg());
                            moduleDb.setBasePath(po.getModuleName());
                            break;
                        case PROP_PATH:
                            // 配置文件路径
                            this.propPath = po.getModuleName()+File.separator+po.getDirBase()+File.separator+po.getDirPath();
                            break;
                    }
                }
            }
            
            // 添加模块到相应的列表
            if (moduleApi != null) {
                moduleApis.add(moduleApi);
            }
            if (moduleBiz != null) {
                moduleBizs.add(moduleBiz);
            }
            if (moduleDb != null) {
                moduleDbs.add(moduleDb);
            }
        }
        System.out.println("the end");
    }

    public GlobalProperties(String groupName, String projectName,DbConfig dbConfig) {
        this.groupName = groupName;
        this.projectName = projectName;
        this.dbConfig = dbConfig;
    }

    @Data
    public class Operator {
        private String userId;
        private String username;
        private String realname;
        private String roles;
    }

    @Data
    public class ModuleDb {
        private String mainFlg;
        private String basePath;
        private String entity;
        private String mapper;
        private String xml;
    }

    @Data
    public class ModuleBiz {
        private String mainFlg;
        private String basePath;
        private String action;
        private String controller;
        private String domain;
    }

    @Data
    public static class ModuleApi {
        private String mainFlg;
        private String basePath;
        private String interfaces;
        private String request;
        private String response;
    }

    @Data
    public static class CrEnum {
        private String mainFlg;
        private String basePath;
        private String url;
    }

    @Data
    public static class CrMsgCode {
        private String mainFlg;
        private String basePath;
        private String url;
    }

    public String getDDLSchema(){
        return getInputDataBaseHandler().getSchema();
    }

    public DataBaseHandler getInputDataBaseHandler() {
        if(this.getDbConfig() == null){
           throw new RuntimeException("无法连接数据库，当前项目主数据源为空");
        }
        try {
            DataBaseHandler httpHandler = MrSpringContextHolder.getBean("httpDataBaseHandler", DataBaseHandler.class);
            if (httpHandler != null) {
            httpHandler.setDbConfig(getDbConfig());
            return httpHandler;
            }
        } catch (Exception ignored) {
            // service 侧没有 HTTP handler
        }
        String dbHandlerName = dbConfig.getDataBaseHandlerName();
        DataBaseHandler databaseHandler = MrSpringContextHolder.getBean(dbHandlerName, DataBaseHandler.class);
        databaseHandler.setDbConfig(getDbConfig());
        return databaseHandler;
    }

} 