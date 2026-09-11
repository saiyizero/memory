package com.murong.ecp.tools.fx.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库类型枚举
 * 用于基础字典管理中的数据库类型选择
 */
public enum DatabaseTypeEnum {
    // 字符串类型
    VARCHAR("varchar", "可变长度字符串"),
    TEXT("text", "长文本类型"),
    
    // 数值类型 - 整数
    INTEGER("integer", "整数类型（同INT）"),
    
    // 数值类型 - 浮点数
    NUMERIC("numeric", "数值类型"),
    
    // 布尔类型
    BOOLEAN("boolean", "布尔类型"),
    
    // 日期时间类型
    DATE("DATE", "日期类型");
    

    private final String typeName;
    private final String description;

    DatabaseTypeEnum(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据类型名称获取枚举
     * @param typeName 类型名称
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static DatabaseTypeEnum getByTypeName(String typeName) {
        for (DatabaseTypeEnum type : values()) {
            if (type.getTypeName().equals(typeName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取所有类型名称列表
     * @return 类型名称列表
     */
    public static List<String> getAllTypeNames() {
        return Arrays.stream(values())
                .map(DatabaseTypeEnum::getTypeName)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return typeName;
    }
}
