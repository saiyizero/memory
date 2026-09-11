package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 业务消息信息DAO
 */
@Repository
public class BizMsgInfoDao extends HttpDaoSupport<BizMsgInfoPO> {
    @Autowired
    GlobalProperties globalPropes;

    /**
     * 查询业务消息信息列表
     */
    public List<BizMsgInfoPO> queryForSearch(String text) {
        String sql = "select * from biz_msg_info where group_name='"+globalPropes.getGroupName()+"'"
                + " and project_name='"+globalPropes.getProjectName()+
        "' and (msg_desc_cn like '%" +text+ "%' or msg_desc_en like '%" +text+"%' )";
        return queryListBySql(sql,BizMsgInfoPO.class);
    }

    /**
     * 查询业务消息信息列表
     */
    public List<BizMsgInfoPO> queryForList(BizMsgInfoPO po) {
        return super.queryForList(po);
    }

    /**
     * 删除业务消息信息
     */
    public void delete(BizMsgInfoPO po) {
        super.delete(po);
    }

    /**
     * 插入或更新业务消息信息
     * 如果记录不存在则插入，如果存在则更新
     */
    public void upsert(BizMsgInfoPO po) {
        // 构造主键查询对象
        BizMsgInfoPO query = new BizMsgInfoPO();
        query.setGroupName(po.getGroupName());
        query.setProjectName(po.getProjectName());
        query.setAppName(po.getAppName());
        query.setModuleName(po.getModuleName());
        query.setMsgClass(po.getMsgClass());
        query.setMsgKey(po.getMsgKey());
        query.setMsgCd(po.getMsgCd());
        
        BizMsgInfoPO exist = this.queryOne(query);
        if (exist == null) {
            super.insert(po);
        } else {
            this.updateByOne(po, query);
        }
    }
    
    /**
     * 查询指定前缀的消息码序列号列表
     * 性能优化：使用高效SQL直接在数据库层面筛选
     * 
     * @param groupName 分组名
     * @param projectName 项目名
     * @param msgPrefix 消息前缀（如：SAVP）
     * @return 已存在的序列号列表
     */
    public List<String> queryExistingSeqNos(String groupName, String projectName,String msgPrefix) {
        // 使用高效的SQL直接在数据库层面筛选
        String sql = "SELECT SUBSTRING(msg_cd, 5) AS seq_no " +
                    "FROM biz_msg_info " +
                    "WHERE group_name = ? " +
                    "AND project_name = ? " +
                    "AND SUBSTRING(msg_cd, 0, 5)= ? " +
                    "GROUP BY msg_cd, app_name " +
                    "ORDER BY CAST(SUBSTRING(msg_cd, 5) AS INTEGER)";
        
        try {
            return queryListBySql(sql, String.class, groupName, projectName, msgPrefix);
        } catch (Exception e) {
            throw e;
        }
    }
} 