package com.murong.ecp.tools.fx.enums;

public enum FlgEnum {
    YES("Y", "是"),
    NO("N", "否"),;

    private final String value;
    private final String desc;

    FlgEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getValue() {
        return value;
    }
}
