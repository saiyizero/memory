package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.AuditRecordQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditRecordRpcService extends HttpDaoSupport<AuditRecordPO> {

    public List<AuditRecordPO> queryByCondition(AuditRecordQuery query) {
        return invokeList("queryByCondition", AuditRecordPO.class, query);
    }

    public AuditRecordPO queryById(String id) {
        return invoke("queryById", AuditRecordPO.class, id);
    }

    public void audit(String id, String status, String remark, String auditor) {
        invokeVoid("audit", id, status, remark, auditor);
    }
}
