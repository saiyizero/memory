package com.murong.ecp.tools.fx.domain.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
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

    /**
     * tmp 表保存的是正式表修改前的快照，查询正式表时不能再叠加 tmp。
     */
    public boolean shouldOverlay(Class<?> clazz) {
        return false;
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
        markDataStatus(entity, DataStatusEnum.WAIT_AUDIT);
        register(null, entity, false);
        return false;
    }

    public boolean stageUpdate(Object oldEntity, Object updateEntity) {
        if (!isAuditable(updateEntity) && !isAuditable(oldEntity)) {
            return false;
        }
        Object merged = merge(oldEntity, updateEntity);
        markDataStatus(merged, DataStatusEnum.WAIT_AUDIT);
        markDataStatus(updateEntity, DataStatusEnum.WAIT_AUDIT);
        register(oldEntity, merged, false);
        return false;
    }

    public boolean stageDelete(Object entity) {
        if (!isAuditable(entity)) {
            return false;
        }
        register(entity, null, true);
        return false;
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
                Object oldEntity = firstNonNull(
                        auditStagingStore.load(record.getId(), bizType),
                        fromJson(record.getOldData(), bizType.getEntityClass()));
                if (oldEntity != null) {
                    genericJdbcDao.delete(bizType.newPkWhere(oldEntity));
                }
            } else {
                Object newEntity = fromJson(record.getNewData(), bizType.getEntityClass());
                if (newEntity == null) {
                    throw new RuntimeException("待审核新数据为空，无法写入正式表");
                }
                markDataStatus(newEntity, DataStatusEnum.NORMAL);
                upsertOfficial(newEntity, bizType);
            }
            deleteTmpQuietly(record.getId(), bizType);
        } finally {
            SKIP.set(Boolean.FALSE);
            OFFICIAL_ONLY.set(Boolean.FALSE);
        }
    }

    public void discard(AuditRecordPO record) {
        restoreRejected(record);
    }

    public void restoreRejected(AuditRecordPO record) {
        if (record == null) {
            return;
        }
        AuditBizTypeEnum bizType = AuditBizTypeEnum.getByCode(record.getBizType());
        if (bizType == null) {
            auditStagingStore.delete(record.getId());
            return;
        }
        SKIP.set(Boolean.TRUE);
        OFFICIAL_ONLY.set(Boolean.TRUE);
        try {
            AuditOperTypeEnum operType = AuditOperTypeEnum.getByCode(record.getOperType());
            Object backup = firstNonNull(
                    auditStagingStore.load(record.getId(), bizType),
                    fromJson(record.getOldData(), bizType.getEntityClass()));
            if (operType == AuditOperTypeEnum.ADD) {
                Object added = firstNonNull(backup, fromJson(record.getNewData(), bizType.getEntityClass()));
                if (added != null) {
                    genericJdbcDao.delete(bizType.newPkWhere(added));
                }
            } else if (backup != null) {
                markDataStatus(backup, DataStatusEnum.NORMAL);
                upsertOfficial(backup, bizType);
            }
            deleteTmpQuietly(record.getId(), bizType);
        } finally {
            SKIP.set(Boolean.FALSE);
            OFFICIAL_ONLY.set(Boolean.FALSE);
        }
    }

    private void upsertOfficial(Object entity, AuditBizTypeEnum bizType) {
        Object pk = bizType.newPkWhere(entity);
        Object existing = null;
        try {
            existing = genericJdbcDao.queryOne(pk);
        } catch (Exception ignored) {
        }
        if (existing == null) {
            genericJdbcDao.insert(entity);
        } else {
            genericJdbcDao.updateByOne(entity, pk);
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
            Object tmpEntity = oldEntity != null ? oldEntity : newEntity;
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
            if (tmpEntity != null && auditStagingStore.load(pending.getId(), bizType) == null) {
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

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private void deleteTmpQuietly(String auditId, AuditBizTypeEnum bizType) {
        try {
            auditStagingStore.delete(auditId, bizType);
        } catch (Exception e) {
            System.err.println("清理临时表失败: " + e.getMessage());
        }
    }

    private void markDataStatus(Object entity, DataStatusEnum status) {
        if (entity == null || status == null) {
            return;
        }
        try {
            Field field = entity.getClass().getDeclaredField("status");
            field.setAccessible(true);
            field.set(entity, status.getCode());
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("更新数据状态失败: " + e.getMessage(), e);
        }
    }
}
