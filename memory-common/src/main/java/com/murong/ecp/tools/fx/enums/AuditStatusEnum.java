package com.murong.ecp.tools.fx.enums;

public enum AuditStatusEnum {
    PENDING("P", "待审核"),
    APPROVED("A", "已通过"),
    REJECTED("R", "已驳回");

    private final String code;
    private final String desc;

    AuditStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AuditStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescByCode(String code) {
        AuditStatusEnum status = getByCode(code);
        return status == null ? code : status.getDesc();
    }
}
