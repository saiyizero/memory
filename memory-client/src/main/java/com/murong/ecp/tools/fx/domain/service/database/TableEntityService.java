package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.service.GenerateCodeService;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.enums.ExeStatusEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.cache.BizDictCache;
import com.murong.ecp.tools.fx.infrastructure.converter.DatabaseConvert;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.CommonClassDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.TableDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.TableRecordDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableRecordPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class TableEntityService {
    @Autowired
    GlobalProperties globalPropes;
    @Autowired
    GenerateCodeService generateCodeService;
    @Autowired
    TableRecordDao tableRecordDao;
    @Autowired
    TableDataDao tableDataDao;
    @Autowired
    CommonClassDao commonClassDao;
    @Autowired
    BizDictDao bizDictDao;
    @Autowired
    BizDictCache bizDictCache;

    /**
     * 同步表结构（带进度回调）
     **/
    public CrResult synchTableStructure(BiConsumer<Double, String> progressCallback) {
        DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();

        List<String> allTableNameLst = dataBaseHandler.getAllTableNameLst();
        int totalTables = allTableNameLst.size();
        int processedTables = 0;
        
        for (String tableName : allTableNameLst) {
            processedTables++;
            double progress = (double) processedTables / totalTables;
            
            // 报告进度和当前表名
            if (progressCallback != null) {
                progressCallback.accept(progress, tableName);
            }

            TableDataPO dataIndex = new TableDataPO();
            dataIndex.setGroupName(globalPropes.getGroupName());
            dataIndex.setProjectName(globalPropes.getProjectName());
            dataIndex.setAppName(globalPropes.getAppName());
            dataIndex.setTableNameSnake(tableName);
            TableDataPO resultPO = tableDataDao.queryOne(dataIndex);
            if (resultPO != null) {
                System.out.println("表：" + tableName + "已存在");
                continue;
            }
            
            TableEntity tableEntity = dataBaseHandler.getTableEntityFromDatabase(tableName);
            tableEntity=dataBaseHandler.adjustTableEntity(tableEntity);
            if (tableEntity == null) {
                System.out.println("表：" + tableName + "无法查询到表结构");
                continue;
            }

            for (RxField rxField : tableEntity.getRxFields()) {
                BizDictPO po = new BizDictPO();
                po.setGroupName(globalPropes.getGroupName());
                po.setProjectName(globalPropes.getProjectName());
                po.setAppName(globalPropes.getAppName());
                po.setNameCamel(rxField.getNameCamel());
                po.setNameSnake(rxField.getNameSnake());
                po.setType(rxField.getType());
                po.setDbTyp(rxField.getDbTyp());
                po.setNotNull(null);
                po.setEnumNme(rxField.getEnumNme());
                po.setEnumRef(rxField.getEnumRef());
                po.setDefaultValue(rxField.getDefaultValue());
                po.setCommentCn(rxField.getCommentCn());
                po.setCommentEn(rxField.getCommentEn());
                po.setLength(rxField.getLength());
                if(rxField.getBizUpdSts()!=null){
                    po.setStatus(rxField.getBizUpdSts().getCode());
                }
                if(!bizDictCache.existsInPublicBizDict(po)){
                    BizDictPO rspPO = bizDictDao.save(po);
                    rxField.setNameCamel(rspPO.getNameCamel());
                    rxField.setNameSnake(po.getNameSnake());
                    rxField.setType(rspPO.getType());
                    rxField.setDbTyp(rspPO.getDbTyp());
                    rxField.setEnumNme(rspPO.getEnumNme());
                    rxField.setEnumRef(rspPO.getEnumRef());
                    rxField.setDefaultValue(rspPO.getDefaultValue());
                    rxField.setCommentCn(rspPO.getCommentCn());
                    rxField.setCommentEn(rspPO.getCommentEn());
                    rxField.setLength(rspPO.getLength());
                }else {
                    BizDictPO rspPO = bizDictCache.getPublicBizDict(po);
                    rxField.setNameCamel(rspPO.getNameCamel());
                    rxField.setNameSnake(po.getNameSnake());
                    rxField.setType(rspPO.getType());
                    rxField.setDbTyp(rspPO.getDbTyp());
                    rxField.setEnumNme(rspPO.getEnumNme());
                    rxField.setEnumRef(rspPO.getEnumRef());
                    rxField.setDefaultValue(rspPO.getDefaultValue());
                    rxField.setCommentCn(rspPO.getCommentCn());
                    rxField.setCommentEn(rspPO.getCommentEn());
                    rxField.setLength(rspPO.getLength());
                }
            }

            TableDataPO po = DatabaseConvert.toPO(tableEntity);
            // 设置全局属性
            po.setGroupName(globalPropes.getGroupName());
            po.setProjectName(globalPropes.getProjectName());
            po.setAppName(globalPropes.getAppName());
            if(resultPO == null) {
                po.setGenerCdFlg(FlgEnum.YES.getValue());
                po.setCreateTabFlg(FlgEnum.YES.getValue());
                po.setStatus(DataStatusEnum.PENDING.getCode());
                tableDataDao.insert(po);
            }
        }

        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    /**
     * 删除表
     **/
    public CrResult dropTableEntity(TableEntity tableEntity) {

        //登记删除记录
        String dropSql = "DROP TABLE " + tableEntity.getTableNameSnake() + ";";
        TableRecordPO record = DatabaseConvert.toRecordPO(tableEntity);
        record.setExecsqlJson(dropSql);
        tableRecordDao.save(record);
        try {
            DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();
            dataBaseHandler.executeSql(dropSql);
        }catch (Exception e) {
            TableRecordPO indexPO = new TableRecordPO();
            indexPO.setId(record.getId());
            record.setStatus(ExeStatusEnum.FAIL.getKey());
            tableRecordDao.updateByOne(record,indexPO);
            if(!StringUtils.contains(e.getMessage(), "not exist")) {
                CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                crResult.setMsgInf(e.getMessage());
                return crResult;
            }
        }

        //删除表数据
        TableDataPO dataIndex = new TableDataPO();
        dataIndex.setGroupName(globalPropes.getGroupName());
        dataIndex.setProjectName(globalPropes.getProjectName());
        dataIndex.setAppName(globalPropes.getAppName());
        dataIndex.setTableNameSnake(tableEntity.getTableNameSnake());
        tableDataDao.delete(dataIndex);

        //更新执行成功
        TableRecordPO indexPO = new TableRecordPO();
        indexPO.setId(record.getId());
        record.setStatus(ExeStatusEnum.SUCCESS.getKey());
        tableRecordDao.updateByOne(record,indexPO);

        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    /**
     * 生成单张DDL建表语句
     **/
    public CrResult<String> generateDdl(TableEntity tableEntity) {
        String parentClass = tableEntity.getParentClass();
        List<RxField> rxFieldList=null;
        if(StringUtils.isNotBlank(parentClass)) {
            CommonClassPO commonClassPO = commonClassDao.queryByClassPath(parentClass);
            String fieldsJson = commonClassPO.getFieldsJson();
            rxFieldList = DatabaseConvert.jsonToFields(fieldsJson);
        }else {
            rxFieldList=new ArrayList<RxField>();
        }




        //检查是否存在主键
        if(tableEntity.getPrimaryKey()==null){
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("表:"+tableEntity.getTableNameSnake()+" 主键不存在！");
            return crResult;
        }

        // 去重后与原tableEntity.fields合并成tableEntity.fields
        List<RxField> originRxFields = tableEntity.getRxFields();
        List<RxField> mergedRxFields = new java.util.ArrayList<>();
        if (originRxFields != null && !originRxFields.isEmpty()) {
            mergedRxFields.addAll(originRxFields);
        }
        if (rxFieldList != null && !rxFieldList.isEmpty()) {
            java.util.LinkedHashMap<String, RxField> fieldMap = new java.util.LinkedHashMap<>();
            for (RxField f : mergedRxFields) {
                fieldMap.put(f.getNameSnake(), f);
            }
            for (RxField f : rxFieldList) {
                fieldMap.putIfAbsent(f.getNameSnake(), f);
            }
            mergedRxFields = new java.util.ArrayList<>(fieldMap.values());
        }
        tableEntity.setRxFields(mergedRxFields);
        DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();
        String ddlSql = dataBaseHandler.generateCreateTabSql(tableEntity);
        CrResult<String> ddlCrResult=CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        ddlCrResult.setData(ddlSql);
        return ddlCrResult;
    }

    /**
     * 批量生成多张表的DDL建表语句
     **/
    public CrResult<String> generateBatchDdl(List<TableEntity> tableEntityList) {
        if (tableEntityList == null || tableEntityList.isEmpty()) {
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("表实体列表不能为空！");
            return crResult;
        }

        StringBuilder allDdlContent = new StringBuilder();
        List<String> failedTables = new ArrayList<>();

        // 为每个表生成DDL
        for (TableEntity tableEntity : tableEntityList) {
            try {
                CrResult<String> singleResult = generateDdl(tableEntity);
                if (singleResult != null && singleResult.isSucess()) {
                    String ddlContent = singleResult.getData();
                    
                    // 添加表注释分隔符
                    if (allDdlContent.length() > 0) {
                        allDdlContent.append("\n\n");
                    }
                    allDdlContent.append("-- =============================================\n");
                    allDdlContent.append("-- 表名: ").append(tableEntity.getTableNameCamel()).append("\n");
                    allDdlContent.append("-- 描述: ").append(tableEntity.getTableCommentCn() != null ? tableEntity.getTableCommentCn() : "").append("\n");
                    allDdlContent.append("-- =============================================\n\n");
                    allDdlContent.append(ddlContent);
                    
                    // 添加分号结束符
                    if (!ddlContent.trim().endsWith(";")) {
                        allDdlContent.append(";");
                    }
                } else {
                    failedTables.add(tableEntity.getTableNameCamel() + " (生成DDL失败: " + (singleResult != null ? singleResult.getMsgInf() : "未知错误") + ")");
                }
            } catch (Exception e) {
                e.printStackTrace();
                failedTables.add(tableEntity.getTableNameCamel() + " (异常: " + e.getMessage() + ")");
            }
        }

        // 如果有失败的表，在返回结果中包含错误信息
        if (!failedTables.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("以下表生成DDL失败:\n");
            for (String failedTable : failedTables) {
                errorMsg.append("- ").append(failedTable).append("\n");
            }
            
            // 即使有失败的表，如果有成功的DDL内容，仍然返回成功，但包含错误信息
            if (allDdlContent.length() > 0) {
                CrResult<String> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
                result.setData(allDdlContent.toString());
                result.setMsgInf(errorMsg.toString());
                return result;
            } else {
                // 如果全部失败，返回失败结果
                CrResult<String> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf(errorMsg.toString());
                return result;
            }
        }

        // 全部成功的情况
        CrResult<String> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        result.setData(allDdlContent.toString());
        return result;
    }

    /**
     * 根据实体保存单张表
     **/
    public CrResult saveTableEntity(TableEntity tableEntity) {
        String parentClass = tableEntity.getParentClass();

        List<RxField> rxFieldList=new ArrayList<>();
        CommonClassPO commonClassPO =null;
        if(StringUtils.isNotBlank(parentClass)){
            commonClassPO = commonClassDao.queryByClassPath(parentClass);
            String fieldsJson = commonClassPO.getFieldsJson();
            rxFieldList = DatabaseConvert.jsonToFields(fieldsJson);
        }


        //检查是否存在主键
        if(tableEntity.getPrimaryKey()==null){
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("表:"+tableEntity.getTableNameSnake()+" 主键不存在！");
            return crResult;
        }

        // 去重后与原tableEntity.fields合并成tableEntity.fields
        List<RxField> originRxFields = tableEntity.getRxFields();
        List<RxField> mergedRxFields = new ArrayList<>();
        if (originRxFields != null && !originRxFields.isEmpty()) {
            mergedRxFields.addAll(originRxFields);
        }
        if (rxFieldList != null && !rxFieldList.isEmpty()) {
            LinkedHashMap<String, RxField> fieldMap = new LinkedHashMap<>();
            for (RxField f : mergedRxFields) {
                fieldMap.put(f.getNameSnake(), f);
            }
            for (RxField f : rxFieldList) {
                fieldMap.putIfAbsent(f.getNameSnake(), f);
            }
            mergedRxFields = new ArrayList<>(fieldMap.values());
        }
        tableEntity.setRxFields(mergedRxFields);

        CrResult<String> ddlCrResult=null;

        if(StringUtils.equals(tableEntity.getCreateTabFlg(),FlgEnum.YES.getValue())){
            DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();
            if (dataBaseHandler.isTableExist(tableEntity.getTableNameSnake())) {
                try {
                    boolean hasDataFlg=dataBaseHandler.ifHasData(tableEntity.getTableNameSnake());
                    if(hasDataFlg) {
                        ddlCrResult = dataBaseHandler.backupTabSql(tableEntity,commonClassPO);
                    }else {
                        ddlCrResult = dataBaseHandler.dropAfterCreateTabSql(tableEntity);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    String message = e.getMessage();
                    if(StringUtils.contains(message, "not exist")) {
                        String ddlSql = dataBaseHandler.generateCreateTabSql(tableEntity);
                        ddlCrResult=CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
                        ddlCrResult.setData(ddlSql);
                    }else {
                        CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                        crResult.setMsgInf(e.getMessage());
                        return crResult;
                    }
                }
            }else {
                String ddlSql = dataBaseHandler.generateCreateTabSql(tableEntity);
                ddlCrResult=CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
                ddlCrResult.setData(ddlSql);
            }
        }

        // tableEntity.fields减去fieldList里面的字段
        if (tableEntity.getRxFields() != null && rxFieldList != null && !rxFieldList.isEmpty()) {
            List<String> removeKeys = rxFieldList.stream()
                    .map(f -> f.getNameCamel() + "_" + f.getNameSnake())
                    .collect(Collectors.toList());
            List<RxField> filtered = tableEntity.getRxFields().stream()
                    .filter(f -> !removeKeys.contains(f.getNameCamel() + "_" + f.getNameSnake()))
                    .collect(Collectors.toList());
            tableEntity.setRxFields(filtered);
        }

        TableRecordPO record = DatabaseConvert.toRecordPO(tableEntity);
        if(ddlCrResult!=null){
            record.setExecsqlJson(ddlCrResult.getData());
        }
        tableRecordDao.save(record);

        try {
            if(StringUtils.equals(tableEntity.getCreateTabFlg(),FlgEnum.YES.getValue())){
                DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();
                dataBaseHandler.executeSql(record.getExecsqlJson());

                //更新执行成功
                TableRecordPO indexPO = new TableRecordPO();
                indexPO.setId(record.getId());
                record.setStatus(ExeStatusEnum.SUCCESS.getKey());
                tableRecordDao.updateByOne(record,indexPO);
            }

            //存储数据
            TableDataPO po = DatabaseConvert.toPO(tableEntity);
            // 设置全局属性
            po.setGroupName(globalPropes.getGroupName());
            po.setProjectName(globalPropes.getProjectName());
            po.setAppName(globalPropes.getAppName());
            TableDataPO dataIndex = new TableDataPO();
            dataIndex.setGroupName(po.getGroupName());
            dataIndex.setProjectName(po.getProjectName());
            dataIndex.setAppName(po.getAppName());
            dataIndex.setTableNameSnake(po.getTableNameSnake());
            TableDataPO resultPO = tableDataDao.queryOne(dataIndex);
            if(resultPO != null) {
                tableDataDao.updateByOne(po,dataIndex);
            }else {
                tableDataDao.insert(po);
            }

        }catch (Exception e) {
            e.printStackTrace();
            TableRecordPO indexPO = new TableRecordPO();
            indexPO.setId(record.getId());
            record.setStatus(ExeStatusEnum.FAIL.getKey());
            tableRecordDao.updateByOne(record,indexPO);

            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf(e.getMessage());
            return crResult;
        }

        if(StringUtils.equals(tableEntity.getCreateTabFlg(),FlgEnum.YES.getValue()) &&
        StringUtils.isNotBlank(ddlCrResult.getDropsql()) ){
            DataBaseHandler dataBaseHandler = globalPropes.getInputDataBaseHandler();
            dataBaseHandler.executeSql(ddlCrResult.getDropsql());
        }

        //生成对应的java代码
        if(StringUtils.equals(tableEntity.getGenerCdFlg(),FlgEnum.YES.getValue())){
            tableEntity.setRxFields(mergedRxFields);
            generateCodeService.generTableCode(tableEntity);
        }


        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }
}
