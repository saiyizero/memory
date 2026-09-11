package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
