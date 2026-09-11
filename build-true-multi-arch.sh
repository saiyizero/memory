#!/bin/bash

# Memory应用程序真正的多架构构建脚本
# 为每种架构创建只包含对应依赖的JAR文件

echo "=== Memory应用程序真正的多架构构建脚本 ==="
echo "系统信息:"
echo "  芯片架构: $(uname -m)"
echo "  Java版本: $(java -version 2>&1 | head -n 1)"
echo ""

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "21" ]; then
    echo "错误: 需要Java 21或更高版本，当前版本: $JAVA_VERSION"
    exit 1
fi

# 设置构建信息
BUILD_DATE=$(date +"%Y%m%d_%H%M%S")
BASE_DIST_DIR="target/true-multi-arch-build"

echo "构建信息:"
echo "  构建时间: $BUILD_DATE"
echo "  输出目录: $BASE_DIST_DIR"
echo ""

# 清理并编译
echo "1. 清理并编译项目..."
mvn clean compile -DskipTests --settings /Users/haoyulin/Application/maven/setting/cbp_dev.xml

# 构建memory-lib
echo "2. 构建memory-lib模块..."
cd memory-lib
mvn install -DskipTests --settings /Users/haoyulin/Application/maven/setting/cbp_dev.xml
cd ..

# 创建多架构分发包
echo "3. 创建多架构分发包..."
rm -rf "$BASE_DIST_DIR"
mkdir -p "$BASE_DIST_DIR"

# 为ARM64架构打包
echo "4. 为ARM64架构打包..."
ARM64_DIST_DIR="$BASE_DIST_DIR/arm64"
mkdir -p "$ARM64_DIST_DIR"

# 创建ARM64专用的pom.xml
echo "  创建ARM64专用配置..."
cat > "memory-core/pom-arm64.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.murong.ecp.tools</groupId>
        <artifactId>memory</artifactId>
        <version>2.0.3</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <groupId>com.murong.ecp.tools</groupId>
    <artifactId>memory-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- 依赖memory-lib模块 -->
        <dependency>
            <groupId>com.murong.ecp.tools</groupId>
            <artifactId>memory-lib</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
        
        <!-- Spring Boot启动器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-freemarker</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- JavaFX核心依赖 - 基础依赖（不包含平台特定库） -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
        </dependency>
        
        <!-- JavaFX平台特定依赖 - 仅ARM64 -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <classifier>mac-aarch64</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>mac-aarch64</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <classifier>mac-aarch64</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <classifier>mac-aarch64</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
            <classifier>mac-aarch64</classifier>
        </dependency>

        <dependency>
            <groupId>com.jfoenix</groupId>
            <artifactId>jfoenix</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>dev.aga.sqlite</groupId>
            <artifactId>sqlite-jdbc</artifactId>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>

        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>com.murong</groupId>
            <artifactId>mudb-jdbc</artifactId>
        </dependency>

        <!-- JLine3 - 成熟的Java终端库 -->
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-terminal</artifactId>
            <version>3.24.1</version>
        </dependency>
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-terminal-jansi</artifactId>
            <version>3.24.1</version>
        </dependency>
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-reader</artifactId>
            <version>3.24.1</version>
        </dependency>
        
        <!-- Lanterna - Java终端UI库 -->
        <dependency>
            <groupId>com.googlecode.lanterna</groupId>
            <artifactId>lanterna</artifactId>
            <version>3.1.1</version>
        </dependency>

        <!-- SSH相关依赖 -->
        <dependency>
            <groupId>com.hierynomus</groupId>
            <artifactId>sshj</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.10.0</version>
        </dependency>
        
        <!-- Excel处理依赖 -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml-schemas</artifactId>
        </dependency>
        
        <!-- 添加差异对比相关依赖 -->
        <dependency>
            <groupId>org.fxmisc.richtext</groupId>
            <artifactId>richtextfx</artifactId>
            <version>0.11.0</version>
        </dependency>
        <dependency>
            <groupId>org.fxmisc.flowless</groupId>
            <artifactId>flowless</artifactId>
            <version>0.7.2</version>
        </dependency>
        <dependency>
            <groupId>org.fxmisc.wellbehaved</groupId>
            <artifactId>wellbehavedfx</artifactId>
            <version>0.3.3</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.murong.ecp.tools.fx.MemoryApplication</mainClass>
                    <layout>JAR</layout>
                    <includeSystemScope>false</includeSystemScope>
                    <requiresUnpack>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-controls</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-fxml</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-graphics</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-base</artifactId>
                        </dependency>
                    </requiresUnpack>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# 使用ARM64专用配置打包
