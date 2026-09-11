package com.murong.ecp.tools.fx.infrastructure.license.model;

/**
 * 许可证验证结果
 */
public class LicenseValidationResult {
    
    private final boolean valid;
    private final String message;
    private final LicenseInfo licenseInfo;
    
    private LicenseValidationResult(boolean valid, String message, LicenseInfo licenseInfo) {
        this.valid = valid;
        this.message = message;
        this.licenseInfo = licenseInfo;
    }
    
    /**
     * 创建成功结果
     */
    public static LicenseValidationResult success(LicenseInfo licenseInfo) {
        return new LicenseValidationResult(true, "许可证验证成功", licenseInfo);
    }
    
    /**
     * 创建失败结果
     */
    public static LicenseValidationResult failure(String message) {
        return new LicenseValidationResult(false, message, null);
    }
    
    /**
     * 检查是否有效
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 获取验证消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取许可证信息
     */
    public LicenseInfo getLicenseInfo() {
        return licenseInfo;
    }
    
    @Override
    public String toString() {
        return "LicenseValidationResult{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                ", licenseInfo=" + licenseInfo +
                '}';
    }
} 