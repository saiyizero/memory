package com.murong.ecp.tools.fx.enums;

/**
 * 用户状态枚举
 */
public enum UserStatusEnum {
    
    APPLY("A", "申请"),
    ONLINE("O", "启用"),
    DISABLED("D", "停止");
    
    private final String code;
    private final String desc;
    
    UserStatusEnum(String code, String desc) {
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
    public static UserStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 根据代码获取描述
     */
    public static String getDescByCode(String code) {
        UserStatusEnum status = getByCode(code);
        return status != null ? status.getDesc() : code;
    }
    
    /**
     * 根据描述获取代码
     */
    public static String getCodeByDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (UserStatusEnum status : values()) {
            if (status.getDesc().equals(desc)) {
                return status.getCode();
            }
        }
        return null;
    }

    public static String toCodeDesc(String code) {
        UserStatusEnum status = getByCode(code);
        return status == null ? code : status.getCode() + "-" + status.getDesc();
    }

    public static String[] displayValues() {
        UserStatusEnum[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].getCode() + "-" + values[i].getDesc();
        }
        return result;
    }
}
