package com.murong.ecp.tools.fx.enums;

public enum AuditOperTypeEnum {
    ADD("ADD", "新增"),
    UPDATE("UPD", "修改"),
    DELETE("DEL", "删除");

    private final String code;
    private final String desc;

    AuditOperTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AuditOperTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditOperTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getDescByCode(String code) {
        AuditOperTypeEnum type = getByCode(code);
        return type == null ? code : type.getDesc();
    }
}
