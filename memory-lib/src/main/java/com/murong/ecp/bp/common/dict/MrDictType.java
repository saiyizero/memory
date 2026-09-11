package com.murong.ecp.bp.common.dict;

public enum MrDictType {
    NUMBER("N", "数字型0-9.-"),
    DIGIT("9", "数字型0-9"),
    AMT("A", "金额"),
    CHAR("X", "字符型(任何字符)"),
    DATE("D", "日期型"),
    TIME("T", "时间类型"),
    MBLNO("M", "手机号"),
    EMAIL("E", "邮箱");

    private final String value;
    private final String desc;

    private MrDictType(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