cd memory-core
mvn package -DskipTests -f pom-arm64.xml --settings /Users/haoyulin/Application/maven/setting/cbp_dev.xml
cd ..

# 复制ARM64版本文件
cp "memory-core/target/memory-core-0.0.1-SNAPSHOT.jar" "$ARM64_DIST_DIR/memory-core-arm64-0.0.1-SNAPSHOT.jar"
if [ -f "memory-core/src/main/resources/database/memory_embedded.db" ]; then
    cp "memory-core/src/main/resources/database/memory_embedded.db" "$ARM64_DIST_DIR/"
fi

# 创建ARM64启动脚本
cat > "$ARM64_DIST_DIR/run.sh" << 'EOF'
#!/bin/bash

# Memory应用程序ARM64启动脚本

JAR_FILE="memory-core-arm64-0.0.1-SNAPSHOT.jar"
DB_FILE="memory_embedded.db"

# 检查文件
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到JAR文件: $JAR_FILE"
    exit 1
fi

if [ ! -f "$DB_FILE" ]; then
    echo "错误: 找不到数据库文件: $DB_FILE"
    exit 1
fi

# 检测系统架构
ARCH=$(uname -m)
echo "检测到系统架构: $ARCH"

if [ "$ARCH" != "arm64" ]; then
    echo "错误: 此版本专为ARM64架构设计，当前架构: $ARCH"
    echo "请使用x86_64版本或通用版本"
    exit 1
fi

# 彻底清理JavaFX缓存
echo "清理JavaFX缓存..."
rm -rf "$HOME/.openjfx" 2>/dev/null
rm -rf "$HOME/.cache/openjfx" 2>/dev/null
rm -rf "$HOME/Library/Caches/openjfx" 2>/dev/null
rm -rf "$HOME/Library/Caches/org.openjfx" 2>/dev/null
rm -rf "/tmp/openjfx" 2>/dev/null
rm -rf "/var/folders/*/T/openjfx" 2>/dev/null

# 设置环境变量禁用JavaFX缓存
export JAVAFX_CACHE_DISABLE=true
export JAVAFX_DISABLE_CACHE=true

# 设置Java选项
JAVA_OPTS="-Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=classpath:/application.properties"
JAVA_OPTS="$JAVA_OPTS -Dtool.run.tmp.path=$HOME/foxi_tmp"
JAVA_OPTS="$JAVA_OPTS -Dspring.datasource.url=jdbc:sqlite:$DB_FILE"

# ARM64特定JavaFX选项
JAVA_OPTS="$JAVA_OPTS -Dprism.order=sw"
JAVA_OPTS="$JAVA_OPTS -Djavafx.platform=mac"
JAVA_OPTS="$JAVA_OPTS -Djava.library.path="
JAVA_OPTS="$JAVA_OPTS -Dprism.verbose=true"
JAVA_OPTS="$JAVA_OPTS -Djavafx.cache.disable=true"
JAVA_OPTS="$JAVA_OPTS -Djavafx.disable.cache=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.cache.disable=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.disable.cache=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.forceGPU=false"
JAVA_OPTS="$JAVA_OPTS -Dprism.allowSoftwareGL=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.allowHiDPIScaling=false"
JAVA_OPTS="$JAVA_OPTS -Dprism.text=t2k"
JAVA_OPTS="$JAVA_OPTS -Dprism.lcdtext=false"

# 创建临时目录
mkdir -p "$HOME/foxi_tmp"

echo "启动Memory应用程序 (ARM64专用版本)..."
echo "架构: $ARCH"
echo "JAR文件: $JAR_FILE"
echo "Java选项: $JAVA_OPTS"
java $JAVA_OPTS -jar "$JAR_FILE"
EOF

chmod +x "$ARM64_DIST_DIR/run.sh"

# 为x86_64架构打包
echo "5. 为x86_64架构打包..."
X86_64_DIST_DIR="$BASE_DIST_DIR/x86_64"
mkdir -p "$X86_64_DIST_DIR"

