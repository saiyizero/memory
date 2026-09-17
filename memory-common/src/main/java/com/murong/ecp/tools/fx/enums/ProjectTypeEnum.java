package com.murong.ecp.tools.fx.enums;

/**
 * 项目类型枚举
 */
public enum ProjectTypeEnum {

    CORE("C", "核心项目"),
    VIEW("V", "视图项目"),
    PLATFORM("P", "平台项目"),
    CHANNEL("L", "渠道项目");

    private final String code;
    private final String desc;

    ProjectTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String toDisplay() {
        return code + "-" + desc;
    }

    public static ProjectTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProjectTypeEnum type : values()) {
            if (type.getCode().equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }

    public static ProjectTypeEnum parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        ProjectTypeEnum byCode = getByCode(text);
        if (byCode != null) {
            return byCode;
        }
        for (ProjectTypeEnum type : values()) {
            if (type.getDesc().equals(text) || type.toDisplay().equals(text)) {
                return type;
            }
        }
        if (text.length() >= 1) {
            ProjectTypeEnum prefix = getByCode(text.substring(0, 1));
            if (prefix != null && text.startsWith(prefix.getCode())) {
                return prefix;
            }
        }
        return null;
    }

    public static String toCode(String value) {
        ProjectTypeEnum type = parse(value);
        return type == null ? value : type.getCode();
    }

    public static String toCodeDesc(String value) {
        ProjectTypeEnum type = parse(value);
        if (type != null) {
            return type.toDisplay();
        }
        return value == null ? "" : value;
    }

    public static String[] displayValues() {
        ProjectTypeEnum[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toDisplay();
        }
        return result;
    }
}
