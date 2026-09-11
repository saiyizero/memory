package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * 独立窗口的标题栏组件
 * 包含窗口标题和控制按钮（最小化、最大化、关闭）
 */
public class DetachedWindowTitleBar extends HBox {
    
    private final Tab tab;
    private final Stage stage;
    private final DoubleClickTabPane originalTabPane;
    private boolean isMaximized = false;
    private double originalX, originalY, originalWidth, originalHeight;
    
    public DetachedWindowTitleBar(Tab tab, Stage stage, DoubleClickTabPane originalTabPane) {
        this.tab = tab;
        this.stage = stage;
        this.originalTabPane = originalTabPane;
        
        setupTitleBar();
    }
    
    /**
     * 设置标题栏
     */
    private void setupTitleBar() {
        getStyleClass().add("detached-title-bar");
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        
        // 窗口标题
        Label titleLabel = new Label(tab.getText());
        titleLabel.getStyleClass().add("detached-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 控制按钮容器
        HBox controlButtons = new HBox(4);
        controlButtons.setAlignment(Pos.CENTER_RIGHT);
        controlButtons.getStyleClass().add("control-buttons");
        
        // 最小化按钮
        Button minimizeBtn = createControlButton("−", "minimize");
        minimizeBtn.setOnAction(e -> minimizeWindow());
        
        // 最大化/还原按钮
        Button maximizeBtn = createControlButton("□", "maximize");
        maximizeBtn.setOnAction(e -> toggleMaximize());
        
        // 关闭按钮
        Button closeBtn = createControlButton("×", "close");
        closeBtn.setOnAction(e -> closeWindow());
        
        // 重新附加按钮（将窗口重新合并到主窗口）
        Button reattachBtn = createControlButton("⊞", "reattach");
        reattachBtn.setOnAction(e -> reattachToMainWindow());
        
        controlButtons.getChildren().addAll(minimizeBtn, maximizeBtn, reattachBtn, closeBtn);
        
        getChildren().addAll(titleLabel, controlButtons);
        
        // 设置拖拽功能
        setupDragFunctionality();
    }
    
    /**
     * 创建控制按钮
     */
    private Button createControlButton(String text, String buttonType) {
        Button button = new Button(text);
        button.getStyleClass().addAll("control-button", buttonType + "-button");
        
        return button;
    }
    
    /**
     * 设置拖拽功能
     */
    private void setupDragFunctionality() {
        setOnMousePressed(e -> {
            originalX = e.getScreenX() - stage.getX();
            originalY = e.getScreenY() - stage.getY();
        });
        
        setOnMouseDragged(e -> {
            if (!isMaximized) {
                stage.setX(e.getScreenX() - originalX);
                stage.setY(e.getScreenY() - originalY);
            }
        });
        
        // 双击最大化/还原
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }
    
    /**
     * 最小化窗口
     */
    private void minimizeWindow() {
        stage.setIconified(true);
    }
    
    /**
     * 切换最大化/还原
     */
    private void toggleMaximize() {
        if (isMaximized) {
            // 还原窗口
            stage.setX(originalX);
            stage.setY(originalY);
            stage.setWidth(originalWidth);
            stage.setHeight(originalHeight);
            stage.setMaximized(false);
            isMaximized = false;
        } else {
            // 保存当前位置和大小
            originalX = stage.getX();
            originalY = stage.getY();
            originalWidth = stage.getWidth();
            originalHeight = stage.getHeight();
            
            // 最大化窗口
            stage.setMaximized(true);
            isMaximized = true;
        }
    }
    
    /**
     * 关闭窗口
     */
    private void closeWindow() {
        stage.close();
    }
    
    /**
     * 重新附加到主窗口
     */
    private void reattachToMainWindow() {
        // 确保只有主窗口的标签页才能重新附加
        if (originalTabPane != null) {
            originalTabPane.reattachTab(tab);
        }
    }
} 