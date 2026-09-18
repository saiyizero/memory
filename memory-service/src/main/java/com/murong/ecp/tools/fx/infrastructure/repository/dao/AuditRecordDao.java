package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.domain.service.audit.AuditRecordWriter;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.AuditRecordQuery;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditRecordDao extends DaoSupport<AuditRecordPO> {

    @Autowired
    @Lazy
    private AuditRecordWriter auditRecordWriter;

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS audit_record (
                id varchar(64) NOT NULL,
                group_name varchar(64),
                project_name varchar(64),
                app_name varchar(32),
                biz_type varchar(32) NOT NULL,
                biz_key varchar(512) NOT NULL,
                biz_name varchar(256),
                oper_type varchar(16) NOT NULL,
                audit_status varchar(8) NOT NULL,
                old_data text,
                new_data text,
                submit_by varchar(64),
                submit_time varchar(64),
                audit_by varchar(64),
                audit_time varchar(64),
                audit_remark varchar(512),
                update_by varchar(64),
                update_time varchar(64),
                CONSTRAINT audit_record_pkey PRIMARY KEY (id)
            )
            """;

    private volatile boolean tableReady;

    public void ensureTable() {
        if (tableReady) {
            return;
        }
        synchronized (this) {
            if (tableReady) {
                return;
            }
            super.updateBySql(CREATE_TABLE_SQL);
            super.updateBySql("CREATE INDEX IF NOT EXISTS idx_audit_record_status ON audit_record (audit_status)");
            super.updateBySql("CREATE INDEX IF NOT EXISTS idx_audit_record_biz ON audit_record (biz_type, biz_key)");
            super.updateBySql("CREATE INDEX IF NOT EXISTS idx_audit_record_group ON audit_record (group_name, project_name, app_name)");
            super.updateBySql("CREATE INDEX IF NOT EXISTS idx_audit_record_submit_time ON audit_record (submit_time)");
            tableReady = true;
        }
    }

    public AuditRecordPO findPending(String bizType, String bizKey) {
        if (StringUtils.isBlank(bizType) || StringUtils.isBlank(bizKey)) {
            return null;
        }
        ensureTable();
        String sql = "select * from audit_record where biz_type = ? and biz_key = ? and audit_status = ? order by submit_time desc";
        List<AuditRecordPO> list = super.queryListBySql(sql, AuditRecordPO.class, bizType, bizKey, AuditStatusEnum.PENDING.getCode());
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public List<AuditRecordPO> queryByCondition(AuditRecordQuery query) {
        ensureTable();
        StringBuilder sql = new StringBuilder("select id, group_name, project_name, app_name, biz_type, biz_key, biz_name, ");
        sql.append("oper_type, audit_status, submit_by, submit_time, audit_by, audit_time, audit_remark, update_by, update_time ");
        sql.append("from audit_record where 1=1");
        List<Object> params = new ArrayList<>();
        appendEq(sql, params, "group_name", query == null ? null : query.getGroupName());
        appendEq(sql, params, "project_name", query == null ? null : query.getProjectName());
        appendEq(sql, params, "app_name", query == null ? null : query.getAppName());
        appendEq(sql, params, "biz_type", query == null ? null : query.getBizType());
        appendEq(sql, params, "oper_type", query == null ? null : query.getOperType());
        appendEq(sql, params, "audit_status", query == null ? null : query.getAuditStatus());
        if (query != null && StringUtils.isBlank(query.getAuditStatus()) && Boolean.TRUE.equals(query.getAuditedOnly())) {
            sql.append(" and audit_status in (?, ?)");
            params.add(AuditStatusEnum.APPROVED.getCode());
            params.add(AuditStatusEnum.REJECTED.getCode());
        }
        if (query != null && StringUtils.isNotBlank(query.getSubmitBy())) {
            sql.append(" and submit_by like ?");
            params.add("%" + query.getSubmitBy().trim() + "%");
        }
        if (query != null && StringUtils.isNotBlank(query.getBeginTime())) {
            sql.append(" and submit_time >= ?");
            params.add(query.getBeginTime());
        }
        if (query != null && StringUtils.isNotBlank(query.getEndTime())) {
            sql.append(" and submit_time <= ?");
            params.add(query.getEndTime());
        }
        if (query != null && StringUtils.isNotBlank(query.getKeyword())) {
            sql.append(" and (biz_name like ? or biz_key like ? or submit_by like ?)");
            String like = "%" + query.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" order by submit_time desc, id desc");
        if (params.isEmpty()) {
            return super.queryListBySql(sql.toString(), AuditRecordPO.class);
        }
        return super.queryListBySql(sql.toString(), AuditRecordPO.class, params.toArray());
    }

    public AuditRecordPO queryById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        ensureTable();
        AuditRecordPO where = new AuditRecordPO();
        where.setId(id);
        return super.queryOne(where);
    }

    public void save(AuditRecordPO po) {
        ensureTable();
        super.insert(po);
    }

    public void audit(String id, String status, String remark, String auditor) {
        ensureTable();
        AuditRecordPO existing = queryById(id);
        if (existing == null) {
            throw new RuntimeException("待审核记录不存在");
        }
        if (!AuditStatusEnum.PENDING.getCode().equals(existing.getAuditStatus())) {
            throw new RuntimeException("该记录已审核，不能重复处理");
        }
        if (AuditStatusEnum.APPROVED.getCode().equals(status)) {
            auditRecordWriter.applyApproved(existing);
        } else if (AuditStatusEnum.REJECTED.getCode().equals(status)) {
            auditRecordWriter.discard(existing);
        } else {
            throw new RuntimeException("不支持的审核状态: " + status);
        }
        String now = MrDateUtils.getCurrentTime();
        AuditRecordPO update = new AuditRecordPO();
        update.setAuditStatus(status);
        update.setAuditBy(auditor);
        update.setAuditTime(now);
        update.setAuditRemark(StringUtils.defaultString(remark));
        update.setUpdateBy(auditor);
        update.setUpdateTime(now);
        AuditRecordPO where = new AuditRecordPO();
        where.setId(id);
        super.updateByOne(update, where);
    }

    private void appendEq(StringBuilder sql, List<Object> params, String column, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        sql.append(" and ").append(column).append(" = ?");
        params.add(value);
    }
}
