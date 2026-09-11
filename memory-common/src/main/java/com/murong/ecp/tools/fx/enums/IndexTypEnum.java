package com.murong.ecp.tools.fx.enums;

/**
 * 索引类型枚举
 */
public enum IndexTypEnum {
    /**
     * 主键索引
     */
    PRIMARY("primary", "主键索引"),
    
    /**
     * 普通索引
     */
    INDEX("index", "普通索引"),
    
    /**
     * 唯一索引
     */
    UNIQUE("unique", "唯一索引");
    
    private final String code;
    private final String desc;
    
    IndexTypEnum(String code, String desc) {
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
    public static IndexTypEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (IndexTypEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
