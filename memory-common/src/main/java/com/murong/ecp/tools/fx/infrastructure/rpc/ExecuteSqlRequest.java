package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

@Data
public class ExecuteSqlRequest {
    private String sql;
    private String envName;
}
