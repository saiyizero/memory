package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.DebugLogQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DebugLogDao extends DaoSupport<DebugLogPO> {
    public void save(DebugLogPO po) {
        super.insert(po);
    }

    public List<DebugLogPO> queryForList(DebugLogPO po) {
        return super.queryForList(po,"update_time desc");
    }

    public List<DebugLogGroupPO> qryGroupForList(DebugLogQuery query) {
        StringBuilder sql = new StringBuilder("select group_name,project_name,interface_name," +
                "trans_name,max(trans_nme_dsc) as trans_nme_dsc from debug_log where 1=1");
        if (query.getBeginDt() != null && !query.getBeginDt().isEmpty()) {
            String beginDt = query.getBeginDt();
            if (beginDt.length() == 8) {
                beginDt = beginDt.substring(0, 4) + "-" + beginDt.substring(4, 6) + "-" + beginDt.substring(6, 8);
            }
            sql.append(" and update_time >= '"+beginDt+" 00:00:00'");
        }
        if (query.getEndDt() != null && !query.getEndDt().isEmpty()) {
            String endDt = query.getEndDt();
            if (endDt.length() == 8) {
                endDt = endDt.substring(0, 4) + "-" + endDt.substring(4, 6) + "-" + endDt.substring(6, 8);
            }
            sql.append(" and update_time <= '"+endDt+" 23:59:59'");
        }
        if (query.getGroupName() != null && !query.getGroupName().isEmpty()) {
            sql.append(" and group_name = '"+query.getGroupName()+"'");
        }
        if (query.getProjectName() != null && !query.getProjectName().isEmpty()) {
            sql.append(" and project_name = '"+query.getProjectName()+"'");
        }
        if (query.getIp() != null && !query.getIp().isEmpty()) {
            sql.append(" and ip = '"+query.getIp()+"'");
        }
        sql.append(" group by group_name,project_name,interface_name,trans_name order by max(update_time) desc");
        return super.queryListBySql(sql.toString(), DebugLogGroupPO.class);
    }

    /**
     * 查询指定交易组的IP汇总
     */
    public List<String> queryIpListByGroup(DebugLogGroupPO group) {
        StringBuilder sql = new StringBuilder("select distinct ip from debug_log where 1=1");
        if (group.getGroupName() != null && !group.getGroupName().isEmpty()) {
            sql.append(" and group_name = '").append(group.getGroupName()).append("'");
        }
        if (group.getProjectName() != null && !group.getProjectName().isEmpty()) {
            sql.append(" and project_name = '").append(group.getProjectName()).append("'");
        }
        if (group.getInterfaceName() != null && !group.getInterfaceName().isEmpty()) {
            sql.append(" and interface_name = '").append(group.getInterfaceName()).append("'");
        }
        if (group.getTransName() != null && !group.getTransName().isEmpty()) {
            sql.append(" and trans_name = '").append(group.getTransName()).append("'");
        }
        sql.append(" and ip is not null and ip != '' order by ip");
        return super.queryListBySql(sql.toString(), String.class);
    }
} 