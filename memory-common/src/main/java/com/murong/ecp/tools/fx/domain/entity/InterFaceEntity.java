package com.murong.ecp.tools.fx.domain.entity;

import lombok.Data;

import java.util.List;

/**
 * 由transaction-inputs.json生成，存储交易服务定义的实体类
 */
@Data
public class InterFaceEntity {

    @Data
    public static class Properties {
        private String updateBy;
        private String updateTime;
        private String actionPackage;
        private String controllerPackage;
        private String interfacePackage;
        private String requestPackage;
        private String responsePackage;
        private String requestClass;
        private String responseClass;
        private String interfaceUrl;
        private String methodUrl;
    }

    private String interfaceName;
    private String transName;
    private String className;
    private String moduleName;
    private String transCommentZh;
    private String transCommentEn;
    private String transClass;
    private String simpleName;
    private List<RxField> request;
    private List<RxField> response;
    private Properties properties;
    private String lableName;
    private String associatEntity;
    private String associatEnum;
    private String reqParentClass;
    private String rspParentClass;

    public void setInterfaceUrl(String url) {
        if (this.properties == null) {
            this.properties = new Properties();
        }
        this.properties.setInterfaceUrl(url);
    }

    public void setMethodUrl(String url) {
        if (this.properties == null) {
            this.properties = new Properties();
        }
        this.properties.setMethodUrl(url);
    }
} 