package com.murong.ecp.tools.fx.domain.service;

import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.InterfaceDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GenerateDocService {
    @Autowired
    private InterfaceDataDao interfaceDataDao;
    @Autowired
    private EnumDictDao enumDictDao;
    @Autowired
    private Configuration freemarkerConfig;
    public String generateTransactionDoc(List<Map<String, String>> selectedTransNames) {

        // 1. 查询并组装List<InterFaceEntity>
        List<InterFaceEntity> entityList = selectedTransNames.stream()
                .map((mapSet) -> {
                    InterfaceDataPO query = new InterfaceDataPO();
                    query.setTransName(mapSet.get("transName"));
                    query.setInterfaceName(mapSet.get("interfaceName"));
                    InterfaceDataPO result = interfaceDataDao.queryOne(query);
                    if (result != null) {
                        return convertToEntity(result);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        if (entityList.isEmpty()) {
            return null;
        }
        // 2. 收集所有涉及的枚举
        List<String> enumKeys = new ArrayList<>();
        for (InterFaceEntity entity : entityList) {
            collectEnumKeys(entity.getRequest(), enumKeys);
            collectEnumKeys(entity.getResponse(), enumKeys);
        }
        // 去重并排序
        Set<String> uniqueEnumKeys = new TreeSet<>(enumKeys);
        Map<String, List<EnumDictPO>> enumGroupMap = new LinkedHashMap<>();
        for (String key : uniqueEnumKeys) {
            String[] arr = key.split("#", 2);
            if (arr.length == 2) {
                EnumDictPO query = new EnumDictPO();
                query.setEnumNme(arr[0]);
                query.setEnumRef(arr[1]);
                List<EnumDictPO> found = enumDictDao.queryForList(query);
                if (found != null && !found.isEmpty()) {
                    enumGroupMap.put(key, found);
                }
            }
        }
        // 3. 用transaction-markdown.ftl生成markdown
        try {
            java.io.StringWriter out = new java.io.StringWriter();
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("interfaceList", entityList);
            dataModel.put("enumGroupMap", enumGroupMap);
            Template template = freemarkerConfig.getTemplate("markdown/transaction-markdown.ftl");
            template.process(dataModel, out);
            return out.toString();
        } catch (Exception e) {
            return "生成文档失败: " + e.getMessage();
        }
    }

    // 递归收集所有字段涉及的枚举key（enumNme#enumRef）
    private void collectEnumKeys(List<RxField> rxFields, List<String> enumKeys) {
        if (rxFields == null) return;
        for (RxField f : rxFields) {
            if (f.getEnumNme() != null && !f.getEnumNme().isEmpty() && f.getEnumRef() != null && !f.getEnumRef().isEmpty()) {
                enumKeys.add(f.getEnumNme() + "#" + f.getEnumRef());
            }
            if (f.getChildren() != null && !f.getChildren().isEmpty()) {
                collectEnumKeys(f.getChildren(), enumKeys);
            }
        }
    }

    private InterFaceEntity convertToEntity(InterfaceDataPO po) {
        InterFaceEntity entity = new InterFaceEntity();
        entity.setInterfaceName(po.getInterfaceName());
        entity.setTransName(po.getTransName());
        entity.setClassName(po.getClassName());
        entity.setTransCommentZh(po.getTransCommentZh());
        if (StringUtils.isBlank(entity.getTransCommentZh())) {
            entity.setTransCommentZh("");
        }
        entity.setTransCommentEn(po.getTransCommentEn());
        if (StringUtils.isBlank(entity.getTransCommentEn())) {
            entity.setTransCommentEn("");
        }
        entity.setTransClass(po.getTransClass());
        entity.setSimpleName(po.getSimpleName());
        // requestJson/responseJson转List<Field>
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<RxField> req = new ArrayList<>();
            List<RxField> res = new ArrayList<>();
            if (po.getRequestJson() != null && !po.getRequestJson().isEmpty()) {
                req = mapper.readValue(po.getRequestJson(), mapper.getTypeFactory().constructCollectionType(List.class, RxField.class));
            }
            if (po.getResponseJson() != null && !po.getResponseJson().isEmpty()) {
                res = mapper.readValue(po.getResponseJson(), mapper.getTypeFactory().constructCollectionType(List.class, RxField.class));
            }
            entity.setRequest(req);
            entity.setResponse(res);
        } catch (Exception e) {
            entity.setRequest(new ArrayList<>());
            entity.setResponse(new ArrayList<>());
        }
        InterFaceEntity.Properties prop = new InterFaceEntity.Properties();
        prop.setUpdateBy(po.getUpdateBy() != null ? po.getUpdateBy() : "");
        prop.setUpdateTime(po.getUpdateTime() != null ? po.getUpdateTime() : "");
        prop.setRequestPackage(po.getRequestPackage() != null ? po.getRequestPackage() : "");
        prop.setResponsePackage(po.getResponsePackage() != null ? po.getResponsePackage() : "");
        prop.setRequestClass(po.getRequestClass() != null ? po.getRequestClass() : "");
        prop.setResponseClass(po.getResponseClass() != null ? po.getResponseClass() : "");
        prop.setInterfaceUrl(po.getInterfaceUrl() != null ? po.getInterfaceUrl() : "");
        prop.setMethodUrl(po.getMethodUrl() != null ? po.getMethodUrl() : "");
        entity.setProperties(prop);
        return entity;
    }
}
