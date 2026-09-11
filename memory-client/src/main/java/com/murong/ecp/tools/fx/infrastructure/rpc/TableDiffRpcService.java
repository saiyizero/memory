package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表差异 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class TableDiffRpcService extends HttpDaoSupport<TableDiffPO> {

    public void batchSave(List<TableDiffPO> diffList) {
        invokeVoid("batchSave", diffList);
    }

    public List<TableDiffPO> queryForList(TableDiffPO query) {
        return super.queryForList(query);
    }

    public List<TableDiffPO> queryForListWithCustomMapper(TableDiffPO query) {
        return invokeList("queryForListWithCustomMapper", TableDiffPO.class, query);
    }
}
