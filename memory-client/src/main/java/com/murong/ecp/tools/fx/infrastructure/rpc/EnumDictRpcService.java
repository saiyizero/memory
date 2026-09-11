package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumGroupPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 枚举字典 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class EnumDictRpcService extends HttpDaoSupport<EnumDictPO> {

    public void save(EnumDictPO po) {
        super.insert(po);
    }

    public List<EnumDictPO> queryForList(EnumDictPO po) {
        return super.queryForList(po);
    }

    public void batchDelete(EnumDictPO po) {
        super.delete(po);
    }

    public EnumDictPO queryInfcDataHis(EnumDictPO po) {
        return invoke("queryInfcDataHis", EnumDictPO.class, po);
    }

    public void batchBackUp(String groupName,String appName) {
        invokeVoid("batchBackUp", groupName, appName);
    }

    public List<EnumGroupPO> groupByEnumNme(EnumDictPO po,String appName) {
        return invokeList("groupByEnumNme", EnumGroupPO.class, po, appName);
    }

    public void saveAll(List<EnumDictPO> poLst) {
        invokeVoid("saveAll", poLst);
    }

    public void upsert(EnumDictPO po) {
        invokeVoid("upsert", po);
    }
}
