package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EnumDictDao extends DaoSupport<EnumDictPO> {

    public void save(EnumDictPO po) {
        super.insert(po);
    }

    public List<EnumDictPO> queryForList(EnumDictPO po) {
        return super.queryForList(po);
    }

    public void batchDelete(EnumDictPO po) {
        super.delete(po);
    }

    public EnumDictPO queryInfcDataHis(EnumDictPO po) {
        String groupName = po.getGroupName();
        String appName = po.getAppName();
        String enumNme = po.getEnumNme();
        String enumRef = po.getEnumRef();
        String enumVal = po.getEnumVal();
        String hisSql = "select * from enum_dict_his where group_name='"+
                groupName+"' and app_name='"+appName+"' and enum_nme='"+
                enumNme+"' and enum_ref='"+enumRef+"' and enum_val='"+enumVal+"'";
        return super.queryOneBySql(hisSql);
    }

    public void batchBackUp(String groupName,String appName) {
        String esql = "delete from enum_dict_his where exists (" +
                      "select 1 from enum_dict where enum_dict_his.group_name=enum_dict.group_name " +
                         "and enum_dict_his.app_name =enum_dict.app_name and enum_dict_his.enum_nme = enum_dict.enum_nme " +
                         "and enum_dict_his.enum_ref =enum_dict.enum_ref) " +
                         "and enum_dict_his.group_name='"+groupName+"' and enum_dict_his.app_name='"+appName+"'";
        super.updateBySql(esql);
        String hql = "insert into enum_dict_his (group_name,project_name,enum_nme,enum_ref,enum_cd," +
                "enum_val,desc_cn,desc_en,db_name,app_name,update_by,update_time) " +
                "select group_name,project_name,enum_nme,enum_ref,enum_cd,enum_val,desc_cn,desc_en," +
                "db_name,app_name,update_by,update_time from enum_dict where group_name='"+groupName+
                "' and app_name='"+appName+"'";
        super.updateBySql(hql);
    }

    public List<EnumGroupPO> groupByEnumNme(EnumDictPO po,String appName) {
        String sql = "select enum_nme,enum_ref,db_name,app_name from enum_dict " +
                "where group_name='"+ po.getGroupName()+"'";
                if(StringUtils.isNotBlank(appName)){
                    sql = sql+"and app_name in ('" + appName + "','pub')";
                }
        sql = sql+" group by enum_nme,enum_ref,db_name,app_name";
        return super.queryListBySql(sql, EnumGroupPO.class);
    }

    public void saveAll(List<EnumDictPO> poLst) {
        poLst.forEach(this::save);
    }

    public void upsert(EnumDictPO po) {
        // 构造主键查询对象
        EnumDictPO query = new EnumDictPO();
        query.setGroupName(po.getGroupName());
        query.setProjectName(po.getProjectName());
        query.setAppName(po.getAppName());
        query.setEnumNme(po.getEnumNme());
        query.setEnumRef(po.getEnumRef());
        query.setEnumVal(po.getEnumVal());
        EnumDictPO exist = this.queryOne(query);
        if (exist == null) {
            this.save(po);
        } else {
            this.updateByOne(po, query);
        }
    }

} 