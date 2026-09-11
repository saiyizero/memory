package com.murong.ecp.tools.fx.infrastructure.repository.query;

import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogPO;
import lombok.Data;

@Data
public class DebugLogQuery extends DebugLogPO {
    private String beginDt;
    private String endDt;
}
