package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name = "audit_record")
public class AuditRecordPO {
    private String id;
    private String groupName;
    private String projectName;
    private String appName;
    private String bizType;
    private String bizKey;
    private String bizName;
    private String operType;
    private String auditStatus;
    private String oldData;
    private String newData;
    private String submitBy;
    private String submitTime;
    private String auditBy;
    private String auditTime;
    private String auditRemark;
    private String updateBy;
    private String updateTime;
}
