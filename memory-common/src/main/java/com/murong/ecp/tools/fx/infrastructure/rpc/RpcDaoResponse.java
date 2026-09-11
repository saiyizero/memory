package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class RpcDaoResponse {
    private boolean success;
    private String message;
    private JsonNode data;

    public static RpcDaoResponse ok(JsonNode data) {
        RpcDaoResponse response = new RpcDaoResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static RpcDaoResponse fail(String message) {
        RpcDaoResponse response = new RpcDaoResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
