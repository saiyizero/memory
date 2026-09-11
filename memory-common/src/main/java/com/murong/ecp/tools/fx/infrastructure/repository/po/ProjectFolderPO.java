package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="project_folder")
public class ProjectFolderPO {
    private String groupName;      // 分组名
    private String projectName;    // 项目名称
    private String appName;        // 服务器名称
    private String moduleName;     // 模块名称
    private String dirBase;        // 目录基础路径
    private String dirType;        // 目录类型
    private String dirPath;        // 目录路径
    private String mainFlg;        // 主标志
    private String updateBy;       // 更新人
    private String updateTime;     // 更新时间
} 