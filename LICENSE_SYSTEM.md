# Memory工具许可证保护系统

## 🎯 系统概述

为Memory工具实现的完整许可证保护系统，支持版本控制和过期时间管理，便于强制版本升级和程序推广。

## ✅ 核心功能

### 1. 版本控制
- **最低版本**：许可证支持的最低应用版本
- **最高版本**：许可证支持的最高应用版本
- **强制升级**：通过设置版本范围强制用户升级

### 2. 时间控制
- **过期时间**：许可证的有效期
- **精确控制**：支持年、月、日、时、分、秒级别控制

### 3. 安全特性
- **AES-256加密**：许可证文件完全加密
- **SHA-256签名**：防止许可证被篡改
- **启动验证**：应用启动时强制验证

## 🔧 核心文件

### 1. 许可证生成器
- **文件**：`LicenseGeneratorStandalone.java`
- **功能**：生成加密的许可证文件
- **特点**：独立运行，不依赖Maven

### 2. 许可证生成脚本
- **文件**：`generate-license.sh`
- **功能**：命令行许可证生成工具
- **用法**：`./generate-license.sh "公司名" "产品名" "版本" "最低版本" "最高版本" "过期天数" "功能特性"`

### 3. 许可证管理器
- **文件**：`memory-client/src/main/java/com/murong/ecp/tools/fx/infrastructure/license/LicenseManager.java`
- **功能**：许可证验证、加密、解密

### 4. 许可证验证器
- **文件**：`memory-client/src/main/java/com/murong/ecp/tools/fx/infrastructure/license/LicenseValidator.java`
- **功能**：应用启动时验证许可证

## 🚀 使用方法

### 1. 生成许可证

```bash
# 编译生成器
javac LicenseGeneratorStandalone.java

# 生成许可证（支持2.0.3版本，有效期365天）
./generate-license.sh "沐融" "memory" "2.0.3" "2.0.3" "2.0.3" 7 "功能特性"

# 生成许可证（支持2.0.0到3.0.0版本范围）
./generate-license.sh "沐融" "memory" "2.0.3" "2.0.0" "3.0.0" 365 "高级功能"
```

### 2. 安装许可证

许可证文件会自动保存到：
- 当前目录：`generated_license.txt`
- 应用目录：`~/memory.license`

### 3. 验证许可证

应用启动时会自动验证许可证：
1. 检查许可证文件是否存在
2. 解密许可证内容
3. 验证数字签名
4. 检查过期时间
5. 验证版本范围

## 📋 使用场景

### 1. 强制版本升级
```bash
# 只支持最新版本，强制用户升级
./generate-license.sh "公司名" "产品名" "2.0.3" "2.0.3" "2.0.3" 365 "功能特性"
```

### 2. 版本范围支持
```bash
# 支持2.0.0到3.0.0版本范围
./generate-license.sh "公司名" "产品名" "2.0.3" "2.0.0" "3.0.0" 365 "功能特性"
```

### 3. 试用版许可证
```bash
# 短期试用，只支持当前版本
./generate-license.sh "公司名" "产品名" "2.0.3" "2.0.3" "2.0.3" 30 "试用功能"
```

### 4. 企业版许可证
```bash
# 长期使用，支持较宽版本范围
./generate-license.sh "公司名" "产品名" "2.0.3" "2.0.0" "4.0.0" 1095 "企业功能"
```

## 🔒 许可证文件格式

生成的许可证文件是AES加密的Base64编码字符串，包含以下信息：

```json
{
  "companyName": "公司名称",
  "productName": "产品名称",
  "version": "版本号",
  "minVersion": "最低支持版本",
  "maxVersion": "最高支持版本",
  "expireDate": "过期时间",
  "signature": "数字签名",
  "features": "功能特性",
  "issuedDate": "签发时间"
}
```

## ⚙️ 配置说明

### 1. 应用配置
在 `memory-client/src/main/resources/application.properties` 中：

