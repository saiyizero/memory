package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
}
