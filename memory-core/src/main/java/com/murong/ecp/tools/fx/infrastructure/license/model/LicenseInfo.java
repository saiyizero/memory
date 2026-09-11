package com.murong.ecp.tools.fx.infrastructure.license.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 许可证信息模型
 */
public class LicenseInfo {
    
    @JsonProperty("companyName")
    private String companyName;
    
    @JsonProperty("productName")
    private String productName;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("minVersion")
    private String minVersion;
    
    @JsonProperty("maxVersion")
    private String maxVersion;
    
    @JsonProperty("expireDate")
    private String expireDate;
    
    @JsonProperty("signature")
    private String signature;
    
    @JsonProperty("features")
    private String features;
    
    @JsonProperty("issuedDate")
    private String issuedDate;
    
    public LicenseInfo() {}
    
    public LicenseInfo(String companyName, String productName, String version, 
                      String minVersion, String maxVersion, String expireDate, String signature, 
                      String features, String issuedDate) {
        this.companyName = companyName;
        this.productName = productName;
        this.version = version;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.expireDate = expireDate;
        this.signature = signature;
        this.features = features;
        this.issuedDate = issuedDate;
    }
    
    // Getters and Setters
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getMinVersion() {
        return minVersion;
    }
    
    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }
    
    public String getMaxVersion() {
        return maxVersion;
    }
    
    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }
    
    public String getExpireDate() {
        return expireDate;
    }
    
    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public String getFeatures() {
        return features;
    }
    
    public void setFeatures(String features) {
        this.features = features;
    }
    
    public String getIssuedDate() {
        return issuedDate;
    }
    
    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }
    
    @Override
    public String toString() {
        return "LicenseInfo{" +
                "companyName='" + companyName + '\'' +
                ", productName='" + productName + '\'' +
                ", version='" + version + '\'' +
                ", minVersion='" + minVersion + '\'' +
                ", maxVersion='" + maxVersion + '\'' +
                ", expireDate='" + expireDate + '\'' +
                ", features='" + features + '\'' +
                ", issuedDate='" + issuedDate + '\'' +
                '}';
    }
} 