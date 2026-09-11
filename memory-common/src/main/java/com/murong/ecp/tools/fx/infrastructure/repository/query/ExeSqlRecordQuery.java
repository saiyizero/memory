package com.murong.ecp.tools.fx.infrastructure.repository.query;

import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import lombok.Data;

@Data
public class ExeSqlRecordQuery extends ExeSqlRecordPO {
    private String beginDt;
    private String endDt;
    private String keyword; // 支持关键字查询
}
