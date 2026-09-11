package com.murong.ecp.tools.fx.infrastructure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonFormatUtil {

    public static String toHumpJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.readValue(json, Object.class);
        Object humpObj = toHumpObject(obj);
        return mapper.writeValueAsString(humpObj);
    }

    public static String formatJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.readValue(json, Object.class);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(obj);
    }

    // 递归处理Object/Map/List，将所有key下划线转驼峰
    private static Object toHumpObject(Object obj) {
        if (obj instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            java.util.Map<String, Object> newMap = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                String humpKey = toCamelCase(key);
                newMap.put(humpKey, toHumpObject(entry.getValue()));
            }
            return newMap;
        } else if (obj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) obj;
            java.util.List<Object> newList = new java.util.ArrayList<>();
            for (Object item : list) {
                newList.add(toHumpObject(item));
            }
            return newList;
        } else {
            return obj;
        }
    }

    // 下划线转驼峰
    private static String toCamelCase(String str) {
        if (str == null || !str.contains("_")) return str;
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upper = true;
            } else {
                if (upper) {
                    sb.append(Character.toUpperCase(c));
                    upper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    // 新增：对象转json字符串
    public static String toJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
} 