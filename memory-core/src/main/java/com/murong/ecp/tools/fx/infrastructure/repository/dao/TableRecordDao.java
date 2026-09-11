package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableRecordPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TableRecordDao extends DaoSupport<TableRecordPO> {

    public void save(TableRecordPO po) {
        super.insert(po);
    }

    public List<TableRecordPO> queryForList (TableRecordPO po) {
        return super.queryForList(po);
    }

}
