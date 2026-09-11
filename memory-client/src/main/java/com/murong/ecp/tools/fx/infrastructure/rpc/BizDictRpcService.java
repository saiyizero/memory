package com.murong.ecp.tools.fx.infrastructure.rpc;


import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务字典 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class BizDictRpcService extends HttpDaoSupport<BizDictPO> {

    public void updateEnumRef(BizDictPO bizDictPO){
        invokeVoid("updateEnumRef", bizDictPO);
    }

    public List<BizDictPO> searchByName (String searchText,String appName) {
        return invokeList("searchByName", BizDictPO.class, searchText, appName);
    }

    public List<BizDictPO> searchByName (String name) {
        return invokeList("searchByName", BizDictPO.class, name);
    }

    public BizDictPO queryOne(BizDictPO po) {
        return invoke("queryOne", BizDictPO.class, po);
    }

    public BizDictPO save(BizDictPO po) {
        return invoke("save", BizDictPO.class, po);
    }

    public void upsert(BizDictPO po) {
        invokeVoid("upsert", po);
    }

    public BatchWriteResult upsertAll(List<BizDictPO> list) {
        BatchWriteResult result = invoke("upsertAll", BatchWriteResult.class, list);
        return result == null ? BatchWriteResult.of(0, 0) : result;
    }
}
