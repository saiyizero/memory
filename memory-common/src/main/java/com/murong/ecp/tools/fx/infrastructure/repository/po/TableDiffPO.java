package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import lombok.Data;

/**
 * 表结构差异记录持久化对象
 */
@Data
@JTable(name="table_diff")
public class TableDiffPO {
    private String groupName;             // 分组名
    private String projectName;           // 项目名称
    private String appName;               // 服务器名称
    private String tableNameCamel;        // 表驼峰名称
    private String tableNameSnake;        // 表下划线名称
    private String schemaNm;              // Schema名称
    private String compareEnv;            // 对比环境
    private DiffTypeEnum diffType;        // 差异类型枚举
    private String localValue;            // 本地表结构DDL内容
    private String remoteValue;           // 远程表结构DDL内容
    private Integer diffFieldCount;       // 字段差异数量
    private String updateBy;              // 更新人
    private String updateTime;            // 更新时间
} 