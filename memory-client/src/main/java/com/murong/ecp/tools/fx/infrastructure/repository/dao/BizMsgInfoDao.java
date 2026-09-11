package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 业务消息 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class BizMsgInfoDao extends HttpDaoSupport<BizMsgInfoPO> {

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
