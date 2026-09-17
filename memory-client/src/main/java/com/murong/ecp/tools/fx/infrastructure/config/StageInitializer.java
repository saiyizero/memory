package com.murong.ecp.tools.fx.infrastructure.config;

import com.murong.ecp.tools.fx.domain.service.auth.LoginService;
import com.murong.ecp.tools.fx.infrastructure.view.NativeSplashScreen;
import com.murong.ecp.tools.fx.controller.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LoginService loginService;

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        long stageInitStartTime = System.currentTimeMillis();
        System.out.println("[界面] === 开始初始化主界面 ===");
        
        try {
            Stage stage = event.getStage();
            
            // 加载FXML文件
            long fxmlStartTime = System.currentTimeMillis();
            System.out.println("[界面] 开始加载FXML文件: /fxml/main.fxml");
            NativeSplashScreen.updateProgress(92); // 92% - 开始加载FXML
            NativeSplashScreen.updateStatus("正在加载界面布局...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            fxmlLoader.setClassLoader(getClass().getClassLoader());
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();
            long fxmlEndTime = System.currentTimeMillis();
            System.out.println("[界面] FXML文件加载完成，耗时: " + (fxmlEndTime - fxmlStartTime) + "ms");
            
            // 创建场景
            long sceneStartTime = System.currentTimeMillis();
            System.out.println("[界面] 开始创建Scene...");
            NativeSplashScreen.updateProgress(94); // 94% - 开始创建Scene
            NativeSplashScreen.updateStatus("正在创建界面场景...");
            Scene scene = new Scene(root);
            long sceneEndTime = System.currentTimeMillis();
            System.out.println("[界面] Scene创建完成，耗时: " + (sceneEndTime - sceneStartTime) + "ms");
            
            // 加载样式表
            long cssStartTime = System.currentTimeMillis();
            System.out.println("[界面] 开始加载样式表...");
            NativeSplashScreen.updateProgress(95); // 95% - 开始加载样式表
            NativeSplashScreen.updateStatus("正在加载界面样式...");
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/sql-highlighter.css").toExternalForm());
            long cssEndTime = System.currentTimeMillis();
            System.out.println("[界面] 样式表加载完成，耗时: " + (cssEndTime - cssStartTime) + "ms");
            
            // 设置Stage属性
            long stageConfigStartTime = System.currentTimeMillis();
            System.out.println("[界面] 开始配置Stage属性...");
            NativeSplashScreen.updateProgress(97); // 97% - 开始配置Stage
            NativeSplashScreen.updateStatus("正在配置主窗口...");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setWidth(1380);
            stage.setHeight(700);
            stage.centerOnScreen();
            long stageConfigEndTime = System.currentTimeMillis();
            System.out.println("[界面] Stage属性配置完成，耗时: " + (stageConfigEndTime - stageConfigStartTime) + "ms");
            
            // 配置完成后开始准备隐藏启动画面
            NativeSplashScreen.updateProgress(98);
            NativeSplashScreen.updateStatus("检查登录状态...");
            
            // 检查是否有有效的登录信息
            if (!loginService.requiresLogin()) {
                System.err.println("[界面] 登录状态检查失败，无法显示主界面");
                NativeSplashScreen.updateStatus("登录状态检查失败");
                return;
            }
            
            // 登录状态检查通过，准备显示主界面
            NativeSplashScreen.updateStatus("准备显示主界面...");
            NativeSplashScreen.complete();
            
            // 获取MainController并设置Stage引用
            MainController mainController = fxmlLoader.getController();
            if (mainController != null) {
                mainController.setMainStage(stage);
                System.out.println("[界面] 成功设置MainController的Stage引用");
            } else {
                System.err.println("[界面] 警告: 无法获取MainController实例");
            }
            
            // 显示Stage
            long showStartTime = System.currentTimeMillis();
            System.out.println("[界面] 开始显示主界面...");
            
            stage.show();
            long showEndTime = System.currentTimeMillis();
            System.out.println("[界面] 主界面显示完成，耗时: " + (showEndTime - showStartTime) + "ms");
            
            long stageInitEndTime = System.currentTimeMillis();
            System.out.println("[界面] === 主界面初始化完成，总耗时: " + (stageInitEndTime - stageInitStartTime) + "ms ===");
            
            NativeSplashScreen.updateProgress(99); // 99% - 主界面初始化完成
            NativeSplashScreen.updateStatus("主界面初始化完成");
            
        } catch (IOException e) {
            System.err.println("[界面] 主界面初始化失败: " + e.getMessage());
            NativeSplashScreen.updateStatus("主界面初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 