# 创建x86_64专用的pom.xml
echo "  创建x86_64专用配置..."
cat > "memory-core/pom-x86_64.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.murong.ecp.tools</groupId>
        <artifactId>memory</artifactId>
        <version>2.0.3</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <groupId>com.murong.ecp.tools</groupId>
    <artifactId>memory-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- 依赖memory-lib模块 -->
        <dependency>
            <groupId>com.murong.ecp.tools</groupId>
            <artifactId>memory-lib</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
        
        <!-- Spring Boot启动器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-freemarker</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- JavaFX核心依赖 - 基础依赖（不包含平台特定库） -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
        </dependency>
        
        <!-- JavaFX平台特定依赖 - 仅x86_64 -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <classifier>mac</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>mac</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <classifier>mac</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <classifier>mac</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
            <classifier>mac</classifier>
        </dependency>

        <dependency>
            <groupId>com.jfoenix</groupId>
            <artifactId>jfoenix</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>dev.aga.sqlite</groupId>
            <artifactId>sqlite-jdbc</artifactId>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>

        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>com.murong</groupId>
            <artifactId>mudb-jdbc</artifactId>
        </dependency>

        <!-- JLine3 - 成熟的Java终端库 -->
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-terminal</artifactId>
            <version>3.24.1</version>
        </dependency>
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-terminal-jansi</artifactId>
            <version>3.24.1</version>
        </dependency>
        <dependency>
            <groupId>org.jline</groupId>
            <artifactId>jline-reader</artifactId>
            <version>3.24.1</version>
        </dependency>
        
        <!-- Lanterna - Java终端UI库 -->
        <dependency>
            <groupId>com.googlecode.lanterna</groupId>
            <artifactId>lanterna</artifactId>
            <version>3.1.1</version>
        </dependency>

        <!-- SSH相关依赖 -->
        <dependency>
            <groupId>com.hierynomus</groupId>
            <artifactId>sshj</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.10.0</version>
        </dependency>
        
        <!-- Excel处理依赖 -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml-schemas</artifactId>
        </dependency>
        
        <!-- 添加差异对比相关依赖 -->
        <dependency>
            <groupId>org.fxmisc.richtext</groupId>
            <artifactId>richtextfx</artifactId>
            <version>0.11.0</version>
        </dependency>
        <dependency>
            <groupId>org.fxmisc.flowless</groupId>
            <artifactId>flowless</artifactId>
            <version>0.7.2</version>
        </dependency>
        <dependency>
            <groupId>org.fxmisc.wellbehaved</groupId>
            <artifactId>wellbehavedfx</artifactId>
            <version>0.3.3</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.murong.ecp.tools.fx.MemoryApplication</mainClass>
                    <layout>JAR</layout>
                    <includeSystemScope>false</includeSystemScope>
                    <requiresUnpack>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-controls</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-fxml</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-graphics</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.openjfx</groupId>
                            <artifactId>javafx-base</artifactId>
                        </dependency>
                    </requiresUnpack>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# 使用x86_64专用配置打包
cd memory-core
mvn package -DskipTests -f pom-x86_64.xml --settings /Users/haoyulin/Application/maven/setting/cbp_dev.xml
cd ..

# 复制x86_64版本文件
cp "memory-core/target/memory-core-0.0.1-SNAPSHOT.jar" "$X86_64_DIST_DIR/memory-core-x86_64-0.0.1-SNAPSHOT.jar"
if [ -f "memory-core/src/main/resources/database/memory_embedded.db" ]; then
    cp "memory-core/src/main/resources/database/memory_embedded.db" "$X86_64_DIST_DIR/"
fi

# 创建x86_64启动脚本
cat > "$X86_64_DIST_DIR/run.sh" << 'EOF'
#!/bin/bash

# Memory应用程序x86_64启动脚本

JAR_FILE="memory-core-x86_64-0.0.1-SNAPSHOT.jar"
DB_FILE="memory_embedded.db"

# 检查文件
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 找不到JAR文件: $JAR_FILE"
    exit 1
fi

if [ ! -f "$DB_FILE" ]; then
    echo "错误: 找不到数据库文件: $DB_FILE"
    exit 1
fi

# 检测系统架构
ARCH=$(uname -m)
echo "检测到系统架构: $ARCH"

if [ "$ARCH" != "x86_64" ]; then
    echo "错误: 此版本专为x86_64架构设计，当前架构: $ARCH"
    echo "请使用ARM64版本或通用版本"
    exit 1
fi

# 彻底清理JavaFX缓存
echo "清理JavaFX缓存..."
rm -rf "$HOME/.openjfx" 2>/dev/null
rm -rf "$HOME/.cache/openjfx" 2>/dev/null
rm -rf "$HOME/Library/Caches/openjfx" 2>/dev/null
rm -rf "$HOME/Library/Caches/org.openjfx" 2>/dev/null
rm -rf "/tmp/openjfx" 2>/dev/null
rm -rf "/var/folders/*/T/openjfx" 2>/dev/null

