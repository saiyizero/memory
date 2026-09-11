# Memory 项目

这是一个基于Spring Boot的多模块Maven项目，用于内存管理和监控。

## 项目结构

```
memory/
├── pom.xml                 # 父项目POM文件
├── memory-lib/             # 核心库模块
├── memory-common/          # 客户端与服务端共享代码
├── memory-service/         # 远程服务（元数据库 / HTTP API）
└── memory-client/          # JavaFX 桌面客户端
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

### memory-common 模块
- **作用**: 客户端与服务端共享的 PO、枚举、RPC DTO
- **打包方式**: JAR包

### memory-service 模块
- **作用**: 连接远程/元数据库，对外提供 HTTP API
- **打包方式**: 可执行 JAR

### memory-client 模块
- **作用**: JavaFX 桌面客户端（本地 SQLite、界面、代码生成）
- **打包方式**: 可执行 JAR
- **依赖**: memory-lib + memory-common，通过 HTTP 调用 memory-service

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
# 先启动服务端
mvn spring-boot:run -pl memory-service

# 再启动客户端
mvn spring-boot:run -pl memory-client
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
2. **memory-common模块**: 共享模型和协议
3. **memory-service模块**: 远程库访问与 REST 接口
4. **memory-client模块**: JavaFX 客户端界面与本地缓存

## 注意事项

- 项目使用Java 21，确保开发环境兼容
- memory-lib模块不包含Spring Boot启动器，只提供核心功能
- 客户端通过 HTTP 调用 memory-service，不再直接连接远程业务库

## 分支说明
develop  为早期2025年单体应用程序开发模式下设计的程序
separate 为公司实际需求，要求对员工权限管理、信息共享有极高的项目需求

## 脚本
[memory-service-deploy.sh](memory-service-deploy.sh): 将模块[memory-service](memory-service)
编译部署到服务器上，并成功启动