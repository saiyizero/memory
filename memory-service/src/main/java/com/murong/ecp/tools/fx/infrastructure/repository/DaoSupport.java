package com.murong.ecp.tools.fx.infrastructure.repository;


import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.audit.AuditRecordWriter;
import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import com.murong.ecp.tools.fx.infrastructure.utils.JsonFormatUtil;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

@Service
public class DaoSupport <T> {
    @Autowired
    @Qualifier("businessJdbcTemplate")
    @Lazy
    public JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private AuditRecordWriter auditRecordWriter;

    public int updateBySql(String sql){
        logWithCaller("[SQL-UPDATE] " + sql);
        return jdbcTemplate.update(sql);
    }

    public int updateBySql(String sql, Object... params){
        logWithCaller("[SQL-UPDATE] " + sql + " | params=" + Arrays.toString(params));
        return jdbcTemplate.update(sql, params);
    }

    public T queryOneBySql(String sql){
        return queryOneBySql(sql, getGenericType());
    }

    /**
     * 获取泛型类型
     */
    @SuppressWarnings("unchecked")
    private Class<T> getGenericType() {
        try {
            Type genericSuperclass = this.getClass().getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                return (Class<T>) parameterizedType.getActualTypeArguments()[0];
            }
            throw new RuntimeException("无法获取泛型类型");
        } catch (Exception e) {
            throw new RuntimeException("获取泛型类型失败: " + e.getMessage());
        }
    }

    public void delete(T entity) {
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
                    
                    // 处理枚举类型，存储为字符串
                    if (value instanceof Enum) {
                        if (value instanceof DiffTypeEnum) {
                            params[idx++] = ((DiffTypeEnum) value).getCode();
                        } else {
                            params[idx++] = value.toString();
                        }
                    } else {
                        params[idx++] = value;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        sql.append(where);
        logWithCaller("[SQL-DELETE] " + sql + " | params=" + java.util.Arrays.toString(Arrays.copyOf(params, idx)));
        jdbcTemplate.update(sql.toString(), Arrays.copyOf(params, idx));
    }

    public <E> E queryOneBySql(String sql, Class<E> clazz) {
        List<E> list = queryListBySql(sql, clazz);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new RuntimeException("查询结果不唯一，返回了多条数据");
        }
        return list.get(0);
    }


    public <E> List<E> queryListBySql(String sql, Class<E> clazz) {
        logWithCaller("[SQL-QUERY] " + sql);
        if (clazz == String.class) {
            return jdbcTemplate.queryForList(sql, clazz);
        }
        List<E> official = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz));
        return overlayBySql(official, sql, clazz);
    }

    public T queryOne(T entity) {
        List<T> list = queryForList(entity);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            logWithCaller(JsonFormatUtil.toJson(entity));
            throw new RuntimeException("查询结果不唯一，返回了多条数据");
        }
        return list.get(0);
    }

    public List<T> queryForList(T entity,String orderBy) {
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
                    params[idx++] = value;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        sql.append(where);
        if(StringUtils.isNotBlank(orderBy)) {
            sql.append(" ORDER BY " + orderBy);
        }
        logWithCaller("[SQL-QUERY] " + sql + " | params=" + java.util.Arrays.toString(Arrays.copyOf(params, idx)));
        List<T> official = (List<T>) jdbcTemplate.query(sql.toString(), Arrays.copyOf(params, idx), new BeanPropertyRowMapper<>(clazz));
        return overlayList(official, entity);
    }

    public List<T> queryForList(T entity) {
        return queryForList(entity,null);
    }

    public void insert(T entity) {
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

                if(StringUtils.equals(field.getName(), "appName") && StringUtils.isBlank((String)value)) {
                    value=globalPropts.getAppName();
                }
                if(StringUtils.equals(field.getName(), "updateBy") && StringUtils.isBlank((String)value)) {
                    value=globalPropts.getOperator().getUsername();
                }
                if(StringUtils.equals(field.getName(), "updateTime") && StringUtils.isBlank((String)value)) {
                    value= MrDateUtils.getCurrentTime();
                }

                if (value != null) {
                    if (columns.length() > 0) {
                        columns.append(",");
                        values.append(",");
                    }
                    columns.append(camelToSnake(field.getName()));
                    values.append("?");
                    
                    // 处理枚举类型，存储为字符串
                    if (value instanceof Enum) {
                        if (value instanceof DiffTypeEnum) {
                            params[idx++] = ((DiffTypeEnum) value).getCode();
                        } else {
                            params[idx++] = value.toString();
                        }
                    } else {
                        params[idx++] = value;
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table.name(), columns, values);
        Object[] realParams = new Object[idx];
        System.arraycopy(params, 0, realParams, 0, idx);
        logWithCaller("[SQL-INSERT] " + sql + " | params=" + java.util.Arrays.toString(realParams));
        jdbcTemplate.update(sql, realParams);
    }

    public void updateByOne(T updateEntity, T whereEntity) {
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
                    
                    // 处理枚举类型，存储为字符串
                    if (value instanceof Enum) {
                        if (value instanceof DiffTypeEnum) {
                            setParams[setIdx++] = ((DiffTypeEnum) value).getCode();
                        } else {
                            setParams[setIdx++] = value.toString();
                        }
                    } else {
                        setParams[setIdx++] = value;
                    }
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
                    whereParams[whereIdx++] = value;
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
        logWithCaller("[SQL-UPDATE] " + sql + " | params=" + java.util.Arrays.toString(realParams));
        jdbcTemplate.update(sql, realParams);
    }

    public <E> List<E> queryListBySql(String sql, Class<E> clazz, Object... params) {
        logWithCaller("[SQL-QUERY] " + sql + " | params=" + Arrays.toString(params));
        if (clazz == String.class || clazz == Integer.class || clazz == Long.class) {
            return jdbcTemplate.queryForList(sql, clazz, params);
        }
        List<E> official = jdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(clazz));
        return overlayBySql(official, sql, clazz, params);
    }

    public Integer queryCount(String sql, Object... params) {
        logWithCaller("[SQL-COUNT] " + sql + " | params=" + Arrays.toString(params));
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count == null ? 0 : count;
    }

    public java.util.List<java.util.Map<String, Object>> queryForMaps(String sql, Object... params) {
        logWithCaller("[SQL-QUERY-MAP] " + sql + " | params=" + Arrays.toString(params));
        if (params == null || params.length == 0) {
            return jdbcTemplate.queryForList(sql);
        }
        return jdbcTemplate.queryForList(sql, params);
    }

    private boolean stageInsert(T entity) {
        return auditRecordWriter != null && auditRecordWriter.stageInsert(entity);
    }

    private boolean stageUpdate(T updateEntity, T whereEntity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(updateEntity)) {
            return false;
        }
        T oldEntity = null;
        auditRecordWriter.beginOfficialOnly();
        try {
            oldEntity = queryOne(whereEntity);
        } catch (Exception ignored) {
        } finally {
            auditRecordWriter.endOfficialOnly();
        }
        return auditRecordWriter.stageUpdate(oldEntity, updateEntity);
    }

    private boolean stageDelete(T entity) {
        if (auditRecordWriter == null || !auditRecordWriter.isAuditable(entity)) {
            return false;
        }
        List<T> oldList = List.of();
        auditRecordWriter.beginOfficialOnly();
        try {
            oldList = queryForList(entity);
        } catch (Exception ignored) {
        } finally {
            auditRecordWriter.endOfficialOnly();
        }
        if (oldList == null || oldList.isEmpty()) {
            return auditRecordWriter.stageDelete(entity);
        }
        for (T old : oldList) {
            auditRecordWriter.stageDelete(old);
        }
        return true;
    }

    private List<T> overlayList(List<T> official, T example) {
        if (auditRecordWriter == null) {
            return official;
        }
        return auditRecordWriter.overlay(official, example);
    }

    private <E> List<E> overlayBySql(List<E> official, String sql, Class<E> clazz, Object... params) {
        if (auditRecordWriter == null) {
            return official;
        }
        return auditRecordWriter.overlayBySql(official, sql, clazz, params);
    }

    private void fillAuditDefaults(T entity) {
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

    private void setIfBlank(T entity, String fieldName, String value) {
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

    private void logWithCaller(String msg) {
        String loglevel = MrSpringContextHolder.getProperty("logging.level.daosupport");
        if(StringUtils.isNotBlank(loglevel) && !loglevel.equals("DEBUG")) {
            return;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String callerInfo = "";
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            // 跳过DaoSupport、Dao、JDK、Spring、反射、代理等
            if (
                !className.equals(this.getClass().getName())
                && !className.startsWith("java.")
                && !className.startsWith("sun.")
                && !className.startsWith("jdk.")
                && !className.startsWith("org.springframework.")
                && !className.contains("cglib")
                && !className.contains("CGLIB")
                && !className.contains("reflect")
                && !className.endsWith("DaoSupport")
                && !className.endsWith("Dao")
                && !className.contains("$$")
                && (element.getFileName() == null || !element.getFileName().equals("<generated>"))
            ) {
                String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                callerInfo = simpleClassName + "." + element.getMethodName()
                        + "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
                break;
            }
        }
        System.out.println(callerInfo+" | "+msg);
    }
}
