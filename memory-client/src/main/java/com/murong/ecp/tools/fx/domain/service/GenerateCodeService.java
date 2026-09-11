package com.murong.ecp.tools.fx.domain.service;


import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class GenerateCodeService {
    private static final Logger log = LoggerFactory.getLogger(GenerateCodeService.class);
    @Autowired
    GlobalProperties globalProperties;
    @Autowired
    Configuration cfg;

    @SneakyThrows
    public void generTableCode(TableEntity entity) {
        if (entity == null) {
            log.error("TableEntity 为空，无法生成代码");
            return;
        }
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("table", entity);

        String basePath = globalProperties.getBasePath();
        if (basePath == null || basePath.isEmpty()) {
            log.error("basePath 为空");
            return;
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }

        String classMiddlePath = globalProperties.getMainModuleDb().getBasePath();
        if (classMiddlePath == null || classMiddlePath.isEmpty()) {
            log.error("classMiddlePath 为空");
            return;
        }
        if (!classMiddlePath.endsWith("/")) {
            classMiddlePath = classMiddlePath + "/";
        }

        String resourcesMiddlePath = globalProperties.getMainModuleDb().getBasePath().replace("/java","/resources");
        if (resourcesMiddlePath == null || resourcesMiddlePath.isEmpty()) {
            log.error("resourcesMiddlePath 为空");
            return;
        }
        if (!resourcesMiddlePath.endsWith("/")) {
            resourcesMiddlePath = resourcesMiddlePath + "/";
        }

        entity.getProperties().setEntityPath(globalProperties.getMainModuleDb().getEntity());
        entity.getProperties().setMapperPath(globalProperties.getMainModuleDb().getMapper());
        entity.getProperties().setXmlPath(globalProperties.getMainModuleDb().getXml().replaceAll("\\.", "/"));

        TableEntity.Properties prop = entity.getProperties();
        String entityPath = prop.getEntityPath() == null ? "" : prop.getEntityPath().replaceAll("\\.", "/");
        String mapperPath = prop.getMapperPath() == null ? "" : prop.getMapperPath().replaceAll("\\.", "/");
        String xmlPath = prop.getXmlPath() == null ? "" : prop.getXmlPath().replaceAll("\\.", "/");
        String tableName = entity.getTableNameCamel();

        String baseJavaPath = "src/main/java/";
        String baseResources = "src/main/resources/";
        generateFile(cfg, "table-entity.ftl", dataModel, basePath+classMiddlePath+baseJavaPath+entityPath + "/" + tableName + "PO.java");
        generateFile(cfg, "table-mapper.ftl", dataModel, basePath+classMiddlePath+baseJavaPath+mapperPath + "/" + tableName + "Mapper.java");
        generateFile(cfg, "table-mapper-xml.ftl", dataModel, basePath+resourcesMiddlePath+baseResources+xmlPath + "/"+ tableName + "Mapper.xml");

        File expMappFile = new File(basePath+classMiddlePath+baseJavaPath+mapperPath + "/" + tableName + "ExtMapper.java");
        if(!expMappFile.exists()){
            generateFile(cfg, "table-exp-mapper.ftl", dataModel, basePath+classMiddlePath+baseJavaPath+mapperPath + "/" + tableName + "ExtMapper.java");
        }
        File expXmlFile = new File(basePath+resourcesMiddlePath+baseResources+xmlPath + "/"+ tableName + "ExtMapper.xml");
        if(!expXmlFile.exists()){
            generateFile(cfg, "table-exp-mapper-xml.ftl", dataModel, basePath+resourcesMiddlePath+baseResources+xmlPath + "/"+ tableName + "ExtMapper.xml");
        }
    }

    private void generateFile(Configuration cfg, String templateName, Map<String, Object> dataModel, String outputPath) throws IOException, TemplateException {
        Template template = cfg.getTemplate(templateName);
        File outFile = new File(outputPath);
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
    }
}
