package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TableDataDao extends DaoSupport<TableDataPO> {

    @Autowired
    GlobalProperties globalPropes;

    public void save(TableDataPO po) {
        super.insert(po);
    }

    public List<TableDataPO> queryForList (TableDataPO po) {
        return super.queryForList(po);
    }

    public List<TableDataPO> queryForSearch(String text) {
        String sql = "select * from table_data where group_name='"+globalPropes.getGroupName()+"'"
                +" and project_name='"+globalPropes.getProjectName()+
                "' and (table_name_snake like '%" +text+ "%' or table_comment_cn like '%" +text+"%' or table_comment_en like '%" +text+"%')";
        return queryListBySql(sql,TableDataPO.class);
    }

    public void delete(TableDataPO po) {
        super.delete(po);
    }
}
