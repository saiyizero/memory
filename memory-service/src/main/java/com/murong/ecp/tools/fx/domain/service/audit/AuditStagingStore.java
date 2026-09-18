package com.murong.ecp.tools.fx.domain.service.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditStagingStore {

    @Autowired
    @Qualifier("businessJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private volatile boolean tablesReady;

    public void ensureTables() {
        if (tablesReady) {
            return;
        }
        synchronized (this) {
            if (tablesReady) {
                return;
            }
            for (AuditBizTypeEnum type : AuditBizTypeEnum.values()) {
                String official = type.officialTable();
                String tmp = type.tmpTable();
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tmp + " (LIKE " + official + " INCLUDING DEFAULTS)");
                jdbcTemplate.execute("ALTER TABLE " + tmp + " ADD COLUMN IF NOT EXISTS audit_id varchar(64)");
                jdbcTemplate.execute("ALTER TABLE " + tmp + " ADD COLUMN IF NOT EXISTS oper_type varchar(16)");
                jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_" + tmp + "_audit_id ON " + tmp + " (audit_id)");
                jdbcTemplate.execute("ALTER TABLE " + tmp + " REPLICA IDENTITY FULL");
            }
            tablesReady = true;
        }
    }

    public void upsert(String auditId, AuditOperTypeEnum operType, Object entity) {
        if (StringUtils.isBlank(auditId) || entity == null) {
            return;
        }
        AuditBizTypeEnum type = AuditBizTypeEnum.fromEntity(entity);
        if (type == null) {
            return;
        }
        ensureTables();
        List<String> columns = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Field field : persistableFields(entity.getClass())) {
            Object value = readValue(entity, field);
            if (value == null) {
                continue;
            }
            columns.add(camelToSnake(field.getName()));
            params.add(unwrap(value));
        }
        columns.add("audit_id");
        params.add(auditId);
        columns.add("oper_type");
        params.add(operType.getCode());
        String placeholders = String.join(",", columns.stream().map(c -> "?").toList());
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(type.tmpTable()).append(" (").append(String.join(",", columns)).append(") VALUES (").append(placeholders).append(")");
        sql.append(" ON CONFLICT (audit_id) DO UPDATE SET ");
        List<String> sets = new ArrayList<>();
        for (String column : columns) {
            if (!"audit_id".equals(column)) {
                sets.add(column + " = EXCLUDED." + column);
            }
        }
        sql.append(String.join(", ", sets));
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void delete(String auditId) {
        if (StringUtils.isBlank(auditId)) {
            return;
        }
        ensureTables();
        for (AuditBizTypeEnum type : AuditBizTypeEnum.values()) {
            delete(auditId, type);
        }
    }

    public void delete(String auditId, AuditBizTypeEnum type) {
        if (StringUtils.isBlank(auditId) || type == null) {
            return;
        }
        ensureTables();
        jdbcTemplate.update("DELETE FROM " + type.tmpTable() + " WHERE audit_id = ?", auditId);
    }

    public <E> List<E> overlay(List<E> official, E example) {
        if (example == null) {
            return official;
        }
        @SuppressWarnings("unchecked")
        Class<E> clazz = (Class<E>) example.getClass();
        AuditBizTypeEnum type = AuditBizTypeEnum.fromClass(clazz);
        if (type == null) {
            return official;
        }
        return merge(official, queryTmp(type, clazz, example), type);
    }

    public <E> List<E> overlayBySql(List<E> official, String sql, Class<E> clazz, Object... params) {
        AuditBizTypeEnum type = AuditBizTypeEnum.fromClass(clazz);
        if (type == null || StringUtils.isBlank(sql)) {
            return official;
        }
        String tmpSql = replaceTable(sql, type.officialTable(), type.tmpTable());
        if (StringUtils.equals(tmpSql, sql)) {
            return overlayCurrentScope(official, clazz, type);
        }
        List<TmpRow<E>> tmpRows = queryTmpBySql(tmpSql, clazz, params);
        return merge(official, tmpRows, type);
    }

    public <E> List<E> overlayCurrentScope(List<E> official, Class<E> clazz) {
        AuditBizTypeEnum type = AuditBizTypeEnum.fromClass(clazz);
        if (type == null) {
            return official;
        }
        return overlayCurrentScope(official, clazz, type);
    }

    private <E> List<E> overlayCurrentScope(List<E> official, Class<E> clazz, AuditBizTypeEnum type) {
        try {
            E example = clazz.getDeclaredConstructor().newInstance();
            fillScope(example, type);
            return merge(official, queryTmp(type, clazz, example), type);
        } catch (Exception e) {
            return official;
        }
    }

    private <E> List<TmpRow<E>> queryTmp(AuditBizTypeEnum type, Class<E> clazz, E example) {
        ensureTables();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(type.tmpTable());
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        for (Field field : persistableFields(clazz)) {
            Object value = readValue(example, field);
            if (value == null) {
                continue;
            }
            if (where.isEmpty()) {
                where.append(" WHERE ");
            } else {
                where.append(" AND ");
            }
            where.append(camelToSnake(field.getName())).append(" = ?");
            params.add(unwrap(value));
        }
        sql.append(where);
        return queryTmpBySql(sql.toString(), clazz, params.toArray());
    }

    private <E> List<TmpRow<E>> queryTmpBySql(String sql, Class<E> clazz, Object... params) {
        ensureTables();
        BeanPropertyRowMapper<E> mapper = new BeanPropertyRowMapper<>(clazz);
        Object[] realParams = params == null ? new Object[0] : params;
        try {
            if (realParams.length == 0) {
                return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new TmpRow<>(mapper.mapRow(rs, rowNum), rs.getString("oper_type"), rs.getString("audit_id")));
            }
            return jdbcTemplate.query(sql, realParams, (rs, rowNum) ->
                    new TmpRow<>(mapper.mapRow(rs, rowNum), rs.getString("oper_type"), rs.getString("audit_id")));
        } catch (Exception e) {
            System.err.println("查询临时表失败: " + e.getMessage());
            return List.of();
        }
    }

    private <E> List<E> merge(List<E> official, List<TmpRow<E>> tmpRows, AuditBizTypeEnum type) {
        Map<String, E> result = new LinkedHashMap<>();
        if (official != null) {
            for (E item : official) {
                String key = type.resolveBizKey(item);
                if (StringUtils.isNotBlank(key)) {
                    result.put(key, item);
                }
            }
        }
        if (tmpRows != null) {
            for (TmpRow<E> row : tmpRows) {
                if (row == null || row.entity == null) {
                    continue;
                }
                String key = type.resolveBizKey(row.entity);
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                if (AuditOperTypeEnum.DELETE.getCode().equals(row.operType)) {
                    result.remove(key);
                } else {
                    result.put(key, row.entity);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    private void fillScope(Object example, AuditBizTypeEnum type) {
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        if (ctx == null || example == null) {
            return;
        }
        setIfPresent(example, "groupName", ctx.getGroupName());
        setIfPresent(example, "projectName", ctx.getProjectName());
        setIfPresent(example, "appName", ctx.getAppName());
    }

    private void setIfPresent(Object entity, String fieldName, String value) {
        if (entity == null || StringUtils.isBlank(value)) {
            return;
        }
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, value);
            }
        } catch (Exception ignored) {
        }
    }

    private List<Field> persistableFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            Class<?> type = field.getType();
            if (type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                    || type == Boolean.class || type.isEnum()) {
                fields.add(field);
            }
        }
        return fields;
    }

    private Object readValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private Object unwrap(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.toString();
        }
        return value;
    }

    private String replaceTable(String sql, String official, String tmp) {
        return sql.replaceAll("(?i)\\b" + official + "\\b", tmp);
    }

    private String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private record TmpRow<E>(E entity, String operType, String auditId) {
    }
}
