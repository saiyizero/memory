package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="user_proj_group")
public class UserProjGroupPO {
    private String groupName;        // 组名称
    private String groupDesc;        // 组描述
    private String userId;           // 用户ID
    private String username;         // 用户名
    private String curFlag;          // 当前标志
}
