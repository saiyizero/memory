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
}
