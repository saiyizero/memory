package com.murong.ecp.tools.fx.domain.entity;

import lombok.Data;
import org.springframework.util.CollectionUtils;
import com.murong.ecp.tools.fx.enums.IndexTypEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 由table-inputs.json生成，存储表结构定义的实体类
 */
@Data
public class TableEntity {

    public void addField(String nameCamel,String nameSnake,String type,String dbTyp,Integer length,String commentCn,String commentEn){
        if(CollectionUtils.isEmpty(this.rxFields)){
            this.rxFields =new ArrayList<RxField>();
        }
        RxField rxField = new RxField();
        rxField.setNameCamel(nameCamel);
        rxField.setNameSnake(nameSnake);
        rxField.setType(type);
        rxField.setDbTyp(dbTyp);
        
        // 对于text类型，不设置长度，避免对比差异
        if (dbTyp != null) {
            String upperDbType = dbTyp.toUpperCase();
            if ("TEXT".equals(upperDbType) || "LONGTEXT".equals(upperDbType) || "MEDIUMTEXT".equals(upperDbType) || "TINYTEXT".equals(upperDbType)) {
                // text类型不设置长度
                rxField.setLength(null);
            } else {
                rxField.setLength(length);
            }
        } else {
            rxField.setLength(length);
        }
        
        rxField.setCommentCn(commentCn);
        rxField.setCommentEn(commentEn);
        this.rxFields.add(rxField);
    }

    @Data
    public static class Index {
        private String name;
        private String type;
        private List<String> fields;
        
        /**
         * 设置索引类型（使用枚举）
         */
        public void setIndexType(IndexTypEnum indexType) {
            this.type = indexType != null ? indexType.getCode() : null;
        }

    }

    private String groupName;
    private String projectName;
    private String appName;
    private String moduleName;
    private String tableNameCamel;
    private String tableNameSnake;
    private String tableCommentCn;
    private String tableCommentEn;
    private List<RxField> rxFields;
    private Index primaryKey;
    private List<Index> indexes;
    private List<Index> uniqueIndexes;
    private String lableName;
    private String associatEnum;
    private String parentClass;
    private String generCdFlg;
    private String createTabFlg;
    private String defOrderBy;
    private String updateBy;
    private String updateTime;
    private Properties properties;
    private String schema;

    public Properties getProperties(){
        if(properties == null){
            properties=new Properties();
        }
        return properties;
    }

    @Data
    public static class Properties {
        private String entityPath;
        private String mapperPath;
        private String xmlPath;
    }
} 