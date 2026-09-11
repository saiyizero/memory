package com.murong.ecp.tools.fx.domain.service.different;

import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.enums.IndexTypEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DDL生成服务
 */
@Service
public class DdlGenerateService {

    @Autowired
    private Configuration freemarkerConfig;

    /**
     * 生成PostgreSQL建表语句
     * 
     * @param tableEntity 表实体
     * @return DDL建表语句
     */
    public String generatePostgresqlDdl(TableEntity tableEntity) {
        try {
            // 确保表实体不为空
            if (tableEntity == null) {
                throw new RuntimeException("表实体不能为空");
            }
            
            // 确保字段列表不为空
            if (tableEntity.getRxFields() == null || tableEntity.getRxFields().isEmpty()) {
                throw new RuntimeException("表字段不能为空");
            }
            
            // 确保主键有名称
            if (tableEntity.getPrimaryKey() != null && tableEntity.getPrimaryKey().getName() == null) {
                tableEntity.getPrimaryKey().setName("pk_" + tableEntity.getTableNameSnake());
            }
            
            Template template = freemarkerConfig.getTemplate("table-ddl-postgresql.ftl");
            
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("table", tableEntity);
            
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            
            // 清理多余的空行，确保DDL格式紧凑
            String ddl = out.toString();
            return cleanDdlFormat(ddl);
        } catch (Exception e) {
            throw new RuntimeException("生成PostgreSQL DDL失败", e);
        }
    }

    /**
     * 标准化字段顺序，确保两边的字段顺序一致
     * 
     * @param tableEntity 表实体
     * @return 标准化后的表实体
     */
    public TableEntity standardizeFieldOrder(TableEntity tableEntity) {
        if (tableEntity == null || tableEntity.getRxFields() == null) {
            return tableEntity;
        }
        
        // 按照字段名排序，确保顺序一致
        tableEntity.getRxFields().sort((f1, f2) -> {
            String name1 = f1.getNameSnake() != null ? f1.getNameSnake() : "";
            String name2 = f2.getNameSnake() != null ? f2.getNameSnake() : "";
            return name1.compareTo(name2);
        });
        
        return tableEntity;
    }
    
    /**
     * 清理DDL格式，移除多余的空行
     */
    private String cleanDdlFormat(String ddl) {
        if (ddl == null || ddl.isEmpty()) {
            return ddl;
        }
        
        // 移除所有前导和尾随空白字符
        String trimmed = ddl.trim();
        
        // 按行分割并重新构建，确保没有多余的空行
        String[] lines = trimmed.split("\n");
        StringBuilder cleaned = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }
            
            // 添加非空行
            if (cleaned.length() > 0) {
                cleaned.append("\n");
            }
            cleaned.append(line);
        }
        
        return cleaned.toString();
    }
} 