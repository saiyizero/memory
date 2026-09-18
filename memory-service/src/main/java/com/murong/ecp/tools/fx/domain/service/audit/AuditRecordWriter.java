package com.murong.ecp.tools.fx.domain.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.enums.UuidTypEnum;
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

@Service
public class AuditRecordWriter {

    private static final ThreadLocal<Boolean> SKIP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Autowired
    @Lazy
    private AuditRecordDao auditRecordDao;

    @Autowired
    private ObjectMapper objectMapper;

    public void onInsert(Object entity) {
        register(null, entity, false);
    }

    public void onUpdate(Object oldEntity, Object updateEntity) {
        register(oldEntity, merge(oldEntity, updateEntity), false);
    }

    public void onDelete(Object entity) {
        register(entity, null, true);
    }

    public boolean isAuditable(Object entity) {
        return entity != null && AuditBizTypeEnum.fromEntity(entity) != null && !Boolean.TRUE.equals(SKIP.get());
    }

    private void register(Object oldEntity, Object newEntity, boolean delete) {
        Object snapshot = newEntity != null ? newEntity : oldEntity;
        if (!isAuditable(snapshot) && !isAuditable(oldEntity)) {
            return;
        }
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
                }
                return;
            }
            String now = MrDateUtils.getCurrentTime();
            String operator = currentUsername();
            AuditOperTypeEnum operType = resolveOperType(effectiveOld, newJson);
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
        } catch (Exception e) {
            System.err.println("登记待审核记录失败: " + e.getMessage());
            e.printStackTrace();
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
