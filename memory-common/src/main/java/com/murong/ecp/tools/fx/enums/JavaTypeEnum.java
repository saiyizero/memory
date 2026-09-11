package com.murong.ecp.tools.fx.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Java类型枚举
 * 用于基础字典管理中的Java类型选择
 */
public enum JavaTypeEnum {

    STRING("String", "字符串类型"),
    INTEGER("Integer", "整数类型"),
    LONG("Long", "长整数类型"),
    BOOLEAN("Boolean", "布尔类型"),
    BYTE("Byte", "字节类型"),
    DATE("Date", "日期类型"),
    BIG_DECIMAL("BigDecimal", "高精度十进制类型"),
    LIST("List", "列表类型"),
    MAP("Map<String, Object>", "对象映射类型"),
    OBJECT("Object", "对象类型"),
    VOID("void", "无返回值类型");

    private final String typeName;
    private final String description;

    JavaTypeEnum(String typeName, String description) {
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
    public static JavaTypeEnum getByTypeName(String typeName) {
        for (JavaTypeEnum type : values()) {
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
                .map(JavaTypeEnum::getTypeName)
                .collect(Collectors.toList());
    }

    /**
     * 获取基本类型列表（不包含集合和复杂类型）
     * @return 基本类型名称列表
     */
    public static List<String> getBasicTypeNames() {
        return Arrays.stream(values())
                .filter(type -> !type.getTypeName().contains("<") && 
                               !type.getTypeName().equals("Object") && 
                               !type.getTypeName().equals("void"))
                .map(JavaTypeEnum::getTypeName)
                .collect(Collectors.toList());
    }

    /**
     * 获取集合类型列表
     * @return 集合类型名称列表
     */
    public static List<String> getCollectionTypeNames() {
        return Arrays.stream(values())
                .filter(type -> type.getTypeName().contains("<"))
                .map(JavaTypeEnum::getTypeName)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return typeName;
    }
}
