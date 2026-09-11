package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="debug_log")
public class DebugLogPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String appName;          // 应用名称
    private String interfaceName;    // 接口名称
    private String transName;        // 交易名称
    private String transNmeDsc;      // 交易名称描述
    private String jrnNo;            // 流水号
    private String method;           // 方法
    private String url;              // URL
    private String reqParam;         // 请求参数
    private String rspParam;         // 响应参数
    private String headers;          // 请求头
    private String msgCode;          // 消息代码
    private String debugDesc;        // 调试描述
    private String sequence;         // 序列号
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
    private String requestId;        // 请求ID
    private String ip;               // IP地址
    private String msgInf;           // 消息信息
} 