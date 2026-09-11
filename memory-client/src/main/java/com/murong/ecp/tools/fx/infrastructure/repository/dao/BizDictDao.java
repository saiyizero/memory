package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 业务字典 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class BizDictDao extends HttpDaoSupport<BizDictPO> {

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
}
