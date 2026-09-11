package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="debug_log")
public class DebugLogGroupPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String interfaceName;    // 接口名称
    private String transName;        // 交易名称
    private String transNmeDsc;      // 交易名称描述
}
