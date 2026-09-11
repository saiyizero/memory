package com.murong.ecp.tools.fx.infrastructure.config;

import com.murong.ecp.tools.fx.domain.entity.DbConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.*;
import com.murong.ecp.tools.fx.infrastructure.repository.po.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class MemoryConfiguration {

    @Autowired
    private ApplicationContext applicationContext;
    
    // 连接池配置参数
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;
    
    @Value("${spring.datasource.hikari.pool-name:BusinessDataSourcePool}")
    private String poolName;

    // 本地数据源配置 - 优先创建，不依赖其他Bean
    @Bean
    @Primary
    @Qualifier("localDataSource")
    public DataSource localDataSource() throws IOException {
        // 从classpath复制数据库文件到临时目录
        ClassPathResource resource = new ClassPathResource("database/memory_embedded.db");
        Path tempDbPath = createTempDatabaseFile(resource);
        String jdbcUrl = "jdbc:sqlite:" + tempDbPath.toAbsolutePath().toString();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }

    private Path createTempDatabaseFile(ClassPathResource resource) throws IOException {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("memory-db");
        Path tempDbFile = tempDir.resolve("memory_embedded.db");
        // 复制数据库文件到临时目录
        try (var inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempDbFile, StandardCopyOption.REPLACE_EXISTING);
        }
        // 添加JVM关闭时的清理钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tempDbFile);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                // 忽略清理错误
            }
        }));

        return tempDbFile;
    }

    @Bean
    @Primary
    @Qualifier("localJdbcTemplate")
    @DependsOn("localDataSource")
    public JdbcTemplate localJdbcTemplate(@Qualifier("localDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // 业务数据源 - 使用HikariCP连接池，完全延迟初始化，避免循环依赖
    @Bean
    @Qualifier("businessDataSource")
    @Lazy
    public DataSource businessDataSource() {
        try {
            // 延迟获取LocalSettingDao，避免循环依赖
            LocalSettingDao localSettingDao = applicationContext.getBean(LocalSettingDao.class);
            LocalSettingPO localReqPO = new LocalSettingPO();
            localReqPO.setStatus(FlgEnum.YES.getValue());
            LocalSettingPO localSettingPO = localSettingDao.queryOne(localReqPO);
            
            if (localSettingPO == null) {
                throw new RuntimeException("无法获取本地数据库配置信息");
            }
            
            // 配置HikariCP连接池
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(localSettingPO.getDbDriverName());
            config.setJdbcUrl(localSettingPO.getDbUrl());
            config.setUsername(localSettingPO.getDbUsrName());
            config.setPassword(localSettingPO.getDbPassWord());
            
            // 连接池配置参数 - 从配置文件读取
            config.setMaximumPoolSize(maximumPoolSize);          // 最大连接数
            config.setMinimumIdle(minimumIdle);                  // 最小空闲连接数
            config.setConnectionTimeout(connectionTimeout);      // 连接超时时间
            config.setIdleTimeout(idleTimeout);                  // 空闲连接超时时间
            config.setMaxLifetime(maxLifetime);                  // 连接最大生存时间
            config.setLeakDetectionThreshold(leakDetectionThreshold); // 连接泄漏检测阈值
            
            // 连接池名称，便于监控
            config.setPoolName(poolName);
            
            // 连接测试查询（根据数据库类型设置）
            String driverName = localSettingPO.getDbDriverName().toLowerCase();
            if (driverName.contains("mysql")) {
                config.setConnectionTestQuery("SELECT 1");
            } else if (driverName.contains("oracle")) {
                config.setConnectionTestQuery("SELECT 1 FROM DUAL");
            } else if (driverName.contains("postgresql")) {
                config.setConnectionTestQuery("SELECT 1");
            } else if (driverName.contains("sqlite")) {
                config.setConnectionTestQuery("SELECT 1");
            }
            
            // 其他优化配置
            config.setAutoCommit(true);
            config.setReadOnly(false);
            
            return new HikariDataSource(config);
        } catch (Exception e) {
            // 数据库连接失败时，抛出特殊的异常，让启动流程处理
            throw new com.murong.ecp.tools.fx.infrastructure.config.DatabaseConnectionException("数据库连接失败，需要配置数据库连接信息: " + e.getMessage(), e);
        }
    }

    // 业务JdbcTemplate - 依赖业务数据源
    @Bean
    @Qualifier("businessJdbcTemplate")
    @Lazy
    @DependsOn("businessDataSource")
    public JdbcTemplate businessJdbcTemplate(@Qualifier("businessDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // GlobalProperties - 延迟初始化，依赖所有DAO
    @Bean
    @Lazy
    public GlobalProperties globalProperties() {
        try {
            // 延迟获取所有需要的DAO
            UserProjGroupDao userProjGroupDao = applicationContext.getBean(UserProjGroupDao.class);
            UserProjSettingDao userProjSettingDao = applicationContext.getBean(UserProjSettingDao.class);
            DbConnectionDao dbConnectionDao = applicationContext.getBean(DbConnectionDao.class);
            LocalSettingDao localSettingDao = applicationContext.getBean(LocalSettingDao.class);
            LocalSettingPO localReqPO = new LocalSettingPO();
            localReqPO.setStatus(FlgEnum.YES.getValue());
            LocalSettingPO localSettingPO = localSettingDao.queryOne(localReqPO);
            UserProjGroupPO userProjGroupPO = userProjGroupDao.queryCurGroup(localSettingPO);
            
            // 添加空值检查
            UserProjSettingPO userProjSetting = null;
            if (userProjGroupPO != null) {
                userProjSetting=userProjSettingDao.queryCurProject(userProjGroupPO.getGroupName(),localSettingPO);
            }

            String groupName=null;
            String projectName=null;
            DbConfig dbConfig=null;

            if(userProjSetting != null) {
                DbConnectionPO dbConnReqPO = new DbConnectionPO();
                dbConnReqPO.setProjectName(userProjSetting.getProjectName());
                dbConnReqPO.setGroupName(userProjSetting.getGroupName());
                dbConnReqPO.setMainFlg("1");
                DbConnectionPO mainDbConn = dbConnectionDao.queryOne(dbConnReqPO);

                if (mainDbConn != null) {
                    dbConfig = new DbConfig(mainDbConn);
                }
                groupName = userProjSetting.getGroupName();
                projectName = userProjSetting.getProjectName();
            }
            GlobalProperties globalPropes = new GlobalProperties(groupName, projectName,dbConfig);
            if(userProjSetting != null) {
                globalPropes.setAppPort(userProjSetting.getAppPort());
                globalPropes.setAppName(userProjSetting.getAppName());
                globalPropes.setBasePath(userProjSetting.getBasePath());
            }

            return globalPropes;
        } catch (Exception e) {
            throw new RuntimeException("初始化GlobalProperties失败: " + e.getMessage(), e);
        }
    }
    
    // 全局属性初始化 - 最后执行
//    @Bean
//    @Lazy
//    @DependsOn("globalProperties")
//    public String initializeGlobalProperties() {
//        try {
//            // 延迟初始化模块信息，避免循环依赖
//            UserProjGroupDao userProjGroupDao = applicationContext.getBean(UserProjGroupDao.class);
//            UserProjSettingDao userProjSettingDao = applicationContext.getBean(UserProjSettingDao.class);
//            ProjectFolderDao projectFolderDao = applicationContext.getBean(ProjectFolderDao.class);
//
//            UserProjGroupPO userProjGrp = userProjGroupDao.queryCurGroup();
//            UserProjSettingPO projectEnv = userProjSettingDao.queryCurProject(userProjGrp.getGroupName());
//
//            ProjectFolderPO projectFolderPO = new ProjectFolderPO();
//            projectFolderPO.setProjectName(projectEnv.getProjectName());
//            projectFolderPO.setGroupName(projectEnv.getGroupName());
//            projectFolderPO.setAppName(projectEnv.getAppName());
//            List<ProjectFolderPO> projectDirLst = projectFolderDao.queryForList(projectFolderPO);
//
//            if (!CollectionUtils.isEmpty(projectDirLst)) {
//                // 通过Spring上下文获取已创建的GlobalProperties实例
//                GlobalProperties globalPropes = applicationContext.getBean(GlobalProperties.class);
//                globalPropes.setModules(projectDirLst);
//            }
//
//            return "GlobalProperties initialized";
//        } catch (Exception e) {
//            throw new RuntimeException("初始化模块信息失败: " + e.getMessage(), e);
//        }
//    }
    
    // 连接池监控方法 - 用于调试和监控连接池状态
//    @Bean
//    @Lazy
//    @DependsOn("businessDataSource")
//    public String monitorConnectionPool() {
//        try {
//            HikariDataSource dataSource = (HikariDataSource) applicationContext.getBean("businessDataSource");
//            System.out.println("=== 业务数据源连接池状态 ===");
//            System.out.println("连接池名称: " + dataSource.getPoolName());
//            System.out.println("最大连接数: " + dataSource.getMaximumPoolSize());
//            System.out.println("最小空闲连接数: " + dataSource.getMinimumIdle());
//            System.out.println("当前活跃连接数: " + dataSource.getHikariPoolMXBean().getActiveConnections());
//            System.out.println("当前空闲连接数: " + dataSource.getHikariPoolMXBean().getIdleConnections());
//            System.out.println("总连接数: " + dataSource.getHikariPoolMXBean().getTotalConnections());
//            System.out.println("等待连接的线程数: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
//            System.out.println("================================");
//            return "Connection pool monitoring initialized";
//        } catch (Exception e) {
//            System.err.println("连接池监控初始化失败: " + e.getMessage());
//            return "Connection pool monitoring failed";
//        }
//    }

}