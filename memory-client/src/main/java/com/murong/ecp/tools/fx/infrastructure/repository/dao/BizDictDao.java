package com.murong.ecp.tools.fx.infrastructure.repository.dao;


import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class BizDictDao extends HttpDaoSupport<BizDictPO> {
    @Autowired
    private GlobalProperties globalPropes;

    public void updateEnumRef(BizDictPO bizDictPO){
        BizDictPO updatePO = new BizDictPO();
        updatePO.setEnumRef(bizDictPO.getEnumRef());
        updatePO.setEnumNme(bizDictPO.getEnumNme());

        BizDictPO wherePO = new BizDictPO();
        wherePO.setGroupName(bizDictPO.getGroupName());
        wherePO.setProjectName(bizDictPO.getProjectName());
        wherePO.setNameCamel(bizDictPO.getNameCamel());

        super.updateByOne(updatePO,wherePO);
    }


    public List<BizDictPO> searchByName (String searchText,String appName) {
        String sql = "select * from biz_dict where group_name ='" + globalPropes.getGroupName() + "'";
        if (StringUtils.isNotBlank(appName) && !StringUtils.equals(appName,"全部")) {
            sql=sql+ " and app_name in ('" + appName + "','pub')";
        }
        if (StringUtils.isNotBlank(searchText)) {
            sql=sql+ " and ( name_camel like '%" + searchText + "%' or name_snake like '%" + searchText +
                    "%' or comment_cn like '%" + searchText + "%' or comment_en like '%" + searchText + "%')";
        }
        return queryListBySql(sql,BizDictPO.class);
    }

    public List<BizDictPO> searchByName (String name) {
        if(StringUtils.isBlank(name)) {
            BizDictPO bizDictPO = new BizDictPO();
            bizDictPO.setGroupName(globalPropes.getGroupName());
            return queryForList(bizDictPO);
        }else {
            String sql = "select * from biz_dict where group_name ='" + globalPropes.getGroupName() + "' and app_name in ('" + globalPropes.getAppName() + "','pub')"+
                    " and ( name_camel like '%" + name + "%' or name_snake like '%" + name + "%' or comment_cn like '%" + name + "%' or comment_en like '%" + name + "%')";
            return queryListBySql(sql,BizDictPO.class);
        }
    }

    public BizDictPO queryOne(BizDictPO po) {
        String sql = "select * from biz_dict where name_camel = '" + po.getNameCamel() + "' AND group_name = '" +
                po.getGroupName() + "' AND project_name = '" + po.getProjectName() +
                "' AND app_name = '" + po.getAppName() + "' LIMIT 1";
        return queryOneBySql(sql);
    }

    public BizDictPO save(BizDictPO po) {
        if(StringUtils.isNotBlank(po.getNameCamel()) && po.getNameCamel().contains(".")){
            String[] nameCamel = po.getNameCamel().split("\\.");
            po.setNameCamel(nameCamel[1]);
        }
        if(StringUtils.isNotBlank(po.getNameSnake()) && po.getNameSnake().contains(".")){
            String[] nameSnake = po.getNameSnake().split("\\.");
            po.setNameSnake(nameSnake[1]);
        }

        BizDictPO orgBizDictPO = queryOne(po);
        if (orgBizDictPO == null) {
            if(StringUtils.isBlank(po.getCommentCn()) ||StringUtils.isBlank(po.getCommentEn())
                    ||po.getLength()==null||po.getLength()<=0 ){
                po.setStatus(DataStatusEnum.PENDING.getCode());
            }else {
                po.setStatus(DataStatusEnum.REVIEW.getCode());
            }
            insert(po);
            return po;
        }else if(
            StringUtils.equals(orgBizDictPO.getStatus() , DataStatusEnum.PENDING.getCode())||
            (StringUtils.isBlank(orgBizDictPO.getEnumNme()) && StringUtils.isNotBlank(po.getEnumNme()))||
            (po.getLength()!=null && po.getLength()>orgBizDictPO.getLength())||
            (StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_DBTYP.getCode()) && !StringUtils.equals(po.getDbTyp(),orgBizDictPO.getDbTyp())) ||
            (StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_DBTYP_LENGTH.getCode()) && !StringUtils.equals(po.getDbTyp(),orgBizDictPO.getDbTyp())) ||
            (StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_LENGTH.getCode()) && po.getLength()!=orgBizDictPO.getLength())
        ) {
            if(StringUtils.isBlank(orgBizDictPO.getEnumNme()) && StringUtils.isNotBlank(po.getEnumNme())){
                orgBizDictPO.setEnumNme(po.getEnumNme());
                orgBizDictPO.setEnumRef(po.getEnumRef());
            }
            if(StringUtils.isBlank(orgBizDictPO.getCommentEn()) && StringUtils.isNotBlank(po.getCommentEn())){
                orgBizDictPO.setCommentEn(po.getCommentEn());
            }
            if(StringUtils.isBlank(orgBizDictPO.getCommentCn()) && StringUtils.isNotBlank(po.getCommentCn())){
                orgBizDictPO.setCommentCn(po.getCommentCn());
            }
            if((orgBizDictPO.getLength()==null||orgBizDictPO.getLength()<=0) && (po.getLength()!=null && po.getLength()>0)){
                orgBizDictPO.setLength(po.getLength());
            }
            if(StringUtils.isBlank(orgBizDictPO.getEnumNme()) || StringUtils.isBlank(orgBizDictPO.getCommentCn())
                    ||StringUtils.isBlank(orgBizDictPO.getCommentEn())||orgBizDictPO.getLength()==null||orgBizDictPO.getLength()<=0 ){
                orgBizDictPO.setStatus(DataStatusEnum.PENDING.getCode());
            }else {
                orgBizDictPO.setStatus(DataStatusEnum.REVIEW.getCode());
            }
            if(po.getLength()!=null && po.getLength()> orgBizDictPO.getLength()){
                orgBizDictPO.setLength(po.getLength());
            }

            if(StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_DBTYP.getCode())){
                orgBizDictPO.setDbTyp(po.getDbTyp());
            }
            if(StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_DBTYP_LENGTH.getCode())){
                orgBizDictPO.setDbTyp(po.getDbTyp());
                orgBizDictPO.setLength(po.getLength());
            }
            if(StringUtils.equals(po.getStatus(),DataStatusEnum.UPD_LENGTH.getCode())){
                orgBizDictPO.setLength(po.getLength());
            }

            BizDictPO wherePO = new BizDictPO();
            wherePO.setGroupName(orgBizDictPO.getGroupName());
            wherePO.setProjectName(orgBizDictPO.getProjectName());
            wherePO.setNameCamel(orgBizDictPO.getNameCamel());
            wherePO.setAppName(orgBizDictPO.getAppName());
            updateByOne(orgBizDictPO,wherePO);
            return orgBizDictPO;
        }else {
            return orgBizDictPO;
        }
    }
}
