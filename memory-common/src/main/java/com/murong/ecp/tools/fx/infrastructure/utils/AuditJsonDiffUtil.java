package com.murong.ecp.tools.fx.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuditJsonDiffUtil {

    public enum ChangeType {
        ADDED, DELETED, MODIFIED, UNCHANGED
    }

    public static class DiffRow {
        private final String field;
        private final String fieldLabel;
        private final String oldValue;
        private final String newValue;
        private final ChangeType changeType;

        public DiffRow(String field, String fieldLabel, String oldValue, String newValue, ChangeType changeType) {
            this.field = field;
            this.fieldLabel = fieldLabel;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changeType = changeType;
        }

        public String getField() {
            return field;
        }

        public String getFieldLabel() {
            return fieldLabel;
        }

        public String getOldValue() {
            return oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public ChangeType getChangeType() {
            return changeType;
        }

        public String getChangeTypeDesc() {
            return switch (changeType) {
                case ADDED -> "新增";
                case DELETED -> "删除";
                case MODIFIED -> "修改";
                case UNCHANGED -> "未变";
            };
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private AuditJsonDiffUtil() {
    }

    public static List<DiffRow> diff(String oldJson, String newJson) {
        Map<String, String> oldMap = flatten(oldJson);
        Map<String, String> newMap = flatten(newJson);
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(oldMap.keySet());
        keys.addAll(newMap.keySet());
        List<DiffRow> rows = new ArrayList<>();
        for (String key : keys) {
            String oldValue = oldMap.get(key);
            String newValue = newMap.get(key);
            ChangeType changeType;
            if (oldValue == null && newValue != null) {
                changeType = ChangeType.ADDED;
            } else if (oldValue != null && newValue == null) {
                changeType = ChangeType.DELETED;
            } else if (StringUtils.equals(oldValue, newValue)) {
                changeType = ChangeType.UNCHANGED;
            } else {
                changeType = ChangeType.MODIFIED;
            }
            rows.add(new DiffRow(key, AuditFieldLabels.labelOf(key),
                    oldValue == null ? "" : oldValue,
                    newValue == null ? "" : newValue,
                    changeType));
        }
        return rows;
    }

    public static Map<String, String> flatten(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StringUtils.isBlank(json)) {
            return result;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            flattenNode("", node, result);
        } catch (Exception e) {
            result.put("raw", json);
        }
        return result;
    }

    public static String pretty(String json) {
        if (StringUtils.isBlank(json)) {
            return "";
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return json;
        }
    }

    public static boolean jsonEquals(String left, String right) {
        if (StringUtils.isBlank(left) && StringUtils.isBlank(right)) {
            return true;
        }
        if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
            return false;
        }
        try {
            return MAPPER.readTree(left).equals(MAPPER.readTree(right));
        } catch (Exception e) {
            return StringUtils.equals(left, right);
        }
    }

    private static void flattenNode(String prefix, JsonNode node, Map<String, String> result) {
        if (node == null || node.isNull()) {
            put(result, prefix, "");
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            boolean empty = true;
            while (fields.hasNext()) {
                empty = false;
                Map.Entry<String, JsonNode> entry = fields.next();
                String nextPrefix = StringUtils.isBlank(prefix) ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(nextPrefix, entry.getValue(), result);
            }
            if (empty && StringUtils.isNotBlank(prefix)) {
                put(result, prefix, "{}");
            }
            return;
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                put(result, prefix, "[]");
                return;
            }
            for (int i = 0; i < node.size(); i++) {
                flattenNode(prefix + "[" + i + "]", node.get(i), result);
            }
            return;
        }
        if (node.isTextual()) {
            String text = node.asText();
            JsonNode nested = tryParseJson(text);
            if (nested != null && (nested.isObject() || nested.isArray())) {
                flattenNode(prefix, nested, result);
                return;
            }
            put(result, prefix, prettyIfJson(text));
            return;
        }
        put(result, prefix, node.asText());
    }

    private static JsonNode tryParseJson(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return null;
        }
        try {
            return MAPPER.readTree(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    private static String prettyIfJson(String text) {
        JsonNode nested = tryParseJson(text);
        if (nested == null) {
            return text;
        }
        try {
            return MAPPER.writeValueAsString(nested);
        } catch (Exception e) {
            return text;
        }
    }

    private static void put(Map<String, String> result, String key, String value) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        result.put(key, value == null ? "" : value);
    }
}
