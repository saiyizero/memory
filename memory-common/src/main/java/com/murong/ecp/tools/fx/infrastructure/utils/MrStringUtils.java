package com.murong.ecp.tools.fx.infrastructure.utils;


import com.murong.ecp.tools.fx.enums.UuidTypEnum;

/**
 * 字符串工具类，提供常用的字符串处理方法。
 */
public class MrStringUtils {
    
    /**
     * 将下划线命名的字符串转换为驼峰命名（首字母小写）。
     * 例如：hello_world -> helloWorld
     *
     * @param name 下划线命名的字符串
     * @return 驼峰命名的字符串
     */
    public static String toCamel(String name) {
        boolean isCamel = isCamel(name);
        if (isCamel) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }
        return sb.substring(0, 1).toLowerCase() + sb.substring(1);
    }

    /**
     * 将驼峰命名的字符串转换为下划线命名。
     * 例如：helloWorld -> hello_world
     *
     * @param name 驼峰命名的字符串
     * @return 下划线命名的字符串
     */
    public static String toUnderline(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        boolean isUnderline = isUnderline(name);
        if (isUnderline) {
            return name;
        }


        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 判断字符串是否为驼峰命名（首字母小写）。
     * <p>
     * 规则：
     * 1. 不能为 null 或空。
     * 2. 必须以小写字母开头。
     * 3. 不能包含下划线。
     * 4. 示例: "helloWorld", "name"
     *
     * @param name 待检查的字符串
     * @return 如果是驼峰命名则为 true，否则为 false
     */
    public static boolean isCamel(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // 正则：^[a-z]+([A-Z][a-z0-9]*)*$
        return name.matches("^[a-z]+([A-Z][a-z0-9]*)*$");
    }

    /**
     * 判断字符串是否为下划线命名。
     * <p>
     * 规则：
     * 1. 不能为 null 或空。
     * 2. 只能包含小写字母、数字和下划线。
     * 3. 不能以或以下划线结尾。
     * 4. 不能有两个连续的下划线。
     * 5. 示例: "hello_world", "name"
     *
     * @param name 待检查的字符串
     * @return 如果是下划线命名则为 true，否则为 false
     */
    public static boolean isUnderline(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // 正则：^[a-z0-9]+(_[a-z0-9]+)*$
        return name.matches("^[a-z0-9]+(_[a-z0-9]+)*$");
    }

    /**
     * 生成去除横线的UUID字符串。
     *
     * @param idTyp UUID类型枚举（当前未使用）
     * @return 32位无横线的UUID字符串
     */
    public static String generateId(UuidTypEnum idTyp) {
        return idTyp.getKey()+ MrDateUtils.getCurrentTimeLongStr()+generate6DigitNumber();
    }

    /**
     * 生成6位随机数
     */
    public static String generate6DigitNumber() {
        int num = (int)((Math.random() * 9 + 1) * 100000); // 保证首位不为0
        return String.valueOf(num);
    }

    /**
     * 将字符串首字母大写。
     * 例如：hello -> Hello
     *
     * @param str 原始字符串
     * @return 首字母大写后的字符串
     */
    public static String toFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    public static boolean isJavaBasicType(String type) {
        return type.equals("String") || type.equals("Integer") || type.equals("Long") || type.equals("Double") ||
                type.equals("Boolean") || type.equals("int") || type.equals("long") || type.equals("double") ||
                type.equals("boolean") || type.equals("BigDecimal") || type.equals("Date") ||
                type.equals("java.lang.String") || type.equals("java.lang.Integer") || type.equals("java.lang.Long") ||
                type.equals("java.lang.Double") || type.equals("java.lang.Boolean") || type.equals("java.math.BigDecimal") ||
                type.equals("java.util.Date") || type.equals("com.yuangou.ecp.bp.comp.pubatc.common.YGAmt") || type.equals("YGAmt");
    }
}
