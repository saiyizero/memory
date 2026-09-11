package com.murong.ecp.tools.fx.infrastructure.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.infrastructure.license.model.LicenseInfo;
import com.murong.ecp.tools.fx.infrastructure.license.model.LicenseValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

/**
 * 许可证管理器
 * 负责许可证的验证、加密、解密和管理
 */
@Component
public class LicenseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LicenseManager.class);
    
    // 加密密钥 - 建议使用更复杂的密钥
    // AES要求密钥长度为16、24或32字节，这里使用32字节
    private static final String ENCRYPTION_KEY = "MurongLicense2024!@#Key123456789";
    private static final String LICENSE_FILE_NAME = "memory.license";
    private static final String LICENSE_PROP_FILE = "license.properties";
    
    private final ObjectMapper objectMapper;
    private LicenseInfo currentLicense;
    
    public LicenseManager() {
        this.objectMapper = new ObjectMapper();
        this.currentLicense = null;
    }
    
    /**
     * 验证许可证
     */
    public LicenseValidationResult validateLicense() {
        try {
            // 1. 检查许可证文件是否存在
            File licenseFile = getLicenseFile();
            if (!licenseFile.exists()) {
                return LicenseValidationResult.failure("许可证文件不存在");
            }
            
            // 2. 读取并解密许可证
            String encryptedLicense = readLicenseFile(licenseFile);
            if (encryptedLicense == null || encryptedLicense.trim().isEmpty()) {
                return LicenseValidationResult.failure("许可证文件为空或损坏");
            }
            
            // 3. 解密许可证
            String decryptedLicense = decryptLicense(encryptedLicense);
            if (decryptedLicense == null) {
                return LicenseValidationResult.failure("许可证解密失败");
            }
            
            // 4. 解析许可证信息
            LicenseInfo licenseInfo = parseLicense(decryptedLicense);
            if (licenseInfo == null) {
                return LicenseValidationResult.failure("许可证格式错误");
            }
            
            // 5. 验证许可证签名
            if (!validateLicenseSignature(licenseInfo)) {
                return LicenseValidationResult.failure("许可证签名验证失败");
            }
            
            // 6. 检查过期时间
            if (isLicenseExpired(licenseInfo)) {
                return LicenseValidationResult.failure("许可证已过期");
            }
            
            // 7. 验证版本号（强制版本升级）
            if (!validateVersion(licenseInfo)) {
                return LicenseValidationResult.failure("许可证版本过低，需要升级到版本 " + licenseInfo.getMinVersion());
            }
            
            this.currentLicense = licenseInfo;
            return LicenseValidationResult.success(licenseInfo);
            
        } catch (Exception e) {
            logger.error("许可证验证失败", e);
            return LicenseValidationResult.failure("许可证验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前应用版本
     */
    public String getCurrentVersion() {
        // 从pom.xml或配置文件中获取当前版本
        return "2.0.3"; // 这里应该从配置文件读取
    }
    
    /**
     * 安装许可证
     */
    public boolean installLicense(String licenseContent) {
        try {
            // 1. 解密许可证
            String decryptedLicense = decryptLicense(licenseContent);
            if (decryptedLicense == null) {
                return false;
            }
            
            // 2. 解析许可证
            LicenseInfo licenseInfo = parseLicense(decryptedLicense);
            if (licenseInfo == null) {
                return false;
            }
            
            // 3. 验证许可证
            LicenseValidationResult result = validateLicense();
            if (!result.isValid()) {
                return false;
            }
            
            // 4. 保存许可证文件
            File licenseFile = getLicenseFile();
            try (FileOutputStream fos = new FileOutputStream(licenseFile)) {
                fos.write(licenseContent.getBytes(StandardCharsets.UTF_8));
            }
            
            this.currentLicense = licenseInfo;
            return true;
            
        } catch (Exception e) {
            logger.error("安装许可证失败", e);
            return false;
        }
    }
    
    /**
     * 获取当前许可证信息
     */
    public LicenseInfo getCurrentLicense() {
        return currentLicense;
    }
    
    /**
     * 检查许可证是否有效
     */
    public boolean isLicenseValid() {
        if (currentLicense == null) {
            return false;
        }
        
        LicenseValidationResult result = validateLicense();
        return result.isValid();
    }
    
    /**
     * 获取许可证文件路径
     */
    private File getLicenseFile() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, LICENSE_FILE_NAME);
    }
    
    /**
     * 读取许可证文件
     */
    private String readLicenseFile(File licenseFile) {
        try (FileInputStream fis = new FileInputStream(licenseFile)) {
            byte[] content = new byte[(int) licenseFile.length()];
            fis.read(content);
            String result = new String(content, StandardCharsets.UTF_8);
            // 移除可能的换行符，确保Base64解码正确
            return result.trim();
        } catch (Exception e) {
            logger.error("读取许可证文件失败", e);
            return null;
        }
    }
    
    /**
     * 解密许可证
     */
    private String decryptLicense(String encryptedLicense) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedLicense));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("解密许可证失败", e);
            return null;
        }
    }
    
    /**
     * 解析许可证
     */
    private LicenseInfo parseLicense(String licenseContent) {
        try {
            return objectMapper.readValue(licenseContent, LicenseInfo.class);
        } catch (Exception e) {
            logger.error("解析许可证失败", e);
            return null;
        }
    }
    
    /**
     * 验证许可证签名
     */
    private boolean validateLicenseSignature(LicenseInfo licenseInfo) {
        try {
            String dataToSign = licenseInfo.getCompanyName() + "|" + 
                               licenseInfo.getProductName() + "|" + 
                               licenseInfo.getMinVersion() + "|" + 
                               licenseInfo.getMaxVersion() + "|" + 
                               licenseInfo.getExpireDate();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            return expectedSignature.equals(licenseInfo.getSignature());
        } catch (Exception e) {
            logger.error("验证许可证签名失败", e);
            return false;
        }
    }
    
    /**
     * 验证版本号
     */
    private boolean validateVersion(LicenseInfo licenseInfo) {
        try {
            String currentVersion = getCurrentVersion();
            String minVersion = licenseInfo.getMinVersion();
            String maxVersion = licenseInfo.getMaxVersion();
            
            // 检查当前版本是否在允许的版本范围内
            if (minVersion != null && !isVersionGreaterOrEqual(currentVersion, minVersion)) {
                logger.warn("当前版本 {} 低于许可证要求的最低版本 {}", currentVersion, minVersion);
                return false;
            }
            
            if (maxVersion != null && !isVersionLessOrEqual(currentVersion, maxVersion)) {
                logger.warn("当前版本 {} 高于许可证允许的最高版本 {}", currentVersion, maxVersion);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("验证版本号失败", e);
            return false;
        }
    }
    
    /**
     * 比较版本号，检查version1是否大于等于version2
     */
    private boolean isVersionGreaterOrEqual(String version1, String version2) {
        return compareVersions(version1, version2) >= 0;
    }
    
    /**
     * 比较版本号，检查version1是否小于等于version2
     */
    private boolean isVersionLessOrEqual(String version1, String version2) {
        return compareVersions(version1, version2) <= 0;
    }
    
    /**
     * 比较两个版本号
     * 返回: 1 (version1 > version2), 0 (version1 = version2), -1 (version1 < version2)
     */
    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part > v2Part) {
                return 1;
            } else if (v1Part < v2Part) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * 检查许可证是否过期
     */
    private boolean isLicenseExpired(LicenseInfo licenseInfo) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime expireDate = LocalDateTime.parse(licenseInfo.getExpireDate(), formatter);
            LocalDateTime now = LocalDateTime.now();
            
            return now.isAfter(expireDate);
        } catch (Exception e) {
            logger.error("检查许可证过期时间失败", e);
            return true;
        }
    }
    

} 