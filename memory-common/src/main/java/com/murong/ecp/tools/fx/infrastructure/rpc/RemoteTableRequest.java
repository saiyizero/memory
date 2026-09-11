package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

import java.util.List;

@Data
public class RemoteTableRequest {
    private String tableName;
    private String compareEnv;
    private List<String> tableNames;
}
