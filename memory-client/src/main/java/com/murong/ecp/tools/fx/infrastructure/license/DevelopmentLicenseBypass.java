package com.murong.ecp.tools.fx.infrastructure.license;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 开发模式许可证绕过
 * 在开发环境中可以跳过许可证验证
 */
@Component
public class DevelopmentLicenseBypass {
    
    @Value("${license.development.mode:false}")
    private boolean developmentMode;
    
    @Value("${license.bypass.enabled:false}")
    private boolean bypassEnabled;
    
    /**
     * 检查是否为开发模式
     */
    public boolean isDevelopmentMode() {
        return developmentMode || bypassEnabled;
    }
    
    /**
     * 检查是否启用绕过
     */
    public boolean isBypassEnabled() {
        return bypassEnabled;
    }
    
    /**
     * 设置开发模式
     */
    public void setDevelopmentMode(boolean developmentMode) {
        this.developmentMode = developmentMode;
    }
    
    /**
     * 设置绕过启用
     */
    public void setBypassEnabled(boolean bypassEnabled) {
        this.bypassEnabled = bypassEnabled;
    }
} 