package com.murong.ecp.tools.fx.enums;

public enum DirTypeEnum {
    ENUMS("enums", null,"枚举路径"),
    MSG_CODE("msgcode", null,"枚举路径"),
    PROP_PATH("prop-path",null, "配置文件路径"),
    INTERFACE("interface","ModuleApi", "接口"),
    REQUEST("request", "ModuleApi","请求"),
    RESPONSE("response", "ModuleApi","响应"),
    ACTION("action", "ModuleBiz","交易主目录"),
    DOMAIN("domain", "ModuleBiz","领域"),
    CONTROLLER("controller", "ModuleBiz","转接层"),
    ENTITY("entity", "ModuleDb","持久化对象"),
    MAPPER("mapper", "ModuleDb","持久化映射"),
    XML("xml", "ModuleDb","持久化xml"),
    ;



    private final String type;
    private final String module
            ;
    private final String desc;

    DirTypeEnum(String type,String module, String desc) {
        this.type = type;
        this.module = module;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getModule() {
        return module;
    }

    public static DirTypeEnum getByType(String value) {
        for (DirTypeEnum dirTypeEnum : DirTypeEnum.values()) {
            if (dirTypeEnum.getType().equals(value)) {
                return dirTypeEnum;
            }
        }
        return null;
    }
}
