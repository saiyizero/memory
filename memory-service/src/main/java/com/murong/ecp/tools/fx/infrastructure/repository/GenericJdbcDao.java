package com.murong.ecp.tools.fx.infrastructure.repository;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.audit.AuditRecordWriter;
import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 面向 HTTP 的通用 JDBC 访问，不依赖具体 DAO 子类。
 */
@Service
public class GenericJdbcDao {

    @Autowired
    @Qualifier("businessJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private AuditRecordWriter auditRecordWriter;

    public int updateBySql(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return jdbcTemplate.update(sql);
        }
        return jdbcTemplate.update(sql, params);
    }

    public <E> E queryOneBySql(String sql, Class<E> clazz, Object... params) {
        List<E> list = queryListBySql(sql, clazz, params);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new RuntimeException("查询结果不唯一，返回了多条数据");
        }
        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> queryListBySql(String sql, Class<E> clazz, Object... params) {
        Object[] realParams = params == null ? new Object[0] : params;
        if (clazz == String.class || clazz == Integer.class || clazz == Long.class) {
            return jdbcTemplate.queryForList(sql, clazz, realParams);
        }
        List<E> official;
        if (realParams.length == 0) {
            official = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz));
        } else {
            official = jdbcTemplate.query(sql, realParams, new BeanPropertyRowMapper<>(clazz));
        }
        return overlayBySql(official, sql, clazz, realParams);
    }

    public Integer queryCount(String sql, Object... params) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> queryForMaps(String sql, Object... params) {
        if (params == null || params.length == 0) {
            return jdbcTemplate.queryForList(sql);
        }
        return jdbcTemplate.queryForList(sql, params);
    }

    public void insert(Object entity) {
        fillAuditDefaults(entity);
        if (stageInsert(entity)) {
            return;
        }
        GlobalProperties globalPropts = MrSpringContextHolder.getBean(GlobalProperties.class);
        Class<?> clazz = entity.getClass();
        JTable table = clazz.getAnnotation(JTable.class);
        if (table == null) throw new RuntimeException("缺少JTable注解");
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Object[] params = new Object[fields.length];
        int idx = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (StringUtils.equals(field.getName(), "appName") && StringUtils.isBlank((String) value) && globalPropts != null) {
                    value = globalPropts.getAppName();
                }
                if (StringUtils.equals(field.getName(), "updateBy") && StringUtils.isBlank((String) value)
                        && globalPropts != null && globalPropts.getOperator() != null) {
                    value = globalPropts.getOperator().getUsername();
                }
                if (StringUtils.equals(field.getName(), "updateTime") && StringUtils.isBlank((String) value)) {
                    value = MrDateUtils.getCurrentTime();
                }
                if (value != null) {
                    if (columns.length() > 0) {
                        columns.append(",");
                        values.append(",");
                    }
                    columns.append(camelToSnake(field.getName()));
                    values.append("?");
                    params[idx++] = unwrapEnum(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table.name(), columns, values);
        jdbcTemplate.update(sql, Arrays.copyOf(params, idx));
    }

    public void delete(Object entity) {
        if (stageDelete(entity)) {
            return;
        }
        Class<?> clazz = entity.getClass();
        JTable table = clazz.getAnnotation(JTable.class);
        if (table == null) throw new RuntimeException("缺少JTable注解");
        StringBuilder sql = new StringBuilder("DELETE FROM " + table.name());
        StringBuilder where = new StringBuilder();
        Object[] params = new Object[clazz.getDeclaredFields().length];
        int idx = 0;
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    if (where.length() == 0) {
                        where.append(" WHERE ");
                    } else {
                        where.append(" AND ");
                    }
                    where.append(camelToSnake(field.getName())).append(" = ?");
                    params[idx++] = unwrapEnum(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        sql.append(where);
        jdbcTemplate.update(sql.toString(), Arrays.copyOf(params, idx));
    }

    public List<?> queryForList(Object entity, String orderBy) {
        Class<?> clazz = entity.getClass();
        JTable table = clazz.getAnnotation(JTable.class);
        if (table == null) throw new RuntimeException("缺少JTable注解");
        StringBuilder sql = new StringBuilder("SELECT * FROM " + table.name());
        StringBuilder where = new StringBuilder();
        Object[] params = new Object[clazz.getDeclaredFields().length];
        int idx = 0;
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    if (where.length() == 0) {
                        where.append(" WHERE ");
                    } else {
                        where.append(" AND ");
                    }
                    where.append(camelToSnake(field.getName())).append(" = ?");
                    params[idx++] = unwrapEnum(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        sql.append(where);
        if (StringUtils.isNotBlank(orderBy)) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        List<?> official = jdbcTemplate.query(sql.toString(), Arrays.copyOf(params, idx), new BeanPropertyRowMapper<>(clazz));
        return overlayList(official, entity);
    }

    public Object queryOne(Object entity) {
        List<?> list = queryForList(entity, null);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new RuntimeException("查询结果不唯一，返回了多条数据");
        }
        return list.get(0);
    }

    public void updateByOne(Object updateEntity, Object whereEntity) {
        fillAuditDefaults(updateEntity);
        if (stageUpdate(updateEntity, whereEntity)) {
            return;
        }
        Class<?> clazz = updateEntity.getClass();
        JTable table = clazz.getAnnotation(JTable.class);
        if (table == null) throw new RuntimeException("缺少JTable注解");
        Field[] fields = clazz.getDeclaredFields();

        StringBuilder setClause = new StringBuilder();
        Object[] setParams = new Object[fields.length];
        int setIdx = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(updateEntity);
                if (value != null) {
                    if (setClause.length() > 0) {
                        setClause.append(", ");
                    }
                    setClause.append(camelToSnake(field.getName())).append(" = ?");
                    setParams[setIdx++] = unwrapEnum(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (setIdx == 0) {
            throw new RuntimeException("没有需要更新的字段");
        }

        StringBuilder whereClause = new StringBuilder();
        Object[] whereParams = new Object[fields.length];
        int whereIdx = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(whereEntity);
                if (value != null) {
                    if (whereClause.length() > 0) {
                        whereClause.append(" AND ");
                    }
                    whereClause.append(camelToSnake(field.getName())).append(" = ?");
                    whereParams[whereIdx++] = unwrapEnum(value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (whereIdx == 0) {
            throw new RuntimeException("没有筛选条件");
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s", table.name(), setClause, whereClause);
        Object[] realParams = new Object[setIdx + whereIdx];
        System.arraycopy(setParams, 0, realParams, 0, setIdx);
        System.arraycopy(whereParams, 0, realParams, setIdx, whereIdx);
        jdbcTemplate.update(sql, realParams);
    }

    private boolean stageInsert(Object entity) {
        return auditRecordWriter != null && auditRecordWriter.stageInsert(entity);
    }

    private boolean stageUpdate(Object updateEntity, Object whereEntity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(updateEntity)) {
            return false;
        }
        Object oldEntity = null;
        auditRecordWriter.beginOfficialOnly();
        try {
            oldEntity = queryOne(whereEntity);
        } catch (Exception ignored) {
        } finally {
            auditRecordWriter.endOfficialOnly();
        }
        return auditRecordWriter.stageUpdate(oldEntity, updateEntity);
    }

    private boolean stageDelete(Object entity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(entity)) {
            return false;
        }
        List<?> oldList = List.of();
        auditRecordWriter.beginOfficialOnly();
        try {
            oldList = queryForList(entity, null);
        } catch (Exception ignored) {
        } finally {
            auditRecordWriter.endOfficialOnly();
        }
        if (oldList == null || oldList.isEmpty()) {
            auditRecordWriter.stageDelete(entity);
            return false;
        }
        for (Object old : oldList) {
            auditRecordWriter.stageDelete(old);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<?> overlayList(List<?> official, Object example) {
        if (auditRecordWriter == null || example == null) {
            return official;
        }
        return auditRecordWriter.overlay((List<Object>) official, example);
    }

    private <E> List<E> overlayBySql(List<E> official, String sql, Class<E> clazz, Object... params) {
        if (auditRecordWriter == null) {
            return official;
        }
        return auditRecordWriter.overlayBySql(official, sql, clazz, params);
    }

    private void fillAuditDefaults(Object entity) {
        if (entity == null) {
            return;
        }
        GlobalProperties globalPropts = MrSpringContextHolder.getBean(GlobalProperties.class);
        if (globalPropts == null) {
            return;
        }
        setIfBlank(entity, "appName", globalPropts.getAppName());
        if (globalPropts.getOperator() != null) {
            setIfBlank(entity, "updateBy", globalPropts.getOperator().getUsername());
        }
        setIfBlank(entity, "updateTime", MrDateUtils.getCurrentTime());
    }

    private void setIfBlank(Object entity, String fieldName, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object current = field.get(entity);
            if (current == null || (current instanceof String str && StringUtils.isBlank(str))) {
                field.set(entity, value);
            }
        } catch (Exception ignored) {
        }
    }

    private Object unwrapEnum(Object value) {
        if (value instanceof Enum) {
            if (value instanceof DiffTypeEnum) {
                return ((DiffTypeEnum) value).getCode();
            }
            return value.toString();
        }
        return value;
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
}
