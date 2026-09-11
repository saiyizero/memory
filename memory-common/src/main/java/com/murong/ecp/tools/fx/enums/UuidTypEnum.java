package com.murong.ecp.tools.fx.enums;

public enum UuidTypEnum {
    TABLE_REC("TAB", "表更新记录ID"),
    ;

    private final String key;
    private final String value;

    UuidTypEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
