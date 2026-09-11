package com.murong.ecp.tools.fx.enums;

import java.util.Arrays;
import java.util.List;

public enum BackupSetTypEnum {
    //参数初始化备份
    INIT("init","初始化备份", Arrays.asList("project_group", "project_setting", "user_preference")),

    //整库备份
    ALL("all","完整备份", Arrays.asList("biz_dict", "common_class",
            "db_connection","debug_log", "enum_dict", "enum_dict_his",
            "exesql_record","interface_data", "interface_data_his",
            "project_folder","project_group","project_setting", "server_info",
            "table_data","table_diff","table_record", "user_preference")),

    //知识库备份
    DICT("dict","知识库备份", Arrays.asList("biz_dict", "enum_dict")),
    ;

    private final String code;
    private final String desc;
    private final List<String> list;

    BackupSetTypEnum(String code,String desc, List<String> list) {
        this.code = code;
        this.desc = desc;
        this.list = list;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public List<String> getList() {
        return list;
    }
}
