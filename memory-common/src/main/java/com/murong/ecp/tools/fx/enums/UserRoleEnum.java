package com.murong.ecp.tools.fx.enums;

/**
 * 用户角色枚举
 */
public enum UserRoleEnum {
    
    DEVELOPER("D", "开发者"),
    MANAGER("M", "管理者");
    
    private final String code;
    private final String desc;
    
    UserRoleEnum(String code, String desc) {
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
    public static UserRoleEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
    
    /**
     * 根据代码获取描述
     */
    public static String getDescByCode(String code) {
        UserRoleEnum role = getByCode(code);
        return role != null ? role.getDesc() : code;
    }
    
    /**
     * 根据描述获取代码
     */
    public static String getCodeByDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (UserRoleEnum role : values()) {
            if (role.getDesc().equals(desc)) {
                return role.getCode();
            }
        }
        return null;
    }
}
