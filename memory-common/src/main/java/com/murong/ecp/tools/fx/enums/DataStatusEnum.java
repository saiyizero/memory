package com.murong.ecp.tools.fx.enums;

public enum DataStatusEnum {
    PENDING("P", "待补全"),
    WAIT_AUDIT("W", "待审核"),
    REVIEW("R", "待评审"),
    UPD_DBTYP("T", "修改DbTyp"),
    UPD_LENGTH("L", "修改Length"),
    UPD_DBTYP_LENGTH("U", "修改DbTyp和Length"),
    COMPLETED("S", "已完成"),
    NORMAL("N", "正常");

    private final String code;
    private final String desc;

    DataStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DataStatusEnum getByCode(String code) {
        for (DataStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
