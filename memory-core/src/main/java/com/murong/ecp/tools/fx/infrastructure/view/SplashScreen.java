package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动画面类
 * 用于在程序启动时显示进度和状态信息
 */
public class SplashScreen {
    
    private Stage splashStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label progressLabel;
    private Timeline progressTimeline;
    private AtomicInteger progress = new AtomicInteger(0);
    
    private static final int SPLASH_WIDTH = 500;
    private static final int SPLASH_HEIGHT = 300;
    private static final String APP_NAME = "Memory";
    private static final String VERSION = "v2.0.3";
    
    public SplashScreen() {
        initializeSplashScreen();
    }
    
    /**
     * 初始化启动画面
     */
    private void initializeSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setAlwaysOnTop(true);
        
        // 创建主容器
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("splash-screen");
        
        // 创建LOGO
        ImageView logoImageView = new ImageView();
        logoImageView.getStyleClass().add("splash-logo");
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/image/login-logo.png"));
            logoImageView.setImage(logoImage);
            logoImageView.setFitWidth(80);
            logoImageView.setFitHeight(80);
            logoImageView.setPreserveRatio(true);
        } catch (Exception e) {
            // 如果找不到logo，使用默认图标
            logoImageView.setStyle("-fx-background-color: #3498db; -fx-shape: \"M 0 0 L 80 0 L 80 80 L 0 80 Z\";");
            logoImageView.setFitWidth(80);
            logoImageView.setFitHeight(80);
        }
        
        // 应用名称
        Label appNameLabel = new Label(APP_NAME);
        appNameLabel.getStyleClass().add("splash-app-name");
        
        // 版本信息
        Label versionLabel = new Label(VERSION);
        versionLabel.getStyleClass().add("splash-version");
        
        // 状态标签
        statusLabel = new Label("正在初始化...");
        statusLabel.getStyleClass().add("splash-status");
        
        // 进度条
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(8);
        progressBar.getStyleClass().add("splash-progress-bar");
        
        // 进度百分比标签
        progressLabel = new Label("0%");
        progressLabel.getStyleClass().add("splash-progress-text");
        
        // 版权信息
        Label copyrightLabel = new Label("© 2025 Memory Team. All rights reserved.");
        copyrightLabel.getStyleClass().add("splash-copyright");
        
        // 组装界面
        root.getChildren().addAll(
            logoImageView,
            appNameLabel,
            versionLabel,
            statusLabel,
            progressBar,
            progressLabel,
            copyrightLabel
        );
        
        Scene scene = new Scene(root, SPLASH_WIDTH, SPLASH_HEIGHT);
        
        // 加载CSS样式
        try {
            scene.getStylesheets().add(getClass().getResource("/css/splash-screen.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("无法加载启动画面CSS样式: " + e.getMessage());
        }
        
        splashStage.setScene(scene);
        
        // 设置窗口居中
        splashStage.centerOnScreen();
    }
    
    /**
     * 显示启动画面
     */
    public void show() {
        Platform.runLater(() -> {
            splashStage.show();
            startProgressAnimation();
        });
    }
    
    /**
     * 隐藏启动画面
     */
    public void hide() {
        Platform.runLater(() -> {
            // 创建淡出动画
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashStage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> splashStage.close());
            fadeOut.play();
        });
    }
    
    /**
     * 更新状态信息
     */
    public void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            System.out.println("[启动画面] " + status);
        });
    }
    
    /**
     * 更新进度
     */
    public void updateProgress(double progress) {
        Platform.runLater(() -> {
            this.progressBar.setProgress(progress);
            int percentage = (int) (progress * 100);
            progressLabel.setText(percentage + "%");
        });
    }
    
    /**
     * 开始进度动画
     */
    private void startProgressAnimation() {
        progressTimeline = new Timeline();
        progressTimeline.setCycleCount(Timeline.INDEFINITE);
        
        KeyFrame keyFrame = new KeyFrame(Duration.millis(50), event -> {
            int currentProgress = progress.incrementAndGet();
            double normalizedProgress = Math.min(currentProgress / 100.0, 0.95); // 最多到95%
            updateProgress(normalizedProgress);
            
            // 根据进度更新状态
            if (currentProgress < 20) {
                updateStatus("正在初始化系统参数...");
            } else if (currentProgress < 40) {
                updateStatus("正在加载数据库配置...");
            } else if (currentProgress < 60) {
                updateStatus("正在验证许可证...");
            } else if (currentProgress < 80) {
                updateStatus("正在初始化主界面...");
            } else {
                updateStatus("正在完成启动...");
            }
        });
        
        progressTimeline.getKeyFrames().add(keyFrame);
        progressTimeline.play();
    }
    
    /**
     * 停止进度动画
     */
    public void stopProgressAnimation() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
    }
    
    /**
     * 完成启动
     */
    public void complete() {
        stopProgressAnimation();
        updateProgress(1.0);
        updateStatus("启动完成");
        
        // 延迟隐藏启动画面
        Timeline hideTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> hide()));
        hideTimeline.play();
    }
    
    /**
     * 获取启动画面Stage
     */
    public Stage getStage() {
        return splashStage;
    }
} 