package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class RpcDaoRequest {
    /**
     * PO 全限定类名
     */
    private String entityType;
    /**
     * 实体 JSON
     */
    private JsonNode entity;
    /**
     * 更新条件实体 JSON
     */
    private JsonNode whereEntity;
    private String sql;
    private List<Object> params;
    private String orderBy;
    private String resultType;
    /**
     * 服务端 DAO 全限定类名，用于 /api/dao/invoke
     */
    private String daoType;
    /**
     * 要调用的 DAO 方法名
     */
    private String methodName;
    /**
     * 方法参数 JSON 数组
     */
    private JsonNode methodArgs;
}
