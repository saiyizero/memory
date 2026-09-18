package com.murong.ecp.tools.fx.domain.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.enums.UuidTypEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.GenericJdbcDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.AuditRecordDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext;
import com.murong.ecp.tools.fx.infrastructure.utils.AuditJsonDiffUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;

@Service
public class AuditRecordWriter {

    private static final ThreadLocal<Boolean> SKIP = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> OFFICIAL_ONLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Autowired
    @Lazy
    private AuditRecordDao auditRecordDao;

    @Autowired
    @Lazy
    private GenericJdbcDao genericJdbcDao;

    @Autowired
    private AuditStagingStore auditStagingStore;

    @Autowired
    private ObjectMapper objectMapper;

    public boolean isAuditable(Object entity) {
        return entity != null && AuditBizTypeEnum.fromEntity(entity) != null && !Boolean.TRUE.equals(SKIP.get());
    }

    public boolean shouldOverlay(Class<?> clazz) {
        return AuditBizTypeEnum.fromClass(clazz) != null
                && !Boolean.TRUE.equals(SKIP.get())
                && !Boolean.TRUE.equals(OFFICIAL_ONLY.get());
    }

    public <E> List<E> overlay(List<E> official, E example) {
        if (example == null || !shouldOverlay(example.getClass())) {
            return official;
        }
        return auditStagingStore.overlay(official, example);
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> overlay(List<E> official, Class<E> clazz) {
        if (clazz == null || !shouldOverlay(clazz)) {
            return official;
        }
        return auditStagingStore.overlayCurrentScope(official, clazz);
    }

    public <E> List<E> overlayBySql(List<E> official, String sql, Class<E> clazz, Object... params) {
        if (clazz == null || !shouldOverlay(clazz)) {
            return official;
        }
        return auditStagingStore.overlayBySql(official, sql, clazz, params);
    }

    public void beginOfficialOnly() {
        OFFICIAL_ONLY.set(Boolean.TRUE);
    }

    public void endOfficialOnly() {
        OFFICIAL_ONLY.set(Boolean.FALSE);
    }

    public boolean stageInsert(Object entity) {
        if (!isAuditable(entity)) {
            return false;
        }
        register(null, entity, false);
        return true;
    }

    public boolean stageUpdate(Object oldEntity, Object updateEntity) {
        if (!isAuditable(updateEntity) && !isAuditable(oldEntity)) {
            return false;
        }
        register(oldEntity, merge(oldEntity, updateEntity), false);
        return true;
    }

    public boolean stageDelete(Object entity) {
        if (!isAuditable(entity)) {
            return false;
        }
        register(entity, null, true);
        return true;
    }

    public void applyApproved(AuditRecordPO record) {
        if (record == null) {
            return;
        }
        AuditBizTypeEnum bizType = AuditBizTypeEnum.getByCode(record.getBizType());
        if (bizType == null) {
            throw new RuntimeException("未知的待审核类型: " + record.getBizType());
        }
        SKIP.set(Boolean.TRUE);
        OFFICIAL_ONLY.set(Boolean.TRUE);
        try {
            AuditOperTypeEnum operType = AuditOperTypeEnum.getByCode(record.getOperType());
            if (operType == AuditOperTypeEnum.DELETE) {
                Object oldEntity = fromJson(record.getOldData(), bizType.getEntityClass());
                if (oldEntity != null) {
                    genericJdbcDao.delete(oldEntity);
                }
            } else {
                Object newEntity = fromJson(record.getNewData(), bizType.getEntityClass());
                if (newEntity == null) {
                    throw new RuntimeException("待审核新数据为空，无法写入正式表");
                }
                Object oldEntity = fromJson(record.getOldData(), bizType.getEntityClass());
                Object where = oldEntity != null ? oldEntity : newEntity;
                Object existing = null;
                try {
                    existing = genericJdbcDao.queryOne(where);
                } catch (Exception ignored) {
                }
                if (existing == null && oldEntity != null) {
                    try {
                        existing = genericJdbcDao.queryOne(newEntity);
                    } catch (Exception ignored) {
                    }
                }
                if (existing == null) {
                    genericJdbcDao.insert(newEntity);
                } else {
                    genericJdbcDao.updateByOne(newEntity, existing);
                }
            }
            auditStagingStore.delete(record.getId(), bizType);
        } finally {
            SKIP.set(Boolean.FALSE);
            OFFICIAL_ONLY.set(Boolean.FALSE);
        }
    }

    public void discard(AuditRecordPO record) {
        if (record == null) {
            return;
        }
        AuditBizTypeEnum bizType = AuditBizTypeEnum.getByCode(record.getBizType());
        if (bizType != null) {
            auditStagingStore.delete(record.getId(), bizType);
        } else {
            auditStagingStore.delete(record.getId());
        }
    }

    private void register(Object oldEntity, Object newEntity, boolean delete) {
        Object snapshot = newEntity != null ? newEntity : oldEntity;
        AuditBizTypeEnum bizType = AuditBizTypeEnum.fromEntity(snapshot);
        if (bizType == null) {
            bizType = AuditBizTypeEnum.fromEntity(oldEntity);
        }
        if (bizType == null) {
            return;
        }
        try {
            SKIP.set(Boolean.TRUE);
            String oldJson = toJson(oldEntity);
            String newJson = delete ? "" : StringUtils.defaultString(toJson(newEntity));
            String bizKey = bizType.resolveBizKey(snapshot);
            if (StringUtils.isBlank(bizKey)) {
                return;
            }
            AuditRecordPO pending = auditRecordDao.findPending(bizType.getCode(), bizKey);
            String effectiveOld = pending == null ? StringUtils.defaultString(oldJson) : StringUtils.defaultString(pending.getOldData());
            if (AuditJsonDiffUtil.jsonEquals(effectiveOld, newJson)) {
                if (pending != null) {
                    AuditRecordPO where = new AuditRecordPO();
                    where.setId(pending.getId());
                    auditRecordDao.delete(where);
                    auditStagingStore.delete(pending.getId(), bizType);
                }
                return;
            }
            String now = MrDateUtils.getCurrentTime();
            String operator = currentUsername();
            AuditOperTypeEnum operType = resolveOperType(effectiveOld, newJson);
            Object tmpEntity = delete ? oldEntity : newEntity;
            if (pending == null) {
                AuditRecordPO po = new AuditRecordPO();
                po.setId(MrStringUtils.generateId(UuidTypEnum.AUDIT_REC));
                po.setGroupName(firstNonBlank(bizType.resolveGroupName(snapshot), currentGroupName()));
                po.setProjectName(firstNonBlank(bizType.resolveProjectName(snapshot), currentProjectName()));
                po.setAppName(firstNonBlank(bizType.resolveAppName(snapshot), currentAppName()));
                po.setBizType(bizType.getCode());
                po.setBizKey(bizKey);
                po.setBizName(bizType.resolveBizName(snapshot));
                po.setOperType(operType.getCode());
                po.setAuditStatus(AuditStatusEnum.PENDING.getCode());
                po.setOldData(effectiveOld);
                po.setNewData(newJson);
                po.setSubmitBy(operator);
                po.setSubmitTime(now);
                po.setUpdateBy(operator);
                po.setUpdateTime(now);
                auditRecordDao.save(po);
                if (tmpEntity != null) {
                    auditStagingStore.upsert(po.getId(), operType, tmpEntity);
                }
                return;
            }
            AuditRecordPO update = new AuditRecordPO();
            update.setBizName(bizType.resolveBizName(snapshot));
            update.setOperType(operType.getCode());
            update.setNewData(newJson);
            update.setSubmitBy(operator);
            update.setSubmitTime(now);
            update.setUpdateBy(operator);
            update.setUpdateTime(now);
            AuditRecordPO where = new AuditRecordPO();
            where.setId(pending.getId());
            auditRecordDao.updateByOne(update, where);
            if (tmpEntity != null) {
                auditStagingStore.upsert(pending.getId(), operType, tmpEntity);
            }
        } catch (Exception e) {
            System.err.println("登记待审核记录失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("提交待审核失败: " + e.getMessage(), e);
        } finally {
            SKIP.set(Boolean.FALSE);
        }
    }

    private Object merge(Object oldEntity, Object updateEntity) {
        if (oldEntity == null) {
            return updateEntity;
        }
        if (updateEntity == null) {
            return oldEntity;
        }
        try {
            Object merged = objectMapper.convertValue(oldEntity, oldEntity.getClass());
            for (Field field : updateEntity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(updateEntity);
                if (value != null) {
                    Field target = merged.getClass().getDeclaredField(field.getName());
                    target.setAccessible(true);
                    target.set(merged, value);
                }
            }
            return merged;
        } catch (Exception e) {
            return updateEntity;
        }
    }

    private Object fromJson(String json, Class<?> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("解析待审核数据失败: " + e.getMessage(), e);
        }
    }

    private String toJson(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            return String.valueOf(entity);
        }
    }

    private AuditOperTypeEnum resolveOperType(String oldJson, String newJson) {
        if (StringUtils.isBlank(oldJson)) {
            return AuditOperTypeEnum.ADD;
        }
        if (StringUtils.isBlank(newJson)) {
            return AuditOperTypeEnum.DELETE;
        }
        return AuditOperTypeEnum.UPDATE;
    }

    private String currentUsername() {
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        if (ctx != null && StringUtils.isNotBlank(ctx.getUsername())) {
            return ctx.getUsername();
        }
        return null;
    }

    private String currentGroupName() {
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        return ctx == null ? null : ctx.getGroupName();
    }

    private String currentProjectName() {
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        return ctx == null ? null : ctx.getProjectName();
    }

    private String currentAppName() {
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        return ctx == null ? null : ctx.getAppName();
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }
}
