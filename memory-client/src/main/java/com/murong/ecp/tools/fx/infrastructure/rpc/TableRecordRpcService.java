package com.murong.ecp.tools.fx.infrastructure.rpc;


import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableRecordPO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableRecordRpcService extends HttpDaoSupport<TableRecordPO> {

    public void save(TableRecordPO po) {
        super.insert(po);
    }

    public List<TableRecordPO> queryForList (TableRecordPO po) {
        return super.queryForList(po);
    }

}
