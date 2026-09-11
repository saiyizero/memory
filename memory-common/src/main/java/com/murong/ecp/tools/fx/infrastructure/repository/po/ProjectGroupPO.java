package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="project_group")
public class ProjectGroupPO {
    private String groupName;      // 分组名
    private String groupDesc;      // 分组描述
    private String curFlag;        // 当前标志
} 