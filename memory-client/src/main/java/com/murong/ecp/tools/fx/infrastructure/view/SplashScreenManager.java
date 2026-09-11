package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动画面管理器
 * 用于协调启动画面和主界面的显示时机
 */
@Component
public class SplashScreenManager {
    
    private SplashScreen splashScreen;
    private Stage mainStage;
    private AtomicBoolean isMainStageReady = new AtomicBoolean(false);
    private AtomicBoolean isSplashComplete = new AtomicBoolean(false);
    
    /**
     * 初始化启动画面
     */
    public void initializeSplashScreen() {
        // 确保在JavaFX线程中创建启动画面
        if (Platform.isFxApplicationThread()) {
            createSplashScreen();
        } else {
            Platform.runLater(this::createSplashScreen);
        }
    }
    
    /**
     * 创建启动画面
     */
    private void createSplashScreen() {
        splashScreen = new SplashScreen();
        splashScreen.show();
    }
    
    /**
     * 更新启动画面状态
     */
    public void updateStatus(String status) {
        if (splashScreen != null) {
            splashScreen.updateStatus(status);
        }
    }
    
    /**
     * 更新启动画面进度
     */
    public void updateProgress(double progress) {
        if (splashScreen != null) {
            splashScreen.updateProgress(progress);
        }
    }
    
    /**
     * 设置主界面Stage
     */
    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
        this.isMainStageReady.set(true);
        checkAndCompleteSplash();
    }
    
    /**
     * 标记启动完成
     */
    public void markStartupComplete() {
        this.isSplashComplete.set(true);
        checkAndCompleteSplash();
    }
    
    /**
     * 检查并完成启动画面
     */
    private void checkAndCompleteSplash() {
        if (isMainStageReady.get() && isSplashComplete.get()) {
            // 确保主界面已经显示
            Platform.runLater(() -> {
                if (mainStage != null && mainStage.isShowing()) {
                    // 延迟一点时间确保主界面完全显示
                    CompletableFuture.delayedExecutor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            Platform.runLater(() -> {
                                if (splashScreen != null) {
                                    splashScreen.complete();
                                }
                            });
                        });
                }
            });
        }
    }
    
    /**
     * 隐藏启动画面
     */
    public void hideSplashScreen() {
        if (splashScreen != null) {
            splashScreen.hide();
        }
    }
    
    /**
     * 获取启动画面
     */
    public SplashScreen getSplashScreen() {
        return splashScreen;
    }
    
    /**
     * 检查启动画面是否显示
     */
    public boolean isSplashScreenShowing() {
        return splashScreen != null && splashScreen.getStage().isShowing();
    }
} 