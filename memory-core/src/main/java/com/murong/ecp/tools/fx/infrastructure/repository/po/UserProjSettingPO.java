package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="user_proj_setting")
public class UserProjSettingPO {
    private String groupName;        // 组名称
    private String projectName;      // 项目名称
    private String projectType;      // 项目类型
    private String projectDesc;      // 项目描述
    private String appName;          // 应用名称
    private String appPort;          // 应用端口
    private String userId;           // 用户ID
    private String username;         // 用户名
    private String schemaNm;         // 模式名称
    private String basePath;         // 基础路径
    private String propPath;         // 属性路径
    private String enumPath;         // 枚举路径
    private String msgcdPath;        // 消息代码路径
    private String curFlag;          // 当前标志
    private String showFlag;         // 显示标志
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
}
