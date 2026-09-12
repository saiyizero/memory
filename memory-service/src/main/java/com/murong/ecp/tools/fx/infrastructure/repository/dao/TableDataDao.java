package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TableDataDao extends DaoSupport<TableDataPO> {

    /**
     * 列表页不需要 fields_json / primary_key_json / indexes_json，避免一次把整表字段树拉回客户端。
     */
    private static final String LIST_COLUMNS =
            "group_name, project_name, app_name, module_name, table_name_camel, table_name_snake, " +
            "table_comment_cn, table_comment_en, lable_name, associat_enum, parent_class, " +
            "gener_cd_flg, create_tab_flg, def_order_by, status, update_by, update_time";

    public void save(TableDataPO po) {
        super.insert(po);
    }

    public List<TableDataPO> queryForList (TableDataPO po) {
        return super.queryForList(po);
    }

    public List<TableDataPO> queryForListSummary(TableDataPO po) {
        StringBuilder sql = new StringBuilder("select ").append(LIST_COLUMNS).append(" from table_data");
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        appendEq(where, params, "group_name", po == null ? null : po.getGroupName());
        appendEq(where, params, "project_name", po == null ? null : po.getProjectName());
        appendEq(where, params, "app_name", po == null ? null : po.getAppName());
        appendEq(where, params, "lable_name", po == null ? null : po.getLableName());
        sql.append(where);
        if (params.isEmpty()) {
            return super.queryListBySql(sql.toString(), TableDataPO.class);
        }
        return super.queryListBySql(sql.toString(), TableDataPO.class, params.toArray());
    }

    public List<TableDataPO> queryForSearch(String text) {
        StringBuilder sql = new StringBuilder("select ").append(LIST_COLUMNS).append(" from table_data");
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        ServiceRequestContext.Context ctx = ServiceRequestContext.get();
        if (ctx != null) {
            appendEq(where, params, "group_name", ctx.getGroupName());
            appendEq(where, params, "project_name", ctx.getProjectName());
        }
        if (StringUtils.isNotBlank(text)) {
            if (where.length() == 0) {
                where.append(" where ");
            } else {
                where.append(" and ");
            }
            where.append("(table_name_snake like ? or table_comment_cn like ? or table_comment_en like ?)");
            String like = "%" + text + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(where);
        if (params.isEmpty()) {
            return super.queryListBySql(sql.toString(), TableDataPO.class);
        }
        return super.queryListBySql(sql.toString(), TableDataPO.class, params.toArray());
    }

    public void delete(TableDataPO po) {
        super.delete(po);
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
