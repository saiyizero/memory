package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="enum_dict")
public class EnumDictPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String moduleName;       // 模块名称
    private String enumNme;          // 枚举名称
    private String enumRef;          // 枚举引用
    private String enumCd;           // 枚举代码
    private String enumVal;          // 枚举值
    private String descCn;           // 中文描述
    private String descEn;           // 英文描述
    private String dbName;           // 数据库名称
    private String appName;          // 应用名称
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
} 