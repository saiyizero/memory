package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;

public class ToggleSwitch extends Control {
    
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    
    public ToggleSwitch() {
        getStyleClass().add("toggle-switch");
        setPrefSize(40, 20);
        setMinSize(40, 20);
        setMaxSize(40, 20);
    }
    
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    @Override
    protected Skin<?> createDefaultSkin() {
        return new ToggleSwitchSkin(this);
    }
    
    private static class ToggleSwitchSkin extends SkinBase<ToggleSwitch> {
        
        private final Rectangle background;
        private final Circle thumb;
        private final StackPane container;
        
        public ToggleSwitchSkin(ToggleSwitch toggleSwitch) {
            super(toggleSwitch);
            
            // 创建背景轨道
            background = new Rectangle();
            background.setWidth(36);
            background.setHeight(16);
            background.setArcWidth(16);
            background.setArcHeight(16);
            background.setFill(Color.LIGHTGRAY);
            
            // 创建滑块
            thumb = new Circle();
            thumb.setRadius(6);
            thumb.setFill(Color.WHITE);
            thumb.setStroke(Color.LIGHTGRAY);
            thumb.setStrokeWidth(1);
            
            // 创建容器
            container = new StackPane();
            container.getChildren().addAll(background, thumb);
            container.setPrefSize(40, 20);
            
            // 添加点击事件
            container.setOnMouseClicked(e -> {
                toggleSwitch.setSelected(!toggleSwitch.isSelected());
            });
            
            // 监听选中状态变化
            toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateVisualState(newVal);
            });
            
            // 初始化状态
            updateVisualState(toggleSwitch.isSelected());
            
            getChildren().add(container);
        }
        
        private void updateVisualState(boolean selected) {
            if (selected) {
                background.setFill(Color.rgb(0, 120, 212)); // 蓝色
                thumb.setTranslateX(10);
            } else {
                background.setFill(Color.LIGHTGRAY);
                thumb.setTranslateX(-10);
            }
        }
    }
} 