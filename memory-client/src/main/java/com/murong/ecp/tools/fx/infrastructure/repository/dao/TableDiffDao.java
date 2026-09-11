package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 表结构差异记录DAO
 */
@Repository
public class TableDiffDao extends HttpDaoSupport<TableDiffPO> {
    
    /**
     * 批量保存差异记录
     */
    public void batchSave(List<TableDiffPO> diffList) {
        for (TableDiffPO diff : diffList) {
            insert(diff);
        }
    }
    
    /**
     * 查询差异记录列表
     */
    public List<TableDiffPO> queryForList(TableDiffPO query) {
        return super.queryForList(query);
    }
    
    /**
     * 使用自定义RowMapper查询
     */
    public List<TableDiffPO> queryForListWithCustomMapper(TableDiffPO query) {
        // 构建查询SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM table_diff WHERE 1=1");
        
        if (query.getGroupName() != null) {
            sql.append(" AND group_name = ?");
        }
        if (query.getProjectName() != null) {
            sql.append(" AND project_name = ?");
        }
        if (query.getAppName() != null) {
            sql.append(" AND app_name = ?");
        }
        if (query.getTableNameSnake() != null) {
            sql.append(" AND table_name_snake = ?");
        }
        if (query.getSchemaNm() != null) {
            sql.append(" AND schema_nm = ?");
        }
        if (query.getCompareEnv() != null) {
            sql.append(" AND compare_env = ?");
        }
        if (query.getDiffType() != null) {
            sql.append(" AND diff_type = ?");
        }
        
        // 构建参数
        List<Object> params = new java.util.ArrayList<>();
        if (query.getGroupName() != null) params.add(query.getGroupName());
        if (query.getProjectName() != null) params.add(query.getProjectName());
        if (query.getAppName() != null) params.add(query.getAppName());
        if (query.getTableNameSnake() != null) params.add(query.getTableNameSnake());
        if (query.getSchemaNm() != null) params.add(query.getSchemaNm());
        if (query.getCompareEnv() != null) params.add(query.getCompareEnv());
        if (query.getDiffType() != null) params.add(query.getDiffType().getCode());
        
        String finalSql = sql.toString();
        
        List<java.util.Map<String, Object>> rows = queryForMaps(finalSql, params.toArray());
        List<TableDiffPO> result = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> row : rows) {
            result.add(mapRow(row));
        }
        return result;
    }

    private TableDiffPO mapRow(java.util.Map<String, Object> row) {
        TableDiffPO diff = new TableDiffPO();
        diff.setGroupName(asString(row.get("group_name")));
        diff.setProjectName(asString(row.get("project_name")));
        diff.setAppName(asString(row.get("app_name")));
        diff.setTableNameCamel(asString(row.get("table_name_camel")));
        diff.setTableNameSnake(asString(row.get("table_name_snake")));
        diff.setSchemaNm(asString(row.get("schema_nm")));
        diff.setCompareEnv(asString(row.get("compare_env")));
        String diffTypeStr = asString(row.get("diff_type"));
        if (diffTypeStr != null) {
            diff.setDiffType(DiffTypeEnum.getByCode(diffTypeStr));
        }
        diff.setLocalValue(asString(row.get("local_value")));
        diff.setRemoteValue(asString(row.get("remote_value")));
        Object count = row.get("diff_field_count");
        if (count instanceof Number) {
            diff.setDiffFieldCount(((Number) count).intValue());
        }
        diff.setUpdateBy(asString(row.get("update_by")));
        diff.setUpdateTime(asString(row.get("update_time")));
        return diff;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
} 