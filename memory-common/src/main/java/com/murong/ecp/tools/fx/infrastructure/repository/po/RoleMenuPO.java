package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 角色可见菜单配置。
 */
@Data
@JTable(name = "role_menu")
public class RoleMenuPO {
    private String roleCode;
    private String menuKey;
    private String menuName;
    private String groupKey;
    private String groupName;
    private Integer sortNo;
    private String showFlag;
    private String updateBy;
    private String updateTime;
}
