package com.murong.ecp.tools.fx.infrastructure.utils;

import java.util.Map;

public final class AuditFieldLabels {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("groupName", "项目组"),
            Map.entry("projectName", "项目"),
            Map.entry("appName", "应用"),
            Map.entry("moduleName", "模块"),
            Map.entry("nameSnake", "下划线名"),
            Map.entry("nameCamel", "驼峰名"),
            Map.entry("type", "Java类型"),
            Map.entry("dbTyp", "数据库类型"),
            Map.entry("length", "长度"),
            Map.entry("defaultValue", "默认值"),
            Map.entry("commentCn", "中文注释"),
            Map.entry("commentEn", "英文注释"),
            Map.entry("enumNme", "枚举名称"),
            Map.entry("enumRef", "枚举路径"),
            Map.entry("enumCd", "枚举代码"),
            Map.entry("enumVal", "枚举值"),
            Map.entry("descCn", "中文描述"),
            Map.entry("descEn", "英文描述"),
            Map.entry("dbName", "数据库名称"),
            Map.entry("baseRef", "基础依赖"),
            Map.entry("notNull", "非空"),
            Map.entry("status", "状态"),
            Map.entry("msgClass", "信息码类"),
            Map.entry("msgRef", "信息码路径"),
            Map.entry("msgKey", "信息码键"),
            Map.entry("msgCd", "信息码"),
            Map.entry("msgDescCn", "信息码中文"),
            Map.entry("msgDescEn", "信息码英文"),
            Map.entry("className", "类名"),
            Map.entry("classType", "对象类型"),
            Map.entry("classPath", "类路径"),
            Map.entry("classCommentCn", "类中文注释"),
            Map.entry("classCommentEn", "类英文注释"),
            Map.entry("fieldsJson", "字段列表"),
            Map.entry("indexesJson", "索引"),
            Map.entry("completedFlg", "完成标志"),
            Map.entry("interfaceName", "接口名称"),
            Map.entry("transName", "交易名称"),
            Map.entry("transCommentZh", "交易中文注释"),
            Map.entry("transCommentEn", "交易英文注释"),
            Map.entry("transClass", "交易类"),
            Map.entry("simpleName", "简单名称"),
            Map.entry("requestJson", "请求报文"),
            Map.entry("responseJson", "响应报文"),
            Map.entry("requestPackage", "请求包名"),
            Map.entry("responsePackage", "响应包名"),
            Map.entry("requestClass", "请求类"),
            Map.entry("responseClass", "响应类"),
            Map.entry("interfaceUrl", "接口URL"),
            Map.entry("methodUrl", "方法URL"),
            Map.entry("lableName", "标签"),
            Map.entry("associatEntity", "关联实体"),
            Map.entry("associatEnum", "关联枚举"),
            Map.entry("reqParentClass", "请求父类"),
            Map.entry("rspParentClass", "响应父类"),
            Map.entry("tableNameCamel", "表驼峰名"),
            Map.entry("tableNameSnake", "表名"),
            Map.entry("tableCommentCn", "表中文注释"),
            Map.entry("tableCommentEn", "表英文注释"),
            Map.entry("primaryKeyJson", "主键"),
            Map.entry("parentClass", "公共字段"),
            Map.entry("generCdFlg", "生成代码"),
            Map.entry("createTabFlg", "自动建表"),
            Map.entry("defOrderBy", "默认排序"),
            Map.entry("updateBy", "更新人"),
            Map.entry("updateTime", "更新时间")
    );

    private AuditFieldLabels() {
    }

    public static String labelOf(String fieldPath) {
        if (fieldPath == null || fieldPath.isBlank()) {
            return "";
        }
        int dot = indexOfFirstSep(fieldPath);
        if (dot < 0) {
            return LABELS.getOrDefault(fieldPath, fieldPath);
        }
        String root = fieldPath.substring(0, dot);
        String rest = fieldPath.substring(dot);
        return LABELS.getOrDefault(root, root) + rest;
    }

    private static int indexOfFirstSep(String fieldPath) {
        int dot = fieldPath.indexOf('.');
        int bracket = fieldPath.indexOf('[');
        if (dot < 0) {
            return bracket;
        }
        if (bracket < 0) {
            return dot;
        }
        return Math.min(dot, bracket);
    }
}
