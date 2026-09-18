package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 业务消息信息持久化对象
 */
@Data
@JTable(name="biz_msg_info")
public class BizMsgInfoPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String moduleName;       // 模块名称
    private String msgClass;         // 消息类
    private String msgRef;           // 消息引用
    private String msgKey;           // 消息键
    private String msgCd;            // 消息代码
    private String msgDescCn;        // 消息中文描述
    private String msgDescEn;        // 消息英文描述
    private String appName;          // 应用名称
    private String status;           // 数据状态
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
} 