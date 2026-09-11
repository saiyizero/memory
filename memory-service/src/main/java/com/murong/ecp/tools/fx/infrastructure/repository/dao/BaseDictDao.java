package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BaseDictPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.BatchWriteResult;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基础字典数据访问对象
 */
@Repository
public class BaseDictDao extends DaoSupport<BaseDictPO> {

    /**
     * 保存基础字典
     */
    public void save(BaseDictPO po) {
        super.insert(po);
    }

    /**
     * 查询基础字典列表
     */
    public List<BaseDictPO> queryForList(BaseDictPO po) {
        return super.queryForList(po);
    }

    /**
     * 根据名称查询基础字典
     */
    public BaseDictPO queryByNameSnake(String nameSnake) {
        BaseDictPO po = new BaseDictPO();
        po.setNameSnake(nameSnake);
        return super.queryOne(po);
    }

    /**
     * 根据类型查询基础字典列表
     */
    public List<BaseDictPO> queryByType(String type) {
        BaseDictPO po = new BaseDictPO();
        po.setType(type);
        return super.queryForList(po);
    }

    /**
     * 根据数据库类型查询基础字典列表
     */
    public List<BaseDictPO> queryByDbTyp(String dbTyp) {
        BaseDictPO po = new BaseDictPO();
        po.setDbTyp(dbTyp);
        return super.queryForList(po);
    }

    /**
     * 根据状态查询基础字典列表
     */
    public List<BaseDictPO> queryByStatus(String status) {
        BaseDictPO po = new BaseDictPO();
        po.setStatus(status);
        return super.queryForList(po);
    }

    /**
     * 删除基础字典
     */
    public void delete(BaseDictPO po) {
        super.delete(po);
    }

    /**
     * 更新基础字典
     */
    public void updateByOne(BaseDictPO updatePo, BaseDictPO wherePo) {
        super.updateByOne(updatePo, wherePo);
    }

    /**
     * 按 name_snake 存在则更新，否则插入。
     */
    public void upsert(BaseDictPO po) {
        BaseDictPO where = new BaseDictPO();
        where.setNameSnake(po.getNameSnake());
        BaseDictPO existing = queryOne(where);
        if (existing != null) {
            updateByOne(po, where);
        } else {
            save(po);
        }
    }

    public BatchWriteResult upsertAll(List<BaseDictPO> list) {
        int successCount = 0;
        int errorCount = 0;
        if (list != null) {
            for (BaseDictPO po : list) {
                try {
                    upsert(po);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    e.printStackTrace();
                }
            }
        }
        return BatchWriteResult.of(successCount, errorCount);
    }

    /**
     * 根据搜索文本查询基础字典列表
     */
    public List<BaseDictPO> searchByName(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return queryForList(new BaseDictPO());
        } else {
            String sql = "select * from base_dict where " +
                    "name_snake like '%" + searchText.trim() + "%' or " +
                    "comment_cn like '%" + searchText.trim() + "%'";
            return queryListBySql(sql, BaseDictPO.class);
        }
    }
}
