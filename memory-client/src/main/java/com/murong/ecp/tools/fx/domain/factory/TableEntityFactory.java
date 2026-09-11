package com.murong.ecp.tools.fx.domain.factory;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.infrastructure.converter.DatabaseConvert;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.CommonClassDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.TableDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class TableEntityFactory {
    @Autowired
    GlobalProperties globalProps;
    @Autowired
    private TableDataDao tableDataDao;
    @Autowired
    private CommonClassDao commonClassDao;

    public TableEntity assembleCommonClass(TableEntity tableEntity){
        if (tableEntity != null && StringUtils.isNotBlank(tableEntity.getParentClass())) {
            CommonClassPO commonClassPO = commonClassDao.queryByClassPath(tableEntity.getParentClass());
            List<RxField> rxFields = DatabaseConvert.jsonToFields(commonClassPO.getFieldsJson());
            if(!CollectionUtils.isEmpty(rxFields)){
                tableEntity.getRxFields().addAll(rxFields);
            }
            return tableEntity;
        }else {
            return tableEntity;
        }
    }

    public TableEntity getTableEntity(String tableName) {
        TableDataPO reqPO = new TableDataPO();
        reqPO.setGroupName(globalProps.getGroupName());
        reqPO.setProjectName(globalProps.getProjectName());
        reqPO.setAppName(globalProps.getAppName());
        reqPO.setTableNameSnake(tableName);
        return DatabaseConvert.toEntity(tableDataDao.queryOne(reqPO));
    }
    public List<String> getTableList() {
        String sql="select table_name_snake from table_data where group_name='"+globalProps.getGroupName()+"' and project_name='"+globalProps.getProjectName()+"' and app_name='"+globalProps.getAppName()+"'";
        List<String> tableList = tableDataDao.queryListBySql(sql, String.class);
        return tableList;
    }
}
