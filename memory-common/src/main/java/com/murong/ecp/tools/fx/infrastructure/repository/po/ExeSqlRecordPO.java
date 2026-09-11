package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="exesql_record")
public class ExeSqlRecordPO {
    private Integer id;           // 主键
    private String groupName;     // 分组名
    private String schemaNm;      // SCHEMA
    private String envName;       // 环境名
    private String exeDate;       // 执行日期
    private String exeTime;       // 执行时间
    private String exeSql;        // 执行sql
    private String status;        // 执行状态
    private String updateBy;      // 更新人
    private String updateTime;    // 更新时间
} 