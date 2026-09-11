package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BaseDictPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基础字典数据访问对象
 */
@Service
public class BaseDictRpcService extends HttpDaoSupport<BaseDictPO> {

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
     * 根据搜索文本查询基础字典列表
     */
    public List<BaseDictPO> searchByName(String searchText) {
        return invokeList("searchByName", BaseDictPO.class, searchText);
    }

    public void upsert(BaseDictPO po) {
        invokeVoid("upsert", po);
    }

    public BatchWriteResult upsertAll(List<BaseDictPO> list) {
        BatchWriteResult result = invoke("upsertAll", BatchWriteResult.class, list);
        return result == null ? BatchWriteResult.of(0, 0) : result;
    }
}
