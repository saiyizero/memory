package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.ExeSqlRecordQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ExeSqlRecordDao extends DaoSupport<ExeSqlRecordPO> {

    public void save(ExeSqlRecordPO po) {
        super.insert(po);
    }

    public List<ExeSqlRecordPO> queryForList(ExeSqlRecordPO po) {
        return super.queryForList(po);
    }

    public List<ExeSqlRecordPO> queryByDateRange(ExeSqlRecordQuery query) {
        String beginDt=query.getBeginDt().replace("-", "");
        String endDt=query.getEndDt().replace("-", "");
        StringBuilder sql = new StringBuilder("SELECT * FROM exesql_record WHERE 1=1");
        if (beginDt != null && !beginDt.isEmpty()) {
            sql.append(" and exe_date >= '"+beginDt+"'");
        }
        if (endDt != null && !endDt.isEmpty()) {
            sql.append(" and exe_date <= '"+endDt+"'");
        }
        if (query.getEnvName() != null && !query.getEnvName().isEmpty()) {
            sql.append(" and env_name = '"+query.getEnvName()+"'");
        }
        if (query.getSchemaNm() != null && !query.getSchemaNm().isEmpty()) {
            sql.append(" and schema_nm = '"+query.getSchemaNm()+"'");
        }
        if (query.getGroupName() != null && !query.getGroupName().isEmpty()) {
            sql.append(" and group_name = '"+query.getGroupName()+"'");
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String kw = query.getKeyword().replace("'", "''");
            sql.append(" and (exe_sql like '%"+kw+"%' or update_by like '%"+kw+"%')");
        }
        sql.append(" ORDER BY exe_date DESC, exe_time DESC");
        return super.queryListBySql(sql.toString(), ExeSqlRecordPO.class);
    }

    public void delete(ExeSqlRecordPO po) {
        super.delete(po);
    }
} 