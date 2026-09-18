package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InterfaceDataDao extends DaoSupport<InterfaceDataPO> {

    /**
     * 列表页不需要 request_json / response_json，避免一次把整棵字段树拉回客户端。
     */
    private static final String LIST_COLUMNS =
            "group_name, project_name, app_name, interface_name, trans_name, class_name, " +
            "trans_comment_zh, trans_comment_en, interface_url, method_url, lable_name, associat_enum, status";

    public void save(InterfaceDataPO po) {
        super.insert(po);
    }

    public void batchDelete(InterfaceDataPO po) {
        super.delete(po);
    }

    public void batchBackUp(String groupName,String appName) {
        String esql = "delete from interface_data_his where exists (" +
                "select 1 from interface_data where interface_data_his.group_name=interface_data.group_name " +
                     "and interface_data_his.app_name =interface_data.app_name and interface_data_his.trans_name=interface_data.trans_name " +
                     "and interface_data_his.class_name=interface_data.class_name) " +
                     "and interface_data_his.group_name='"+groupName+"' and interface_data_his.app_name='"+appName+"'";
        super.updateBySql(esql);
        String hql = "insert into interface_data_his (group_name,project_name,app_name,interface_name," +
                "trans_name,class_name,trans_comment_zh,trans_comment_en,interface_url,method_url) " +
                "select group_name,project_name,app_name,interface_name,trans_name,class_name," +
                "trans_comment_zh,trans_comment_en,interface_url,method_url from interface_data where group_name='"+groupName+
                "' and app_name='"+appName+"'";
        super.updateBySql(hql);
    }

    @Override
    public List<InterfaceDataPO> queryForList(InterfaceDataPO po) {
        return super.queryForList(po);
    }

    public List<InterfaceDataPO> queryForListSummary(InterfaceDataPO po) {
        StringBuilder sql = new StringBuilder("select ").append(LIST_COLUMNS).append(" from interface_data");
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        appendEq(where, params, "group_name", po == null ? null : po.getGroupName());
        appendEq(where, params, "project_name", po == null ? null : po.getProjectName());
        appendEq(where, params, "app_name", po == null ? null : po.getAppName());
        appendEq(where, params, "lable_name", po == null ? null : po.getLableName());
        sql.append(where);
        if (params.isEmpty()) {
            return super.queryListBySql(sql.toString(), InterfaceDataPO.class);
        }
        return super.queryListBySql(sql.toString(), InterfaceDataPO.class, params.toArray());
    }

    public List<InterfaceDataPO> queryForSearch(String appName, String text) {
        String sql = "select " + LIST_COLUMNS + " from interface_data where app_name = ?" +
                " and (trans_name like ? or trans_comment_zh like ? or trans_comment_en like ? or method_url like ?)";
        String like = "%" + StringUtils.defaultString(text) + "%";
        return super.queryListBySql(sql, InterfaceDataPO.class, appName, like, like, like, like);
    }

    private void appendEq(StringBuilder where, List<Object> params, String column, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (where.length() == 0) {
            where.append(" where ");
        } else {
            where.append(" and ");
        }
        where.append(column).append(" = ?");
        params.add(value);
    }
}
