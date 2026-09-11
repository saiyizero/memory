package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="db_connection")
public class DbConnectionPO {
    private String groupName;     // 分组名
    private String projectName;   // 项目名称
    private String appName;       // app名称
    private String envName;       // 环境名称
    private String dbName;        // 数据库名称
    private String driver;        // JDBC驱动类名
    private String jdbcUrl;       // 连接URL
    private String username;      // 用户名
    private String password;      // 密码（建议加密）
    private String schemaNm;      // SCHEMA
    private String mainFlg;       // 主数据库标识
    private String remark;        // 备注
    private String updateBy;      // 更新人
    private String updateTime;    // 更新时间
} 