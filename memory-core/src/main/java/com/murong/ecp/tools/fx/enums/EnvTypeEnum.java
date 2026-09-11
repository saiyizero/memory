package com.murong.ecp.tools.fx.enums;

/**
 * 环境类型枚举
 */
public enum EnvTypeEnum {
    
    DEV("dev", "开发环境"),
    SIT("sit", "测试环境"),
    UAT("uat", "UAT环境"),
    POC("poc", "POC环境");
    
    private final String code;
    private final String desc;
    
    EnvTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static EnvTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (EnvTypeEnum envType : values()) {
            if (envType.getCode().equals(code)) {
                return envType;
            }
        }
        return null;
    }
    
    /**
     * 根据代码获取描述
     */
    public static String getDescByCode(String code) {
        EnvTypeEnum envType = getByCode(code);
        return envType != null ? envType.getDesc() : null;
    }
}
