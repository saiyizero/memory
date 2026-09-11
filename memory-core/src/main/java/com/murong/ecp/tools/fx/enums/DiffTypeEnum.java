package com.murong.ecp.tools.fx.enums;

/**
 * 表结构差异类型枚举
 */
public enum DiffTypeEnum {

    STRUCTURE_DIFF("STRUCTURE_DIFF", "结构差异"),
    NO_DIFF("NO_DIFF", "无差异");
    
    private final String code;
    private final String desc;
    
    DiffTypeEnum(String code, String desc) {
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
    public static DiffTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (DiffTypeEnum diffType : values()) {
            if (diffType.getCode().equals(code)) {
                return diffType;
            }
        }
        return null;
    }
    
    /**
     * 根据代码获取描述
     */
    public static String getDescByCode(String code) {
        DiffTypeEnum diffType = getByCode(code);
        return diffType != null ? diffType.getDesc() : null;
    }
} 