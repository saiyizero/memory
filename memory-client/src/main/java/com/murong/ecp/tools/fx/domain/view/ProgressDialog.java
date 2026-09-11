package com.murong.ecp.tools.fx.domain.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 简洁美观的进度对话框（Mac风格）
 */
public class ProgressDialog extends Dialog<Void> {
    
    private ProgressIndicator progressIndicator;
    private Label contentLabel;
    private VBox mainContainer;
    
    public ProgressDialog() {
        initComponents();
        setupDialog();
    }
    
    private void initComponents() {
        // 创建进度指示器
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setStyle("-fx-progress-color: #007AFF;"); // Mac风格蓝色
        
        // 创建内容标签
        contentLabel = new Label("请稍候...");
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-font-family: 'SF Pro Display', 'Microsoft YaHei', 'SimSun', sans-serif;");
        contentLabel.setAlignment(Pos.CENTER);
        
        // 创建主容器
        mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30, 40, 30, 40));
        
        // 组装组件
        mainContainer.getChildren().addAll(progressIndicator, contentLabel);
        
        // 设置对话框内容
        getDialogPane().setContent(mainContainer);
    }
    
    private void setupDialog() {
        // 设置对话框属性
        setTitle("进度");
        setHeaderText(null);
        setResizable(false);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        
        // 移除默认按钮
        getDialogPane().getButtonTypes().clear();
        
        // 设置对话框大小
        setWidth(280);
        setHeight(180);
        
        // 设置Mac风格的透明磨砂背景
        getDialogPane().setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.8);" +  // 半透明白色
            "-fx-background-radius: 12;" +                        // 圆角
            "-fx-border-radius: 12;" +                           // 边框圆角
            "-fx-border-width: 0.5;" +                           // 细边框
            "-fx-border-color: rgba(0, 0, 0, 0.1);" +           // 淡灰色边框
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 10, 0, 0, 2);" // 柔和阴影
        );
    }
    
    /**
     * 设置对话框标题
     */
    public void setDialogTitle(String title) {
        // 使用反射设置标题，因为setTitle是final方法
        try {
            java.lang.reflect.Field titleField = javafx.stage.Window.class.getDeclaredField("title");
            titleField.setAccessible(true);
            titleField.set(this, title);
        } catch (Exception e) {
            // 如果反射失败，忽略错误
        }
    }
    
    /**
     * 设置对话框头部文本
     */
    public void setDialogHeaderText(String headerText) {
        // 使用反射设置头部文本，因为setHeaderText是final方法
        try {
            java.lang.reflect.Field headerTextField = javafx.scene.control.Dialog.class.getDeclaredField("headerText");
            headerTextField.setAccessible(true);
            headerTextField.set(this, headerText);
        } catch (Exception e) {
            // 如果反射失败，忽略错误
        }
    }
    
    /**
     * 设置进度标签文本
     */
    public void setProgressText(String progressText) {
        if (contentLabel != null) {
            contentLabel.setText(progressText);
        }
    }
    
    /**
     * 设置进度值
     */
    public void setProgressValue(double progress) {
        if (progress < 0) {
            progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        } else {
            progressIndicator.setProgress(progress);
        }
    }
    
    /**
     * 显示进度对话框
     */
    public void showProgressDialog() {
        // 不设置父窗口，避免循环绑定问题
        super.show();
    }
    
    /**
     * 显示进度对话框（指定父窗口）
     */
    public void showProgressDialog(Stage ownerStage) {
        if (ownerStage != null && ownerStage != this.getDialogPane().getScene().getWindow()) {
            initOwner(ownerStage);
        }
        super.show();
    }
    
    /**
     * 强制关闭进度对话框
     */
    public void forceClose() {
        try {
            if (isShowing()) {
                // 先尝试正常关闭
                close();
                
                // 如果还在显示，尝试隐藏
                if (isShowing()) {
                    hide();
                }
                
                // 最后的尝试：使用反射强制设置
                if (isShowing()) {
                    try {
                        java.lang.reflect.Field showingField = javafx.scene.control.Dialog.class.getDeclaredField("showing");
                        showingField.setAccessible(true);
                        showingField.set(this, false);
                    } catch (Exception refEx) {
                        // 忽略反射异常
                    }
                }
            }
        } catch (Exception e) {
            // 如果close()失败，尝试隐藏窗口
            try {
                if (isShowing()) {
                    hide();
                }
            } catch (Exception ex) {
                // 最后的尝试：使用反射强制设置
                try {
                    java.lang.reflect.Field showingField = javafx.scene.control.Dialog.class.getDeclaredField("showing");
                    showingField.setAccessible(true);
                    showingField.set(this, false);
                } catch (Exception refEx) {
                    // 忽略反射异常
                }
            }
        }
    }
    
    /**
     * 安全关闭进度对话框
     */
    public void safeClose() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (isShowing()) {
                    close();
                }
            } catch (Exception e) {
                // 如果失败，使用强制关闭
                forceClose();
            }
        });
    }
    
    /**
     * 强力关闭进度对话框
     */
    public void forceCloseImmediate() {
        try {
            // 立即尝试关闭
            if (isShowing()) {
                close();
            }
            
            // 如果还在显示，尝试隐藏
            if (isShowing()) {
                hide();
            }
            
            // 使用反射强制设置showing状态
            try {
                java.lang.reflect.Field showingField = javafx.scene.control.Dialog.class.getDeclaredField("showing");
                showingField.setAccessible(true);
                showingField.set(this, false);
            } catch (Exception refEx) {
                // 忽略反射异常
            }
            
            // 尝试从父窗口移除
            try {
                if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                    Stage stage = (Stage) getDialogPane().getScene().getWindow();
                    stage.close();
                }
            } catch (Exception ex) {
                // 忽略异常
            }
            
        } catch (Exception e) {
            // 最后的尝试：使用Platform.runLater
            javafx.application.Platform.runLater(() -> {
                try {
                    if (isShowing()) {
                        close();
                    }
                } catch (Exception ex) {
                    // 忽略所有异常
                }
            });
        }
    }
    
    /**
     * 延迟关闭进度对话框
     */
    public void delayedClose(long delayMillis) {
        javafx.animation.PauseTransition delay = 
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMillis));
        delay.setOnFinished(e -> {
            forceCloseImmediate();
        });
        delay.play();
    }
}
