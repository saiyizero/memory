package com.murong.ecp.tools.fx.infrastructure.config;


import com.murong.ecp.tools.fx.MemoryApplication;
import com.murong.ecp.tools.fx.domain.service.auth.LoginService;
import com.murong.ecp.tools.fx.infrastructure.license.LicenseValidator;

import com.murong.ecp.tools.fx.infrastructure.view.LoginDialog;
import com.murong.ecp.tools.fx.infrastructure.view.MacDockIcon;
import com.murong.ecp.tools.fx.infrastructure.view.NativeSplashScreen;
import com.murong.ecp.tools.fx.infrastructure.view.DatabaseConfigDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXRunner extends Application {

    private ConfigurableApplicationContext context;
    private LicenseValidator licenseValidator;
    private long startTime;
    private boolean databaseConnectionFailed = false;

    public JavaFXRunner() {
        // 在构造函数中记录启动时间
        startTime = System.currentTimeMillis();
        System.out.println("[启动] === Memory应用启动开始 ===");
        System.out.println("[启动] 开始时间: " + new java.util.Date());
        
        // 更新原生启动画面状态
        NativeSplashScreen.updateStatus("JavaFX启动完成，开始初始化...");
    }

    @Override
    public void init() {
        System.out.println("[启动] 开始初始化系统参数...");
        
        // 更新原生启动画面状态
        NativeSplashScreen.updateStatus("正在初始化系统参数...");
        
        long springStartTime = System.currentTimeMillis();
        
        try {
            this.context = new SpringApplicationBuilder()
                    .sources(MemoryApplication.class)
                    .run(getParameters().getRaw().toArray(new String[0]));
        } catch (Exception e) {
            // 检查是否是数据库连接异常
            if (e.getCause() instanceof com.murong.ecp.tools.fx.infrastructure.config.DatabaseConnectionException || 
                e.getMessage() != null && e.getMessage().contains("数据库连接失败")) {
                System.out.println("[启动] 数据库连接失败，需要配置数据库连接信息");
                NativeSplashScreen.updateStatus("数据库连接失败，准备显示配置窗口...");
                
                // 设置标志，在start()方法中处理
                this.databaseConnectionFailed = true;
                
                // 继续完成init()方法的其他部分，但不获取许可证验证器
                System.out.println("[启动] Spring容器初始化失败，但继续完成init()方法");
                NativeSplashScreen.updateStatus("准备显示数据库配置窗口...");
                
                long initEndTime = System.currentTimeMillis();
                System.out.println("[启动] init()方法完成（数据库连接失败），总耗时: " + (initEndTime - startTime) + "ms");
                return;
            } else {
                // 其他异常，直接抛出
                throw e;
            }
        }
        
        long springEndTime = System.currentTimeMillis();
        System.out.println("[启动] 系统参数初始化完成，耗时: " + (springEndTime - springStartTime) + "ms");
        
        // 更新原生启动画面状态
        NativeSplashScreen.updateStatus("系统参数初始化完成");
        
        // 获取许可证验证器
        long licenseStartTime = System.currentTimeMillis();
        System.out.println("[启动] 开始获取许可证验证器...");
        NativeSplashScreen.updateStatus("正在获取许可证验证器...");
        this.licenseValidator = context.getBean(LicenseValidator.class);
        long licenseEndTime = System.currentTimeMillis();
        System.out.println("[启动] 许可证验证器获取完成，耗时: " + (licenseEndTime - licenseStartTime) + "ms");
        NativeSplashScreen.updateStatus("许可证验证器获取完成");
        
        long initEndTime = System.currentTimeMillis();
        System.out.println("[启动] init()方法完成，总耗时: " + (initEndTime - startTime) + "ms");
    }

    @Override
    public void start(Stage primaryStage) {
        long startStartTime = System.currentTimeMillis();
        System.out.println("[启动] === 开始启动主界面 ===");
        
        // 检查是否数据库连接失败
        if (databaseConnectionFailed) {
            System.out.println("[启动] 检测到数据库连接失败，显示配置窗口");
            NativeSplashScreen.updateStatus("数据库连接失败，准备显示配置窗口...");
            
            // 延迟处理，确保JavaFX线程完全就绪
            Platform.runLater(() -> {
                try {
                    // 隐藏启动画面
                    NativeSplashScreen.hide();
                } catch (Exception e) {
                    System.err.println("隐藏启动画面失败: " + e.getMessage());
                }
                
                // 显示数据库配置窗口
                showDatabaseConfigDialogWithoutSpring();
                
                // 配置完成后重新启动应用
                System.exit(0);
            });
            return;
        }
        
        MacDockIcon.apply();
        NativeSplashScreen.updateStatus("正在启动主界面...");
        
        // 在启动主界面之前验证许可证
        long licenseValidateStartTime = System.currentTimeMillis();
        System.out.println("[启动] 开始许可证验证...");
        NativeSplashScreen.updateStatus("正在验证许可证...");
        if (!licenseValidator.validateLicense(primaryStage)) {
            // 许可证验证失败，应用退出
            System.out.println("[启动] 许可证验证失败，应用退出");
            NativeSplashScreen.updateStatus("许可证验证失败");
            // 确保启动动画被隐藏
            NativeSplashScreen.hide();
            Platform.exit();
            return;
        }
        long licenseValidateEndTime = System.currentTimeMillis();
        System.out.println("[启动] 许可证验证完成，耗时: " + (licenseValidateEndTime - licenseValidateStartTime) + "ms");
        NativeSplashScreen.updateStatus("许可证验证完成");
        
        // 许可证验证成功，检查登录信息
        long loginCheckStartTime = System.currentTimeMillis();
        System.out.println("[启动] 开始检查登录信息...");
        NativeSplashScreen.updateStatus("正在检查登录信息...");
        
        // 获取登录服务
        LoginService loginService = context.getBean(LoginService.class);
        
        // 检查是否需要登录
        try {
            if (!loginService.requiresLogin()) {
                System.out.println("[启动] 登录信息表为空，需要用户登录");
                NativeSplashScreen.updateStatus("需要用户登录，准备显示登录窗口...");
                
                // 隐藏启动画面
                NativeSplashScreen.hide();
                
                // 显示登录窗口
                LoginDialog loginDialog = context.getBean(LoginDialog.class);
                boolean loginSuccess = loginDialog.showLoginDialog(primaryStage);
                
                if (!loginSuccess) {
                    System.out.println("[启动] 用户取消登录，应用退出");
                    Platform.exit();
                    return;
                }
                
                System.out.println("[启动] 用户登录成功");
                NativeSplashScreen.updateStatus("用户登录成功，继续启动...");
            } else {
                System.out.println("[启动] 登录信息表已有数据，跳过登录");
                NativeSplashScreen.updateStatus("登录信息检查完成");
            }
        } catch (Exception e) {
            // 检查是否是数据库连接异常
            if (e.getCause() instanceof com.murong.ecp.tools.fx.infrastructure.config.DatabaseConnectionException || 
                e.getMessage() != null && e.getMessage().contains("数据库连接失败")) {
                System.out.println("[启动] 数据库连接失败，需要配置数据库连接信息");
                NativeSplashScreen.updateStatus("数据库连接失败，准备显示配置窗口...");
                
                // 隐藏启动画面
                NativeSplashScreen.hide();
                
                // 显示数据库配置窗口
                DatabaseConfigDialog configDialog = context.getBean(DatabaseConfigDialog.class);
                boolean configSuccess = configDialog.showDatabaseConfigDialog(primaryStage);
                
                if (!configSuccess) {
                    System.out.println("[启动] 用户取消数据库配置，应用退出");
                    Platform.exit();
                    return;
                }
                
                System.out.println("[启动] 数据库配置成功，重新启动应用");
                // 重新启动应用
                Platform.exit();
                System.exit(0);
                return;
            } else {
                // 其他异常，直接抛出
                throw e;
            }
        }
        
        long loginCheckEndTime = System.currentTimeMillis();
        System.out.println("[启动] 登录信息检查完成，耗时: " + (loginCheckEndTime - loginCheckStartTime) + "ms");
        
        // 登录检查完成，启动主界面
        long stageEventStartTime = System.currentTimeMillis();
        System.out.println("[启动] 开始发布StageReadyEvent事件...");
        NativeSplashScreen.updateStatus("正在初始化主界面组件...");
        context.publishEvent(new StageReadyEvent(primaryStage));
        long stageEventEndTime = System.currentTimeMillis();
        System.out.println("[启动] StageReadyEvent事件发布完成，耗时: " + (stageEventEndTime - stageEventStartTime) + "ms");
        
        long startEndTime = System.currentTimeMillis();
        System.out.println("[启动] start()方法完成，耗时: " + (startEndTime - startStartTime) + "ms");
        System.out.println("[启动] === 主界面启动完成 ===");
        
        long totalTime = startEndTime - startTime;
        System.out.println("[启动] === Memory应用启动完成，总耗时: " + totalTime + "ms ===");
        
        // 启动完成，调用complete方法
        NativeSplashScreen.complete();
    }

    /**
     * 显示数据库配置对话框（不依赖Spring容器）
     */
    private void showDatabaseConfigDialogWithoutSpring() {
        try {
            // 加载FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/database_config_dialog.fxml"));
            Parent root = fxmlLoader.load();

            // 获取控制器
            com.murong.ecp.tools.fx.infrastructure.controller.DatabaseConfigDialogController controller = fxmlLoader.getController();

            // 创建配置窗口
            Stage configStage = new Stage();
            configStage.initModality(Modality.APPLICATION_MODAL);
            configStage.initStyle(StageStyle.UTILITY);
            configStage.setTitle("数据库连接配置");
            configStage.setResizable(false);

            // 设置场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/database-config-dialog.css").toExternalForm());
            configStage.setScene(scene);

            // 设置控制器
            controller.setDialogStage(configStage);

            // 显示窗口并等待关闭
            configStage.showAndWait();

            // 检查配置是否成功
            if (controller.isConfigSuccess()) {
                System.out.println("[启动] 数据库配置成功，准备重新启动应用");
            } else {
                System.out.println("[启动] 用户取消数据库配置");
            }

        } catch (Exception e) {
            System.err.println("显示数据库配置窗口失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("[启动] 应用停止，开始清理资源...");
        NativeSplashScreen.hide();
        if (context != null) {
            context.close();
        }
        Platform.exit();
        System.out.println("[启动] 应用停止完成");
    }
} 