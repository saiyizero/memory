package com.murong.ecp.tools.fx.infrastructure.converter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.enums.UuidTypEnum;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableRecordPO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class DatabaseConvert {
    static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TableEntity toEntity(TableDataPO po) {
        if (po == null) return null;
        TableEntity entity = new TableEntity();
        entity.setGroupName(po.getGroupName());
        entity.setProjectName(po.getProjectName());
        entity.setAppName(po.getAppName());
        entity.setModuleName(po.getModuleName());
        entity.setTableNameCamel(po.getTableNameCamel());
        entity.setTableNameSnake(po.getTableNameSnake());
        entity.setTableCommentCn(po.getTableCommentCn());
        entity.setTableCommentEn(po.getTableCommentEn());
        entity.setRxFields(jsonToFields(po.getFieldsJson()));
        entity.setPrimaryKey(jsonToPrimaryKey(po.getPrimaryKeyJson()));
        entity.setIndexes(jsonToIndexes(po.getIndexesJson()));
        entity.setLableName(po.getLableName());
        entity.setAssociatEnum(po.getAssociatEnum());
        entity.setParentClass(po.getParentClass());
        entity.setGenerCdFlg(po.getGenerCdFlg());
        entity.setCreateTabFlg(po.getCreateTabFlg());
        entity.setDefOrderBy(po.getDefOrderBy());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setUpdateTime(po.getUpdateTime());
        if(StringUtils.isBlank(entity.getUpdateTime())){
            entity.setUpdateTime(MrDateUtils.getCurrentTime());
        }
        return entity;
    }

    public static TableDataPO toPO(TableEntity entity) {
        if (entity == null) return null;
        TableDataPO po = new TableDataPO();
        po.setGroupName(entity.getGroupName());
        po.setProjectName(entity.getProjectName());
        po.setAppName(entity.getAppName());
        po.setModuleName(entity.getModuleName());
        po.setTableNameCamel(entity.getTableNameCamel());
        po.setTableNameSnake(entity.getTableNameSnake());
        po.setTableCommentCn(entity.getTableCommentCn());
        po.setTableCommentEn(entity.getTableCommentEn());
        po.setFieldsJson(fieldsToJson(entity.getRxFields()));
        po.setPrimaryKeyJson(primaryKeyToJson(entity.getPrimaryKey()));
        po.setIndexesJson(indexesToJson(entity.getIndexes()));
        po.setLableName(entity.getLableName());
        po.setAssociatEnum(entity.getAssociatEnum());
        po.setParentClass(entity.getParentClass());
        po.setGenerCdFlg(entity.getGenerCdFlg());
        po.setCreateTabFlg(entity.getCreateTabFlg());
        po.setDefOrderBy(entity.getDefOrderBy());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        if(StringUtils.isBlank(po.getUpdateTime())){
            po.setUpdateTime(MrDateUtils.getCurrentTime());
        }
        return po;
    }

    public static TableDataPO toPO(TableRecordPO entity) {
        if (entity == null) return null;
        TableDataPO po = new TableDataPO();
        po.setGroupName(entity.getGroupName());
        po.setProjectName(entity.getProjectName());
        po.setAppName(entity.getAppName());
        po.setModuleName(entity.getModuleName());
        po.setTableNameCamel(entity.getTableNameCamel());
        po.setTableNameSnake(entity.getTableNameSnake());
        po.setTableCommentCn(entity.getTableCommentCn());
        po.setTableCommentEn(entity.getTableCommentEn());
        po.setFieldsJson(entity.getFieldsJson());
        po.setPrimaryKeyJson(entity.getPrimaryKeyJson());
        po.setIndexesJson(entity.getIndexesJson());
        po.setLableName(entity.getLableName());
        po.setAssociatEnum(entity.getAssociatEnum());
        po.setParentClass(entity.getParentClass());
        po.setGenerCdFlg(entity.getGenerCdFlg());
        po.setCreateTabFlg(entity.getCreateTabFlg());
        po.setDefOrderBy(entity.getDefOrderBy());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        if(StringUtils.isBlank(po.getUpdateTime())){
            po.setUpdateTime(MrDateUtils.getCurrentTime());
        }
        return po;
    }

    public static TableRecordPO toRecordPO(TableEntity entity) {
        TableDataPO po = toPO(entity);
        if (po == null){
            return null;
        } else {
            TableRecordPO recordPO = new TableRecordPO();
            recordPO.setId(MrStringUtils.generateId(UuidTypEnum.TABLE_REC));
            recordPO.setGroupName(po.getGroupName());
            recordPO.setProjectName(po.getProjectName());
            recordPO.setAppName(po.getAppName());
            recordPO.setModuleName(po.getModuleName());
            recordPO.setTableNameCamel(po.getTableNameCamel());
            recordPO.setTableNameSnake(po.getTableNameSnake());
            recordPO.setTableCommentCn(po.getTableCommentCn());
            recordPO.setTableCommentEn(po.getTableCommentEn());
            recordPO.setFieldsJson(po.getFieldsJson());
            recordPO.setPrimaryKeyJson(po.getPrimaryKeyJson());
            recordPO.setIndexesJson(po.getIndexesJson());
            recordPO.setLableName(po.getLableName());
            recordPO.setAssociatEnum(po.getAssociatEnum());
            recordPO.setParentClass(po.getParentClass());
            recordPO.setGenerCdFlg(po.getGenerCdFlg());
            recordPO.setCreateTabFlg(po.getCreateTabFlg());
            recordPO.setDefOrderBy(po.getDefOrderBy());
            recordPO.setUpdateBy(po.getUpdateBy());
            recordPO.setUpdateTime(po.getUpdateTime());
            if(StringUtils.isBlank(recordPO.getUpdateTime())){
                recordPO.setUpdateTime(MrDateUtils.getCurrentTime());
            }
            return recordPO;
        }
    }


    // JSON与对象转换工具
    public static List<RxField> jsonToFields(String json) {
        if (json == null) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, RxField.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private static String fieldsToJson(List<RxField> rxFields) {
        if (rxFields == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(rxFields);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static TableEntity.Index jsonToPrimaryKey(String json) {
        if (json == null) return null;
        try {
            return OBJECT_MAPPER.readValue(json, TableEntity.Index.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String primaryKeyToJson(TableEntity.Index index) {
        if (index == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(index);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static List<TableEntity.Index> jsonToIndexes(String json) {
        if (json == null) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, TableEntity.Index.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private static String indexesToJson(List<TableEntity.Index> indexes) {
        if (indexes == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(indexes);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
