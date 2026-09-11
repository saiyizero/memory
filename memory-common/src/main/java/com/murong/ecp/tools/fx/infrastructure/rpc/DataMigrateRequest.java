package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import lombok.Data;

import java.util.List;

@Data
public class DataMigrateRequest {
    private DbConnectionPO source;
    private DbConnectionPO target;
    private List<String> tableList;
}