```properties
# 许可证配置
license.development.mode=false
license.bypass.enabled=false
license.enabled=true

# 许可证相关日志级别
logging.level.com.murong.ecp.tools.fx.infrastructure.license=DEBUG
```

### 2. 开发模式
在开发环境中可以跳过许可证验证：

```properties
license.development.mode=true
license.bypass.enabled=false
```

## 🔍 验证流程

1. **文件检查**：验证许可证文件是否存在
2. **解密验证**：使用AES密钥解密许可证
3. **格式验证**：检查JSON格式是否正确
4. **签名验证**：验证数字签名防止篡改
5. **过期检查**：检查许可证是否过期
6. **版本验证**：检查当前版本是否在允许范围内

## ❌ 错误处理

### 常见错误及解决方案

1. **许可证文件不存在**
   - 检查文件路径：`~/memory.license`
   - 确认文件名正确

2. **许可证解密失败**
   - 检查加密密钥是否一致
   - 确认许可证文件未损坏

3. **签名验证失败**
   - 许可证可能被篡改
   - 重新生成许可证

4. **许可证已过期**
   - 联系供应商获取新许可证
   - 或延长现有许可证有效期

5. **版本不匹配**
   - 升级应用到支持版本
   - 或获取支持当前版本的许可证

## 🎯 核心优势

1. **版本控制** - 不绑定机器码，便于程序推广
2. **强制升级** - 通过版本范围控制强制用户升级
3. **安全可靠** - 多重加密和签名保护
4. **易于使用** - 简单的命令行工具
5. **灵活配置** - 支持多种许可证策略

## 📁 文件结构

```
memory/
├── 核心工具/
│   ├── LicenseGeneratorStandalone.java    # 许可证生成器
│   └── generate-license.sh                # 生成脚本
├── 文档/
│   └── LICENSE_SYSTEM.md                  # 本文件
└── memory-client/
    └── src/main/java/com/murong/ecp/tools/fx/infrastructure/license/
        ├── LicenseManager.java            # 许可证管理器
        ├── LicenseValidator.java          # 许可证验证器
        ├── LicenseGenerator.java          # 许可证生成器
        ├── SimpleLicenseGenerator.java    # 简化许可证生成器
        ├── DevelopmentLicenseBypass.java  # 开发模式绕过
        └── model/
            ├── LicenseInfo.java           # 许可证信息模型
            └── LicenseValidationResult.java # 验证结果模型
```

## 🔧 技术实现

### 1. 加密算法
- **AES-256**：许可证文件加密
- **SHA-256**：数字签名生成
- **Base64**：编码传输

### 2. 版本比较
```java
private int compareVersions(String version1, String version2) {
    String[] v1Parts = version1.split("\\.");
    String[] v2Parts = version2.split("\\.");
    
    int maxLength = Math.max(v1Parts.length, v2Parts.length);
    
    for (int i = 0; i < maxLength; i++) {
        int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
        int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
        
        if (v1Part > v2Part) return 1;
        else if (v1Part < v2Part) return -1;
    }
    
    return 0;
}
```

### 3. 签名生成
```java
String dataToSign = companyName + "|" + productName + "|" + 
                   minVersion + "|" + maxVersion + "|" + expireDate;
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(dataToSign.getBytes(StandardCharsets.UTF_8));
String signature = Base64.getEncoder().encodeToString(hash);
```

## 🚨 安全建议

1. **密钥管理** - 生产环境应使用更安全的密钥管理方式
2. **定期更新** - 定期更新加密算法和密钥
3. **备份重要** - 用户应备份许可证文件
4. **网络验证** - 考虑添加在线验证功能

## 📞 技术支持

如有问题，请检查：
1. 许可证文件是否正确安装到 `~/memory.license`
2. 应用配置是否正确
3. 许可证是否过期或版本不匹配

---

**总结**：本许可证系统已成功实现版本控制和过期时间管理，可以有效保护软件并强制版本升级。 