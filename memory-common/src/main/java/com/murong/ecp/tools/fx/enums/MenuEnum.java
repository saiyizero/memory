package com.murong.ecp.tools.fx.enums;

public enum MenuEnum {
    TRANSACTION_API("transactionApi", "交易接口"),
    TABLE_MANAGER("tableManager", "表结构"),
    DATASOURCE_MANAGER("datasourceManager", "数据源"),
    EXESQL("exesql", "SQL执行"),
    PROJECT_GROUP("projectGroup", "项目组"),
    LOG_SERVICE("logService", "日志服务"),
    TEXT_EDITOR("textEditor", "文本编辑"),
    BASIC_CONFIG("basicConfig", "基础配置"),
    CHANGE_PASSWORD("changePassword", "修改密码"),
    ENUM("enum", "枚举维护"),
    BASE_DICT("baseDict", "基础字典"),
    BIZ_DICT("bizDict", "业务字典"),
    INFO_CODE("infoCode", "信息码"),
    COMMON_OBJ("commonObj", "公共对象"),
    TRANSACTION_LABLE("transactionLable", "交易标签"),
    TRANSACTION_REVIEW("transactionReview", "交易评审"),
    EXE_RECORD("exeRecord", "执行记录"),
    TRANSACTION_TEST("transactionTest", "测试记录"),
    PROJECT_PARAM("projectStructure", "项目结构"),
    USER_MANAGEMENT("userManagement", "用户管理"),
    MENU_MANAGEMENT("menuManagement", "菜单管理"),
    GENERATE_TRANS("generateTrans", "交易生成"),
    AI_PROMPT("AiPrompt", "AI提示词"),
    USER_PROJ_PERMISSION("userProjPermission", "用户项目权限"),
    TABLE_DIFF("tableDiff", "结构对比"),
    KNOWLEDGE("knowledge", "知识库"),
    TERMINAL("terminal", "终端");

    private final String key;
    private final String value;

    MenuEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static String getTitleByKey(String key) {
        for (MenuEnum e : MenuEnum.values()) {
            if (e.getKey().equals(key)) {
                return e.getValue();
            }
        }
        return key;
    }
}