# 设置环境变量禁用JavaFX缓存
export JAVAFX_CACHE_DISABLE=true
export JAVAFX_DISABLE_CACHE=true

# 设置Java选项
JAVA_OPTS="-Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=classpath:/application.properties"
JAVA_OPTS="$JAVA_OPTS -Dtool.run.tmp.path=$HOME/foxi_tmp"
JAVA_OPTS="$JAVA_OPTS -Dspring.datasource.url=jdbc:sqlite:$DB_FILE"

# x86_64特定JavaFX选项
JAVA_OPTS="$JAVA_OPTS -Dprism.order=sw"
JAVA_OPTS="$JAVA_OPTS -Djavafx.platform=mac"
JAVA_OPTS="$JAVA_OPTS -Djava.library.path="
JAVA_OPTS="$JAVA_OPTS -Dprism.verbose=true"
JAVA_OPTS="$JAVA_OPTS -Djavafx.cache.disable=true"
JAVA_OPTS="$JAVA_OPTS -Djavafx.disable.cache=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.cache.disable=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.disable.cache=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.forceGPU=false"
JAVA_OPTS="$JAVA_OPTS -Dprism.allowSoftwareGL=true"
JAVA_OPTS="$JAVA_OPTS -Dprism.allowHiDPIScaling=false"
JAVA_OPTS="$JAVA_OPTS -Dprism.text=t2k"
JAVA_OPTS="$JAVA_OPTS -Dprism.lcdtext=false"

# 创建临时目录
mkdir -p "$HOME/foxi_tmp"

echo "启动Memory应用程序 (x86_64专用版本)..."
echo "架构: $ARCH"
echo "JAR文件: $JAR_FILE"
echo "Java选项: $JAVA_OPTS"
java $JAVA_OPTS -jar "$JAR_FILE"
EOF

chmod +x "$X86_64_DIST_DIR/run.sh"

# 清理临时pom文件
rm -f "memory-core/pom-arm64.xml"
rm -f "memory-core/pom-x86_64.xml"

# 创建README文件
cat > "$BASE_DIST_DIR/README.md" << 'EOF'
# Memory应用程序真正的多架构版本

本目录包含Memory应用程序的真正多架构版本：

## 目录结构

- `arm64/` - Apple Silicon Mac (M1/M2) 专用版本（仅包含ARM64依赖）
- `x86_64/` - Intel Mac 专用版本（仅包含x86_64依赖）

## 使用方法

### 1. ARM64版本（Apple Silicon Mac）
```bash
cd arm64
./run.sh
```

### 2. x86_64版本（Intel Mac）
```bash
cd x86_64
./run.sh
```

## 架构检测

要检测您的Mac架构，请运行：
```bash
uname -m
```

- `arm64` - Apple Silicon Mac (M1/M2)
- `x86_64` - Intel Mac

## 重要说明

- **ARM64版本**：只包含ARM64架构的JavaFX依赖，在Intel Mac上无法运行
- **x86_64版本**：只包含x86_64架构的JavaFX依赖，在Apple Silicon Mac上无法运行
- 每个版本的JAR文件大小会更小，因为只包含对应架构的依赖

## 故障排除

如果遇到架构不匹配错误，请：

1. 确认您的Mac架构：`uname -m`
2. 使用对应架构的版本
3. 如果仍有问题，使用通用版本：`./quick-build.sh`

## 注意事项

- 这些版本是真正的架构特定版本
- 在错误的架构上运行会直接报错并退出
- 建议在分发时同时提供两种版本
EOF

echo ""
echo "=== 真正的多架构构建完成 ==="
echo ""
echo "输出目录: $BASE_DIST_DIR"
echo "包含版本:"
echo "  ✓ ARM64版本: $ARM64_DIST_DIR (仅ARM64依赖)"
echo "  ✓ x86_64版本: $X86_64_DIST_DIR (仅x86_64依赖)"
echo ""
echo "使用方法:"
echo "  ARM64 Mac: cd $ARM64_DIST_DIR && ./run.sh"
echo "  Intel Mac: cd $X86_64_DIST_DIR && ./run.sh"
echo ""
echo "注意: 这些版本是真正的架构特定版本，在错误的架构上无法运行"
echo "查看README: cat $BASE_DIST_DIR/README.md"
