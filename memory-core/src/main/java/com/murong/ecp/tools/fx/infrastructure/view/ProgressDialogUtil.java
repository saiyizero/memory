package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.Supplier;

/**
 * 通用进度对话框工具类
 * 支持后台任务执行和进度显示，自动处理对话框的显示和关闭
 * 采用现代化设计风格，提供优雅的用户体验
 */
public class ProgressDialogUtil {
    
    // 定义现代化的颜色主题
    private static final String PRIMARY_COLOR = "#00FFFF";  // 青色/青绿色
    private static final String SECONDARY_COLOR = "#00E5FF"; // 稍深的青色
    private static final String BACKGROUND_COLOR = "#000000"; // 黑色背景
    private static final String TEXT_COLOR = "#00FFFF"; // 青色文字
    private static final String SUBTEXT_COLOR = "#00CCCC"; // 稍暗的青色
    private static final String BORDER_COLOR = "#00FFFF"; // 青色边框
    private static final String PROGRESS_BG_COLOR = "#1A1A1A"; // 深灰色进度条背景
    
    /**
     * 执行后台任务并显示进度对话框
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     * @param parentStage 父窗口，用于设置对话框模态性
     */
    public static <T> void executeWithProgress(String title, 
                                             String headerText, 
                                             String contentText,
                                             Supplier<T> task,
                                             java.util.function.Consumer<T> onSuccess,
                                             java.util.function.Consumer<String> onError,
                                             Stage parentStage) {
        
        // 创建进度对话框
        Dialog<Void> progressDialog = createProgressDialog(title, headerText, contentText, parentStage);
        
        // 在后台线程中执行任务
        new Thread(() -> {
            try {
                // 执行任务
                T result = task.get();
                
                // 在主线程中处理结果
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行成功回调
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
                
            } catch (Exception e) {
                // 在主线程中处理异常
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行错误回调
                    if (onError != null) {
                        onError.accept(e.getMessage());
                    }
                });
            }
        }).start();
        
        // 显示进度对话框
        progressDialog.showAndWait();
    }
    
    /**
     * 执行后台任务并显示进度对话框（简化版本，只处理成功和失败）
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     * @param parentStage 父窗口，用于设置对话框模态性
     */
    public static <T> void executeWithProgress(String title, 
                                             String headerText, 
                                             String contentText,
                                             Supplier<T> task,
                                             java.util.function.Consumer<T> onSuccess,
                                             java.util.function.Consumer<String> onError) {
        executeWithProgress(title, headerText, contentText, task, onSuccess, onError, null);
    }
    
    /**
     * 执行后台任务并显示进度对话框（最简化版本，使用默认文本）
     * 
     * @param title 对话框标题
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     */
    public static <T> void executeWithProgress(String title,
                                             Supplier<T> task,
                                             java.util.function.Consumer<T> onSuccess,
                                             java.util.function.Consumer<String> onError) {
        executeWithProgress(title, "正在处理...", "请稍候...", task, onSuccess, onError, null);
    }
    
    /**
     * 创建美化后的进度对话框
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param parentStage 父窗口
     * @return 配置好的进度对话框
     */
    private static Dialog<Void> createProgressDialog(String title, 
                                                   String headerText, 
                                                   String contentText,
                                                   Stage parentStage) {
        
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(headerText);
        progressDialog.setContentText(contentText);
        progressDialog.getDialogPane().getButtonTypes().clear();
        progressDialog.setResizable(false);
        
        // 设置模态性
        if (parentStage != null) {
            progressDialog.initOwner(parentStage);
            progressDialog.initModality(Modality.WINDOW_MODAL);
        } else {
            progressDialog.initModality(Modality.APPLICATION_MODAL);
        }
        
        // 创建标题标签
        Label titleLabel = new Label(headerText);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web(TEXT_COLOR));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建进度条（参考截图样式）
        ProgressBar progressBar = createStyledProgressBar();
        
        // 创建加载动画文本
        Label loadingLabel = new Label("加载中");
        loadingLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        loadingLabel.setTextFill(Color.web(TEXT_COLOR));
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建动态点点动画
        createLoadingDotsAnimation(loadingLabel);
        
        // 创建内容容器
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setStyle(String.format(
            "-fx-padding: 30;" +
            "-fx-background-color: %s;" +
            "-fx-border-color: %s;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;", 
            BACKGROUND_COLOR, BORDER_COLOR
        ));
        
        // 添加发光效果
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setRadius(15);
        glow.setSpread(0.3);
        glow.setColor(Color.web(PRIMARY_COLOR + "80")); // 半透明青色
        content.setEffect(glow);
        
        content.getChildren().addAll(titleLabel, progressBar, loadingLabel);
        
        // 设置对话框样式
        progressDialog.getDialogPane().setContent(content);
        progressDialog.getDialogPane().setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        
        return progressDialog;
    }
    
    /**
     * 创建增强版进度对话框（包含进度条和进度指示器）
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param parentStage 父窗口
     * @return 配置好的进度对话框
     */
    public static Dialog<Void> createEnhancedProgressDialog(String title, 
                                                          String headerText, 
                                                          String contentText,
                                                          Stage parentStage) {
        
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(headerText);
        progressDialog.setContentText(contentText);
        progressDialog.getDialogPane().getButtonTypes().clear();
        progressDialog.setResizable(false);
        
        // 设置模态性
        if (parentStage != null) {
            progressDialog.initOwner(parentStage);
            progressDialog.initModality(Modality.WINDOW_MODAL);
        } else {
            progressDialog.initModality(Modality.APPLICATION_MODAL);
        }
        
        // 创建进度指示器
        ProgressIndicator progressIndicator = createStyledProgressIndicator();
        
        // 创建进度条
        ProgressBar progressBar = createStyledProgressBar();
        
        // 创建标题标签
        Label titleLabel = new Label(headerText);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web(TEXT_COLOR));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建内容标签
        Label contentLabel = new Label(contentText);
        contentLabel.setFont(Font.font("System", 14));
        contentLabel.setTextFill(Color.web(SUBTEXT_COLOR));
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建加载动画文本
        Label loadingLabel = new Label("加载中");
        loadingLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        loadingLabel.setTextFill(Color.web(TEXT_COLOR));
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建动态点点动画
        createLoadingDotsAnimation(loadingLabel);
        
        // 创建进度条容器
        VBox progressContainer = new VBox(15);
        progressContainer.setAlignment(Pos.CENTER);
        progressContainer.getChildren().addAll(progressBar, progressIndicator);
        
        // 创建内容容器
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setStyle(String.format(
            "-fx-padding: 35;" +
            "-fx-background-color: %s;" +
            "-fx-border-color: %s;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;", 
            BACKGROUND_COLOR, BORDER_COLOR
        ));
        
        // 添加发光效果
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setRadius(20);
        glow.setSpread(0.3);
        glow.setColor(Color.web(PRIMARY_COLOR + "80")); // 半透明青色
        content.setEffect(glow);
        
        content.getChildren().addAll(titleLabel, progressContainer, contentLabel, loadingLabel);
        
        // 设置对话框样式
        progressDialog.getDialogPane().setContent(content);
        progressDialog.getDialogPane().setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        
        return progressDialog;
    }
    
    /**
     * 创建美化后的进度指示器
     * 
     * @return 样式化的进度指示器
     */
    private static ProgressIndicator createStyledProgressIndicator() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.setMaxSize(60, 60);
        
        // 设置进度指示器样式 - 简洁的科技风格
        progressIndicator.setStyle(String.format(
            "-fx-progress-color: %s;" +
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: transparent;" +
            "-fx-effect: dropshadow(gaussian, %s, 10, 0, 0, 0);", 
            PRIMARY_COLOR, PRIMARY_COLOR + "40"
        ));
        
        // 创建简单的旋转动画
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(2), progressIndicator);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);
        rotateTransition.setAutoReverse(false);
        rotateTransition.play();
        
        return progressIndicator;
    }
    
    /**
     * 创建现代化进度条（替代进度指示器）
     * 
     * @return 样式化的进度条
     */
    private static ProgressBar createStyledProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(250);
        progressBar.setPrefHeight(12);
        progressBar.setMaxWidth(250);
        
        // 设置进度条样式 - 参考截图的胶囊形状和发光效果
        progressBar.setStyle(String.format(
            "-fx-accent: %s;" +
            "-fx-background-color: %s;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-border-color: %s;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, %s, 8, 0, 0, 0);" +
            "-fx-control-inner-background: %s;",
            PRIMARY_COLOR, PROGRESS_BG_COLOR, PRIMARY_COLOR, PRIMARY_COLOR + "60", PROGRESS_BG_COLOR
        ));
        
        return progressBar;
    }
    
    /**
     * 创建动态点点动画
     * 
     * @param loadingLabel 加载文本标签
     */
    private static void createLoadingDotsAnimation(Label loadingLabel) {
        // 创建动态点点动画
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        
        // 添加多个关键帧来创建点点动画效果
        timeline.getKeyFrames().addAll(
            new javafx.animation.KeyFrame(Duration.ZERO, e -> loadingLabel.setText("加载中")),
            new javafx.animation.KeyFrame(Duration.seconds(0.5), e -> loadingLabel.setText("加载中.")),
            new javafx.animation.KeyFrame(Duration.seconds(1.0), e -> loadingLabel.setText("加载中..")),
            new javafx.animation.KeyFrame(Duration.seconds(1.5), e -> loadingLabel.setText("加载中..."))
        );
        
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    /**
     * 执行后台任务并显示进度对话框（无返回值版本）
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param task 后台任务
     * @param onSuccess 成功回调
     * @param onError 错误回调，参数为异常信息
     * @param parentStage 父窗口
     */
    public static void executeWithProgress(String title, 
                                         String headerText, 
                                         String contentText,
                                         Runnable task,
                                         Runnable onSuccess,
                                         java.util.function.Consumer<String> onError,
                                         Stage parentStage) {
        
        // 转换为Supplier<Void>形式
        Supplier<Void> taskSupplier = () -> {
            task.run();
            return null;
        };
        
        java.util.function.Consumer<Void> successConsumer = result -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        };
        
        executeWithProgress(title, headerText, contentText, taskSupplier, successConsumer, onError, parentStage);
    }
    
    /**
     * 执行后台任务并显示进度对话框（无返回值版本，简化）
     * 
     * @param title 对话框标题
     * @param task 后台任务
     * @param onSuccess 成功回调
     * @param onError 错误回调，参数为异常信息
     */
    public static void executeWithProgress(String title,
                                         Runnable task,
                                         Runnable onSuccess,
                                         java.util.function.Consumer<String> onError) {
        executeWithProgress(title, "正在处理...", "请稍候...", task, onSuccess, onError, null);
    }

    /**
     * 执行后台任务并显示增强版进度对话框
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     * @param parentStage 父窗口，用于设置对话框模态性
     */
    public static <T> void executeWithEnhancedProgress(String title, 
                                                     String headerText, 
                                                     String contentText,
                                                     Supplier<T> task,
                                                     java.util.function.Consumer<T> onSuccess,
                                                     java.util.function.Consumer<String> onError,
                                                     Stage parentStage) {
        
        // 创建增强版进度对话框
        Dialog<Void> progressDialog = createEnhancedProgressDialog(title, headerText, contentText, parentStage);
        
        // 在后台线程中执行任务
        new Thread(() -> {
            try {
                // 执行任务
                T result = task.get();
                
                // 在主线程中处理结果
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行成功回调
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
                
            } catch (Exception e) {
                // 在主线程中处理异常
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行错误回调
                    if (onError != null) {
                        onError.accept(e.getMessage());
                    }
                });
            }
        }).start();
        
        // 显示进度对话框
        progressDialog.showAndWait();
    }
    
    /**
     * 执行后台任务并显示增强版进度对话框（简化版本）
     * 
     * @param title 对话框标题
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     */
    public static <T> void executeWithEnhancedProgress(String title,
                                                     Supplier<T> task,
                                                     java.util.function.Consumer<T> onSuccess,
                                                     java.util.function.Consumer<String> onError) {
        executeWithEnhancedProgress(title, "正在处理...", "请稍候...", task, onSuccess, onError, null);
    }

    /**
     * 创建截图样式的进度对话框（简洁科技风格）
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param parentStage 父窗口
     * @return 配置好的进度对话框
     */
    public static Dialog<Void> createScreenshotStyleProgressDialog(String title, 
                                                                 String headerText, 
                                                                 String contentText,
                                                                 Stage parentStage) {
        
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(headerText);
        progressDialog.setContentText(contentText);
        progressDialog.getDialogPane().getButtonTypes().clear();
        progressDialog.setResizable(false);
        
        // 设置模态性
        if (parentStage != null) {
            progressDialog.initOwner(parentStage);
            progressDialog.initModality(Modality.WINDOW_MODAL);
        } else {
            progressDialog.initModality(Modality.APPLICATION_MODAL);
        }
        
        // 创建加载文本（参考截图样式）
        Label loadingLabel = new Label("加载中");
        loadingLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        loadingLabel.setTextFill(Color.web(TEXT_COLOR));
        loadingLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 创建动态点点动画
        createLoadingDotsAnimation(loadingLabel);
        
        // 创建进度条（参考截图的胶囊形状）
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(280);
        progressBar.setPrefHeight(15);
        progressBar.setMaxWidth(280);
        
        // 设置进度条样式 - 完全参考截图
        progressBar.setStyle(String.format(
            "-fx-accent: %s;" +
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 7.5;" +
            "-fx-border-radius: 7.5;" +
            "-fx-border-color: %s;" +
            "-fx-border-width: 2;" +
            "-fx-control-inner-background: transparent;",
            PRIMARY_COLOR, PRIMARY_COLOR
        ));
        
        // 创建内容容器
        VBox content = new VBox(25);
        content.setAlignment(Pos.CENTER);
        content.setStyle(String.format(
            "-fx-padding: 40;" +
            "-fx-background-color: %s;", 
            BACKGROUND_COLOR
        ));
        
        content.getChildren().addAll(loadingLabel, progressBar);
        
        // 设置对话框样式
        progressDialog.getDialogPane().setContent(content);
        progressDialog.getDialogPane().setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        
        return progressDialog;
    }
    
    /**
     * 执行后台任务并显示截图样式的进度对话框
     * 
     * @param title 对话框标题
     * @param headerText 对话框头部文本
     * @param contentText 对话框内容文本
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     * @param parentStage 父窗口，用于设置对话框模态性
     */
    public static <T> void executeWithScreenshotStyleProgress(String title, 
                                                            String headerText, 
                                                            String contentText,
                                                            Supplier<T> task,
                                                            java.util.function.Consumer<T> onSuccess,
                                                            java.util.function.Consumer<String> onError,
                                                            Stage parentStage) {
        
        // 创建截图样式的进度对话框
        Dialog<Void> progressDialog = createScreenshotStyleProgressDialog(title, headerText, contentText, parentStage);
        
        // 在后台线程中执行任务
        new Thread(() -> {
            try {
                // 执行任务
                T result = task.get();
                
                // 在主线程中处理结果
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行成功回调
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
                
            } catch (Exception e) {
                // 在主线程中处理异常
                Platform.runLater(() -> {
                    // 强制关闭进度对话框
                    progressDialog.setResult(null);
                    progressDialog.close();
                    
                    // 确保对话框窗口被隐藏
                    if (progressDialog.getDialogPane().getScene() != null && 
                        progressDialog.getDialogPane().getScene().getWindow() != null) {
                        progressDialog.getDialogPane().getScene().getWindow().hide();
                    }
                    
                    // 强制设置对话框为不可见
                    progressDialog.getDialogPane().setVisible(false);
                    
                    // 直接执行错误回调
                    if (onError != null) {
                        onError.accept(e.getMessage());
                    }
                });
            }
        }).start();
        
        // 显示进度对话框
        progressDialog.showAndWait();
    }
    
    /**
     * 执行后台任务并显示截图样式的进度对话框（简化版本）
     * 
     * @param title 对话框标题
     * @param task 后台任务，返回执行结果
     * @param onSuccess 成功回调，参数为任务结果
     * @param onError 错误回调，参数为异常信息
     */
    public static <T> void executeWithScreenshotStyleProgress(String title,
                                                            Supplier<T> task,
                                                            java.util.function.Consumer<T> onSuccess,
                                                            java.util.function.Consumer<String> onError) {
        executeWithScreenshotStyleProgress(title, "处理中", "请稍候...", task, onSuccess, onError, null);
    }
} 