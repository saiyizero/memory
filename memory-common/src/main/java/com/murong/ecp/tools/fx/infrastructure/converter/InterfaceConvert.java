package com.murong.ecp.tools.fx.infrastructure.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class InterfaceConvert {
    static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * PO转Entity
     */
    public static InterFaceEntity toEntity(InterfaceDataPO po) {
        if (po == null) return null;
        InterFaceEntity entity = new InterFaceEntity();
        entity.setInterfaceName(po.getInterfaceName());
        entity.setTransName(po.getTransName());
        entity.setClassName(po.getClassName());
        entity.setModuleName(po.getModuleName());
        entity.setTransCommentZh(po.getTransCommentZh());
        entity.setTransCommentEn(po.getTransCommentEn());
        entity.setTransClass(po.getTransClass());
        entity.setSimpleName(po.getSimpleName());
        entity.setRequest(jsonToFields(po.getRequestJson()));
        entity.setResponse(jsonToFields(po.getResponseJson()));
        entity.setLableName(po.getLableName());
        entity.setAssociatEntity(po.getAssociatEntity());
        entity.setAssociatEnum(po.getAssociatEnum());
        entity.setReqParentClass(po.getReqParentClass());
        entity.setRspParentClass(po.getRspParentClass());
        InterFaceEntity.Properties prop = new InterFaceEntity.Properties();
        prop.setRequestPackage(po.getRequestPackage());
        prop.setResponsePackage(po.getResponsePackage());
        prop.setRequestClass(po.getRequestClass());
        prop.setResponseClass(po.getResponseClass());
        prop.setInterfaceUrl(po.getInterfaceUrl());
        prop.setMethodUrl(po.getMethodUrl());
        prop.setUpdateBy(po.getUpdateBy());
        prop.setUpdateTime(po.getUpdateTime());
        entity.setProperties(prop);
        return entity;
    }

    /**
     * Entity转PO
     */
    public static InterfaceDataPO toPO(InterFaceEntity entity) {
        if (entity == null) return null;
        InterfaceDataPO po = new InterfaceDataPO();
        po.setInterfaceName(entity.getInterfaceName());
        po.setTransName(entity.getTransName());
        po.setClassName(entity.getClassName());
        po.setModuleName(entity.getModuleName());
        po.setTransCommentZh(entity.getTransCommentZh());
        po.setTransCommentEn(entity.getTransCommentEn());
        po.setTransClass(entity.getTransClass());
        po.setSimpleName(entity.getSimpleName());
        po.setRequestJson(fieldsToJson(entity.getRequest()));
        po.setResponseJson(fieldsToJson(entity.getResponse()));
        po.setLableName(entity.getLableName());
        po.setAssociatEntity(entity.getAssociatEntity());
        po.setAssociatEnum(entity.getAssociatEnum());
        po.setReqParentClass(entity.getReqParentClass());
        po.setRspParentClass(entity.getRspParentClass());
        if (entity.getProperties() != null) {
            InterFaceEntity.Properties prop = entity.getProperties();
            po.setRequestPackage(prop.getRequestPackage());
            po.setResponsePackage(prop.getResponsePackage());
            po.setRequestClass(prop.getRequestClass());
            po.setResponseClass(prop.getResponseClass());
            po.setInterfaceUrl(prop.getInterfaceUrl());
            po.setMethodUrl(prop.getMethodUrl());
            po.setUpdateBy(prop.getUpdateBy());
            po.setUpdateTime(prop.getUpdateTime());
        }
        return po;
    }

    /**
     * JSON转List<Field>
     */
    public static List<RxField> jsonToFields(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, RxField.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * List<Field>转JSON
     */
    public static String fieldsToJson(List<RxField> rxFields) {
        if (rxFields == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(rxFields);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
} 