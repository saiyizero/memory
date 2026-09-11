package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataHisPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InterfaceDataDao extends DaoSupport<InterfaceDataPO> {

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

    public List<InterfaceDataPO> queryForSearch(String appName, String text) {
        String sql="select * from interface_data where app_name='" +appName+
                "' and (trans_name like '%" +text+
                "%' or trans_comment_zh like '%" +text+"%' or trans_comment_en like '%"+text+
                "%' or method_url like '%" +text+"%') ";
        return super.queryListBySql(sql, InterfaceDataPO.class);
    }
}
