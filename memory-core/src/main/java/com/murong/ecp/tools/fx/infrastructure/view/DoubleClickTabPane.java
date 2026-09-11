package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 可双击分离的标签页管理器
 * 支持双击标签页分离成独立窗口
 */
@Component
public class DoubleClickTabPane extends TabPane {
    
    private static final Map<Tab, Stage> detachedWindows = new HashMap<>();
    
    public DoubleClickTabPane() {
        super();
        setupDoubleClickDetachment();
    }
    
    /**
     * 设置双击分离功能
     */
    private void setupDoubleClickDetachment() {
        // 监听标签页添加事件
        getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        // 延迟设置事件处理器，确保标签页完全初始化
                        Platform.runLater(() -> {
                            setupTabDoubleClickHandler(tab);
                        });
                    }
                }
            }
        });
        
        // 为现有标签页设置双击处理器
        for (Tab tab : getTabs()) {
            setupTabDoubleClickHandler(tab);
        }
    }
    
    /**
     * 为单个标签页设置双击处理器
     */
    private void setupTabDoubleClickHandler(Tab tab) {
        System.out.println("开始为标签页设置双击处理器: " + tab.getText());
        
        // 如果标签页已有内容，立即设置事件处理器
        if (tab.getContent() != null) {
            System.out.println("标签页已有内容，立即设置事件处理器: " + tab.getText());
            setupContentDoubleClickHandler(tab, tab.getContent());
        }
        
        // 监听标签页内容变化，确保内容加载后设置事件处理器
        tab.contentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                System.out.println("标签页内容加载完成: " + tab.getText());
                // 延迟设置事件处理器，确保内容完全加载
                Platform.runLater(() -> {
                    setupContentDoubleClickHandler(tab, newValue);
                });
            }
        });
        
        // 为TabPane本身也添加事件处理器（备用方案）
        this.setOnMouseClicked(event -> {
            System.out.println("TabPane鼠标点击事件触发，点击次数: " + event.getClickCount());
            if (event.getClickCount() == 2) {
                Tab selectedTab = getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    System.out.println("TabPane双击检测到，分离标签页: " + selectedTab.getText());
                    startDoubleClickDetachment(selectedTab, event);
                }
            }
        });
    }
    
    /**
     * 为内容节点设置双击处理器
     */
    private void setupContentDoubleClickHandler(Tab tab, Node content) {
        System.out.println("为内容节点设置双击处理器: " + tab.getText());
        
        // 为整个内容区域添加双击事件处理器
        content.setOnMouseClicked(event -> {
            System.out.println("鼠标点击事件触发，点击次数: " + event.getClickCount() + ", 标签页: " + tab.getText());
            
            // 暂时注释掉交互元素检测，让所有双击都能触发分离
            /*
            // 检查点击的节点是否是表单元素或其他交互元素
            Node target = (Node) event.getTarget();
            if (isInteractiveElement(target)) {
                System.out.println("点击的是交互元素，不处理双击分离: " + target.getClass().getSimpleName());
                return;
            }
            */
            
            if (event.getClickCount() == 2) {
                System.out.println("检测到双击，开始分离标签页: " + tab.getText());
                event.consume(); // 阻止事件冒泡
                startDoubleClickDetachment(tab, event);
            }
        });
    }
    
    /**
     * 检查节点是否是交互元素
     */
    private boolean isInteractiveElement(Node node) {
        if (node == null) return false;
        
        String className = node.getClass().getSimpleName();
        System.out.println("检查节点类型: " + className);
        
        // 检查是否是表单控件或其他交互元素
        boolean isInteractive = className.contains("TextField") ||
               className.contains("TextArea") ||
               className.contains("Button") ||
               className.contains("ComboBox") ||
               className.contains("TableView") ||
               className.contains("TableRow") ||
               className.contains("TableCell") ||
               className.contains("CheckBox") ||
               className.contains("RadioButton") ||
               className.contains("Slider") ||
               className.contains("ScrollBar") ||
               className.contains("ScrollPane") ||
               className.contains("TabPane") ||
               className.contains("Tab") ||
               className.contains("Menu") ||
               className.contains("MenuItem") ||
               className.contains("ContextMenu") ||
               className.contains("Tooltip") ||
               className.contains("Hyperlink") ||
               (className.contains("Label") && node.getStyleClass().contains("clickable"));
        
        if (isInteractive) {
            System.out.println("检测到交互元素: " + className);
        }
        
        return isInteractive;
    }
    
    /**
     * 开始双击分离
     */
    public void startDoubleClickDetachment(Tab tab, MouseEvent event) {
        System.out.println("双击检测到，开始分离标签页: " + tab.getText());
        
        if (tab == null || getTabs().size() <= 1) {
            System.out.println("无法分离：标签页为空或只有一个标签页");
            return;
        }
        
        // 保存原始内容
        Node originalContent = tab.getContent();
        
        // 创建独立窗口
        Stage detachedStage = createDetachedWindow(tab, event);
        if (detachedStage != null) {
            // 从原标签页移除
            getTabs().remove(tab);
            detachedWindows.put(tab, detachedStage);
            
            // 显示独立窗口
            detachedStage.show();
            System.out.println("标签页分离成功: " + tab.getText());
        } else {
            // 如果创建失败，恢复原始内容
            tab.setContent(originalContent);
            System.out.println("标签页分离失败: " + tab.getText());
        }
    }
    
    /**
     * 创建分离的独立窗口
     */
    private Stage createDetachedWindow(Tab tab, MouseEvent event) {
        try {
            // 创建新窗口
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle(tab.getText());
            stage.setWidth(800);
            stage.setHeight(600);
            
            // 设置窗口位置（在鼠标位置附近）
            stage.setX(event.getScreenX() - 100);
            stage.setY(event.getScreenY() - 50);
            
            // 创建窗口内容
            BorderPane root = new BorderPane();
            
            // 创建窗口标题栏
            DetachedWindowTitleBar titleBar = new DetachedWindowTitleBar(tab, stage, this);
            root.setTop(titleBar);
            
            // 设置标签页内容（分离的窗口不再支持双击分离）
            Node content = tab.getContent();
            if (content != null) {
                // 移除双击事件处理器，确保分离的窗口不能再分离
                content.setOnMouseClicked(null);
                root.setCenter(content);
            }
            
            // 创建场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/detached-window.css").toExternalForm());
            stage.setScene(scene);
            
            // 窗口关闭事件
            stage.setOnCloseRequest(e -> {
                // 将标签页重新添加回原标签页管理器
                reattachTab(tab);
            });
            
            return stage;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 重新附加标签页到原标签页管理器
     */
    public void reattachTab(Tab tab) {
        if (tab != null && detachedWindows.containsKey(tab)) {
            Stage stage = detachedWindows.get(tab);
            if (stage != null) {
                // 从独立窗口中恢复标签页内容
                Scene scene = stage.getScene();
                if (scene != null && scene.getRoot() instanceof BorderPane) {
                    BorderPane root = (BorderPane) scene.getRoot();
                    Node content = root.getCenter();
                    if (content != null) {
                        tab.setContent(content);
                    }
                }
                
                stage.close();
                detachedWindows.remove(tab);
            }
            
            // 重新设置标签页的双击处理器
            setupTabDoubleClickHandler(tab);
            
            // 添加回标签页管理器
            if (!getTabs().contains(tab)) {
                getTabs().add(tab);
                getSelectionModel().select(tab);
            }
        }
    }
    
    /**
     * 获取分离的窗口映射
     */
    public static Map<Tab, Stage> getDetachedWindows() {
        return detachedWindows;
    }
    
    /**
     * 关闭所有分离的窗口
     */
    public void closeAllDetachedWindows() {
        for (Stage stage : detachedWindows.values()) {
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        }
        detachedWindows.clear();
    }
} 