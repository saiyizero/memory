package com.murong.ecp.tools.fx.infrastructure.rpc;


import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 接口数据 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class InterfaceDataRpcService extends HttpDaoSupport<InterfaceDataPO> {

    public void save(InterfaceDataPO po) {
        super.insert(po);
    }

    public void batchDelete(InterfaceDataPO po) {
        super.delete(po);
    }

    public void batchBackUp(String groupName,String appName) {
        invokeVoid("batchBackUp", groupName, appName);
    }

    @Override
    public List<InterfaceDataPO> queryForList(InterfaceDataPO po) {
        return super.queryForList(po);
    }

    public List<InterfaceDataPO> queryForListSummary(InterfaceDataPO po) {
        return invokeList("queryForListSummary", InterfaceDataPO.class, po);
    }

    public List<InterfaceDataPO> queryForSearch(String appName, String text) {
        return invokeList("queryForSearch", InterfaceDataPO.class, appName, text);
    }
}
