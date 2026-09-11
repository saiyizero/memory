package com.murong.ecp.tools.fx.enums;

/**
 * @author yao
 */
public enum SuccessFailureEnum {
    /**
     * 成功
     */
    SUCCESS("success", "成功"),
    /**
     * 失败
     */
    FAILURE("failure", "失败");

    private final String code;
    private final String desc;

    SuccessFailureEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
} 