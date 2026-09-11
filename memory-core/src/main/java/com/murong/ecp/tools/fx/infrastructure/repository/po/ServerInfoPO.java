package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="server_info")
public class ServerInfoPO {
    private String groupName;      // 分组名
    private String projectName;    // 项目名称
    private String appName;        // 应用名称
    private String envName;        // 环境名称
    private String appPort;        // 应用端口
    private String ip;             // IP地址
    private String port;           // 端口
    private String username;       // 用户名
    private String password;       // 密码
    private String appPath;        // 应用路径
    private String appProp;        // 应用属性
    private Integer scanFlg;       // 扫描标志
    private String updateBy;       // 更新人
    private String updateTime;     // 更新时间
} 