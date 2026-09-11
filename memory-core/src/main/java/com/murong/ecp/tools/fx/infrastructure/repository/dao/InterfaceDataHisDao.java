package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataHisPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InterfaceDataHisDao extends DaoSupport<InterfaceDataHisPO> {
    public void save(InterfaceDataHisPO po) {
        super.insert(po);
    }

    public List<InterfaceDataHisPO> queryForList(InterfaceDataHisPO po) {
        return super.queryForList(po);
    }

    public void batchDelete(InterfaceDataHisPO po) {
        super.delete(po);
    }

    public void saveAll(List<InterfaceDataHisPO> poLst) {
        poLst.forEach(this::save);
    }

    public InterfaceDataHisPO queryInfcDataHis(InterfaceDataHisPO po) {
        String groupName = po.getGroupName();
        String appName = po.getAppName();
        String transName = po.getTransName();
        String className = po.getClassName();
        String hisSql = "select * from interface_data_his where group_name='"+
                groupName+"' and app_name='"+appName+"' and trans_name='"+
                transName+"' and class_name='"+className+"'";
        return super.queryOneBySql(hisSql);
    }
} 