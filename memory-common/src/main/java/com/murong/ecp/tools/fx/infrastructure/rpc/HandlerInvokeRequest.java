package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import lombok.Data;

@Data
public class HandlerInvokeRequest {
    private String sql;
    private String tableName;
    private TableEntity tableEntity;
    private CommonClassPO commonClassPO;
    private String dbType;
    private Integer length;
    private String javaType;
}
