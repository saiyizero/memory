package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务消息 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class BizMsgInfoRpcService extends HttpDaoSupport<BizMsgInfoPO> {

    public List<BizMsgInfoPO> queryForSearch(String text) {
        return invokeList("queryForSearch", BizMsgInfoPO.class, text);
    }

    public List<BizMsgInfoPO> queryForList(BizMsgInfoPO po) {
        return super.queryForList(po);
    }

    public void delete(BizMsgInfoPO po) {
        super.delete(po);
    }

    public void upsert(BizMsgInfoPO po) {
        invokeVoid("upsert", po);
    }

    public List<String> queryExistingSeqNos(String groupName, String projectName,String msgPrefix) {
        return invokeList("queryExistingSeqNos", String.class, groupName, projectName, msgPrefix);
    }
}
