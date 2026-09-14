package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="user_info")
public class UserInfoPO {
    private String userId;         // 用户ID
    private String username;       // 用户名
    private String password;       // 密码
    private String realName;       // 真实姓名
    private String email;          // 邮箱
    private String phone;          // 电话
    private String roles;          // 角色：D-开发者，M-管理员，C-审批者
    private String status;         // 状态：ACTIVE-激活，INACTIVE-禁用
    private String updateBy;       // 更新人
    private String updateTime;     // 更新时间
    private String remark;         // 备注
}