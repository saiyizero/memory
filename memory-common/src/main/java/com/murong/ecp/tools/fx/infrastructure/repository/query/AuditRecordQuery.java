package com.murong.ecp.tools.fx.infrastructure.repository.query;

import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditRecordQuery extends AuditRecordPO {
    private String keyword;
    private String beginTime;
    private String endTime;
    /** 为 true 且未指定具体审核状态时，只查已通过 / 已驳回 */
    private Boolean auditedOnly;
}
