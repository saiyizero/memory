package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.ExeSqlRecordQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SQL 执行记录 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class ExeSqlRecordDao extends HttpDaoSupport<ExeSqlRecordPO> {

    public void save(ExeSqlRecordPO po) {
        super.insert(po);
    }

    public List<ExeSqlRecordPO> queryForList(ExeSqlRecordPO po) {
        return super.queryForList(po);
    }

    public List<ExeSqlRecordPO> queryByDateRange(ExeSqlRecordQuery query) {
        return invokeList("queryByDateRange", ExeSqlRecordPO.class, query);
    }

    public void delete(ExeSqlRecordPO po) {
        super.delete(po);
    }
}
