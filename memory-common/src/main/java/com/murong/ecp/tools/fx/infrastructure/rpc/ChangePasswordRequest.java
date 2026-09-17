package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
