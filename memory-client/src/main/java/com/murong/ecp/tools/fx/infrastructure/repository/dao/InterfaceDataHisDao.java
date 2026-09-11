package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataHisPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 接口历史 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class InterfaceDataHisDao extends HttpDaoSupport<InterfaceDataHisPO> {
    public void save(InterfaceDataHisPO po) {
        super.insert(po);
    }

    public List<InterfaceDataHisPO> queryForList(InterfaceDataHisPO po) {
        return super.queryForList(po);
    }

    public void batchDelete(InterfaceDataHisPO po) {
        super.delete(po);
    }

    public void saveAll(List<InterfaceDataHisPO> poLst) {
        invokeVoid("saveAll", poLst);
    }

    public InterfaceDataHisPO queryInfcDataHis(InterfaceDataHisPO po) {
        return invoke("queryInfcDataHis", InterfaceDataHisPO.class, po);
    }
}
