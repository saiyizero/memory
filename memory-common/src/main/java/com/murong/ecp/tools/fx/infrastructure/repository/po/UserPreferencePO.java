package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 用户偏好设置持久化对象
 */
@Data
@JTable(name="user_preference")
public class UserPreferencePO {
    private String id;                    // 主键ID
    private String userId;                // 用户ID
    private String preferenceKey;         // 偏好设置键
    private String preferenceValue;       // 偏好设置值
    private String description;           // 描述
    private String updateBy;              // 更新人
    private String updateTime;            // 更新时间
} 