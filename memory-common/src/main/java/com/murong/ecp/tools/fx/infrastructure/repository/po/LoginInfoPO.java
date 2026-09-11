package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 登录信息持久化对象
 */
@Data
@JTable(name="login_info")
public class LoginInfoPO {
    private String id;                    // 主键ID
    private String username;              // 用户名
    private String password;              // 密码（建议加密）
    private String displayName;           // 显示名称
    private String email;                 // 邮箱
    private String phone;                 // 电话
    private String role;                  // 角色
    private String status;                // 状态（active/inactive）
    private String lastLoginTime;         // 最后登录时间
    private String lastLoginIp;           // 最后登录IP
    private String updateBy;              // 更新人
    private String updateTime;            // 更新时间
}
