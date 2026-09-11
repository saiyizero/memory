package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="msg_code")
public class MsgCodePO {
    private String msgKey;        // 消息键
    private String msgCd;         // 消息代码
    private String msgInf;        // 消息信息
    private String appName;       // 应用名称
    private String updateBy;      // 更新人
    private String updateTime;    // 更新时间
}
