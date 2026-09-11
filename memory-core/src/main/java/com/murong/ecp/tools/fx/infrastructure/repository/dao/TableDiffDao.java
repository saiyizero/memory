package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 表结构差异记录DAO
 */
@Repository
public class TableDiffDao extends DaoSupport<TableDiffPO> {
    
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
        
        return jdbcTemplate.query(finalSql, params.toArray(), new TableDiffRowMapper());
    }
    
    /**
     * 自定义RowMapper，处理枚举类型转换
     */
    private static class TableDiffRowMapper implements RowMapper<TableDiffPO> {
        @Override
        public TableDiffPO mapRow(ResultSet rs, int rowNum) throws SQLException {
            TableDiffPO diff = new TableDiffPO();
            
            diff.setGroupName(rs.getString("group_name"));
            diff.setProjectName(rs.getString("project_name"));
            diff.setAppName(rs.getString("app_name"));
            diff.setTableNameCamel(rs.getString("table_name_camel"));
            diff.setTableNameSnake(rs.getString("table_name_snake"));
            diff.setSchemaNm(rs.getString("schema_nm"));
            diff.setCompareEnv(rs.getString("compare_env"));
            
            // 处理枚举类型转换
            String diffTypeStr = rs.getString("diff_type");
            if (diffTypeStr != null) {
                diff.setDiffType(DiffTypeEnum.getByCode(diffTypeStr));
            }
            
            diff.setLocalValue(rs.getString("local_value"));
            diff.setRemoteValue(rs.getString("remote_value"));
            diff.setDiffFieldCount(rs.getInt("diff_field_count"));
            diff.setUpdateBy(rs.getString("update_by"));
            diff.setUpdateTime(rs.getString("update_time"));
            
            return diff;
        }
    }
} 