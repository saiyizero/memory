package com.murong.ecp.tools.fx.infrastructure.rpc;


import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表结构 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class TableDataRpcService extends HttpDaoSupport<TableDataPO> {

    public void save(TableDataPO po) {
        super.insert(po);
    }

    public List<TableDataPO> queryForList (TableDataPO po) {
        return super.queryForList(po);
    }

    public List<TableDataPO> queryForSearch(String text) {
        return invokeList("queryForSearch", TableDataPO.class, text);
    }

    public void delete(TableDataPO po) {
        super.delete(po);
    }
}
