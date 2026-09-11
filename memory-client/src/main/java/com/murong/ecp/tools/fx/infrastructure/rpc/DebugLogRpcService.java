package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.DebugLogQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 调试日志 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class DebugLogRpcService extends HttpDaoSupport<DebugLogPO> {
    public void save(DebugLogPO po) {
        super.insert(po);
    }

    public List<DebugLogPO> queryForList(DebugLogPO po) {
        return super.queryForList(po,"update_time desc");
    }

    public List<DebugLogGroupPO> qryGroupForList(DebugLogQuery query) {
        return invokeList("qryGroupForList", DebugLogGroupPO.class, query);
    }

    public List<String> queryIpListByGroup(DebugLogGroupPO group) {
        return invokeList("queryIpListByGroup", String.class, group);
    }
}
