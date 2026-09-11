package com.murong.ecp.tools.fx.infrastructure.utils;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;

import java.io.File;

public class BusinessUtils {

    public static String getClassPath(String sourcePath){
        String classRoot;
        if(sourcePath.endsWith("/src/main/java")) {
            classRoot= sourcePath.replace("/src/main/java","/target/classes");
        }else {
            classRoot= sourcePath + "/target/classes";
        }
        return classRoot;
    }

    /**
     * 从完整路径中提取模块名称
     * @param fullBasePath 完整路径，如：saving-web/src/main/java
     * @return 模块名称，如：saving-web
     */
    public static String extractModuleName(String fullBasePath) {
        if (fullBasePath == null || fullBasePath.trim().isEmpty()) {
            return "";
        }

        // 按路径分隔符分割
        String[] pathParts = fullBasePath.split(File.separator);

        // 返回第一个部分作为模块名称
        if (pathParts.length > 0) {
            return pathParts[0];
        }

        return fullBasePath;
    }

    /**
     * 根据接口名称获取标签
     */
    public static String getLabelByInterfaceName(String interfaceName) {
        GlobalProperties globalPropes = MrSpringContextHolder.getBean(GlobalProperties.class);
        String simpleName = globalPropes.getAppName();
        if (interfaceName == null || interfaceName.isEmpty() || simpleName == null || simpleName.isEmpty()) {
            return null;
        }
        // 统一大小写
        String prefix = simpleName.toLowerCase();
        String name = interfaceName;
        // 去掉前缀（忽略大小写）
        if (name.toLowerCase().startsWith(prefix)) {
            name = name.substring(prefix.length());
        }
        // 去掉Service后缀（忽略大小写）
        if (name.toLowerCase().endsWith("service")) {
            name = name.substring(0, name.length() - 7);
        }
        // 首字母小写
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        return name;
    }

    /**
     * 判断这句话是否为纯中文
     */
    public static boolean isAllChinese(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (!isChinese(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断这句话是否为中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /**
     * 判断这句话是否为纯英文
     */
    public static boolean isAllEnglish(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z') && c != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * 枚举类名转DB字段名（如AplCnlEnum->APL_CNL, IDTypDict->ID_TYP）
     */
    public static String enumNameToDbName(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        // 去除结尾的Enum/Dict等后缀
        String name = enumName.replaceAll("(Enum|Dict)$", "");
        // 驼峰转下划线
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

}
