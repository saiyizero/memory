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
        if (realParams.length == 0) {
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz));
        }
        return jdbcTemplate.query(sql, realParams, new BeanPropertyRowMapper<>(clazz));
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
        auditInsert(entity);
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
        auditDelete(entity);
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
        return jdbcTemplate.query(sql.toString(), Arrays.copyOf(params, idx), new BeanPropertyRowMapper<>(clazz));
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
        auditUpdate(updateEntity, whereEntity);
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

    private void auditInsert(Object entity) {
        if (auditRecordWriter != null && auditRecordWriter.isAuditable(entity)) {
            auditRecordWriter.onInsert(entity);
        }
    }

    private void auditUpdate(Object updateEntity, Object whereEntity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(updateEntity)) {
            return;
        }
        Object oldEntity = null;
        try {
            oldEntity = queryOne(whereEntity);
        } catch (Exception ignored) {
        }
        auditRecordWriter.onUpdate(oldEntity, updateEntity);
    }

    private void auditDelete(Object entity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(entity)) {
            return;
        }
        List<?> oldList = List.of();
        try {
            oldList = queryForList(entity, null);
        } catch (Exception ignored) {
        }
        if (oldList == null || oldList.isEmpty()) {
            auditRecordWriter.onDelete(entity);
            return;
        }
        for (Object old : oldList) {
            auditRecordWriter.onDelete(old);
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
