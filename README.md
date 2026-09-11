# Memory 项目

这是一个基于Spring Boot的多模块Maven项目，用于内存管理和监控。

## 项目结构

```
memory/
├── pom.xml                 # 父项目POM文件
├── memory-lib/             # 核心库模块
│   ├── pom.xml            # 库模块POM文件
│   └── src/main/java/     # 核心工具类和库代码
└── memory-core/           # 应用启动模块
    ├── pom.xml            # 应用模块POM文件
    └── src/main/java/     # Spring Boot启动类和应用代码
```

## 模块说明

### memory-lib 模块
- **作用**: 提供核心工具类和库功能
- **包含**: 
  - 内存监控工具类
  - 数据库连接支持 (SQLite, MySQL, Oracle, PostgreSQL)
  - 通用工具类 (Lombok, Apache Commons, Guava等)
  - JSON/XML处理工具
- **打包方式**: JAR包
- **依赖**: Spring Framework核心组件，各种工具库

### memory-core 模块
- **作用**: Spring Boot应用启动模块
- **包含**: 
  - Spring Boot启动类
  - Web应用支持
  - JavaFX UI组件
  - FreeMarker模板引擎
- **打包方式**: 可执行JAR包
- **依赖**: memory-lib模块 + Spring Boot启动器

## 构建和运行

### 编译项目
```bash
mvn clean compile
```

### 打包项目
```bash
mvn clean package -DskipTests
```

### 运行应用
```bash
# 方式1: 使用Maven插件
mvn spring-boot:run -pl memory-core

# 方式2: 直接运行JAR包
java -jar memory-core/target/memory-core-0.0.1-SNAPSHOT.jar
```

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.5
- **Maven**: 多模块项目
- **数据库**: SQLite, MySQL, Oracle, PostgreSQL
- **工具库**: Lombok, Apache Commons, Guava, MapStruct
- **UI**: JavaFX, JFoenix
- **模板引擎**: FreeMarker

## 开发说明

1. **memory-lib模块**: 开发核心业务逻辑和工具类
2. **memory-core模块**: 开发应用启动类和Web接口
3. 两个模块通过Maven依赖关系进行集成
4. 使用Spring Boot的自动配置功能简化开发

## 注意事项

- 项目使用Java 21，确保开发环境兼容
- memory-lib模块不包含Spring Boot启动器，只提供核心功能
- memory-core模块依赖memory-lib模块，提供完整的应用功能

## 分支说明
develop  为早期2025年单体应用程序开发模式下设计的程序
separate 为公司实际需求，要求对员工权限管理、信息共享有极高的项目需求
