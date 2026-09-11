package com.murong.ecp.tools.fx.enums;

public enum ExeStatusEnum {
    INIT("I", "初始"),      // 初始
    SUCCESS("S", "执行成功"),   // 执行成功
    FAIL("F", "执行失败"),    // 执行失败
    STORED("C", "存储完成");     // 存储完成

    private final String key;
    private final String value;

    ExeStatusEnum(String key, String value) {
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
