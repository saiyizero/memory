#!/bin/bash

# 许可证生成脚本
# 用法: ./generate-license.sh <公司名> <产品名> <版本> <最低版本> <最高版本> <过期天数> [功能特性]

if [ $# -lt 6 ]; then
    echo "用法: $0 <公司名> <产品名> <版本> <最低版本> <最高版本> <过期天数> [功能特性]"
    echo "示例: $0 \"测试公司\" \"Memory工具\" \"2.0.3\" \"2.0.0\" \"3.0.0\" 365 \"高级功能\""
    exit 1
fi

COMPANY_NAME="$1"
PRODUCT_NAME="$2"
VERSION="$3"
MIN_VERSION="$4"
MAX_VERSION="$5"
EXPIRE_DAYS="$6"
FEATURES="${7:-标准功能}"

echo "正在生成许可证..."
echo "公司名称: $COMPANY_NAME"
echo "产品名称: $PRODUCT_NAME"
echo "版本: $VERSION"
echo "最低版本: $MIN_VERSION"
echo "最高版本: $MAX_VERSION"
echo "过期天数: $EXPIRE_DAYS"
echo "功能特性: $FEATURES"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 需要Java环境来生成许可证"
    exit 1
fi

# 创建临时Java文件来生成许可证
cat > /tmp/LicenseGenerator.java << 'EOF'
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileWriter;
import java.io.File;

public class LicenseGenerator {
    private static final String ENCRYPTION_KEY = "MurongLicense2024!@#Key123456789";
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("参数不足");
            return;
        }
        
        String companyName = args[0];
        String productName = args[1];
        String version = args[2];
        String minVersion = args[3];
        String maxVersion = args[4];
        int expireDays = Integer.parseInt(args[5]);
        String features = args.length > 6 ? args[6] : "标准功能";
        
        try {
            // 计算过期时间
            LocalDateTime expireDate = LocalDateTime.now().plusDays(expireDays);
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            // 创建许可证JSON
            String licenseJson = String.format(
                "{\"companyName\":\"%s\",\"productName\":\"%s\",\"version\":\"%s\"," +
                "\"minVersion\":\"%s\",\"maxVersion\":\"%s\",\"expireDate\":\"%s\"," +
                "\"signature\":\"%s\",\"features\":\"%s\",\"issuedDate\":\"%s\"}",
                companyName, productName, version, minVersion, maxVersion,
                expireDate.format(formatter), "", features, now.format(formatter)
            );
            
            // 生成签名
            String dataToSign = companyName + "|" + productName + "|" + minVersion + "|" + maxVersion + "|" + expireDate.format(formatter);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hash);
            
            // 替换签名
            licenseJson = licenseJson.replace("\"signature\":\"\"", "\"signature\":\"" + signature + "\"");
            
            // 加密许可证
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(licenseJson.getBytes(StandardCharsets.UTF_8));
            String encryptedLicense = Base64.getEncoder().encodeToString(encryptedBytes);
            
            // 输出许可证
            System.out.println(encryptedLicense);
            
        } catch (Exception e) {
            System.err.println("生成许可证失败: " + e.getMessage());
            System.exit(1);
        }
    }
}
EOF

# 编译临时Java文件
echo "编译许可证生成器..."
javac /tmp/LicenseGenerator.java

if [ $? -ne 0 ]; then
    echo "编译失败，请检查Java环境"
    rm -f /tmp/LicenseGenerator.java /tmp/LicenseGenerator.class
    exit 1
fi

# 生成许可证
echo "生成许可证..."
LICENSE_CONTENT=$(java -cp /tmp LicenseGenerator "$COMPANY_NAME" "$PRODUCT_NAME" "$VERSION" "$MIN_VERSION" "$MAX_VERSION" "$EXPIRE_DAYS" "$FEATURES")

if [ $? -eq 0 ] && [ -n "$LICENSE_CONTENT" ]; then
    echo "许可证生成成功！"
    
    # 保存到当前目录
    echo "$LICENSE_CONTENT" > generated_license.txt
    echo "许可证文件已保存为: generated_license.txt"
    
    # 自动复制到应用期望的位置
    echo "$LICENSE_CONTENT" > ~/memory.license
    echo "许可证文件已自动安装到: ~/memory.license"
    
    # 清理临时文件
    rm -f /tmp/LicenseGenerator.java /tmp/LicenseGenerator.class
else
    echo "许可证生成失败！"
    rm -f /tmp/LicenseGenerator.java /tmp/LicenseGenerator.class
    exit 1
fi 