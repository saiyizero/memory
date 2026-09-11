package com.murong.ecp.tools.fx.infrastructure.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class ViewUtils {

    public static Optional<ButtonType> alertForAsk(String title,String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        // 设置对话框大小，让问号图标尽量调小，但支持拖拽调整大小
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 150);
        alert.getDialogPane().setMinSize(350, 120);
        alert.getDialogPane().setMaxSize(800, 400);
        
        // 查找主窗口Stage作为owner
        Window owner = null;
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                owner = window;
                break;
            }
        }
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置对话框样式，减小图标尺寸
        alert.getDialogPane().setStyle(
            "-fx-graphic-size: 16px;" +  // 减小图标尺寸
            "-fx-font-size: 12px;" +     // 减小字体
            "-fx-padding: 10px;"         // 减小内边距
        );
        
        // 在显示对话框后设置置顶
        alert.setOnShown(event -> {
            try {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alertStage.setAlwaysOnTop(true);
                
                // 设置初始大小，但允许调整
                alertStage.setWidth(400);
                alertStage.setHeight(150);
                alertStage.setResizable(true);
                
                // 设置最小尺寸
                alertStage.setMinWidth(350);
                alertStage.setMinHeight(120);
            } catch (Exception e) {
                System.err.println("设置置顶失败: " + e.getMessage());
            }
        });
        return alert.showAndWait();
    }

    public static void alertForSucess(String contentText) {
        // 确保在JavaFX应用线程中执行
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(() -> alertForSucess(contentText));
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("成功");
        alert.setHeaderText(null);
        
        // 处理空字符串或null的情况
        String displayText = contentText;
        if (contentText == null || contentText.trim().isEmpty()) {
            displayText = "操作成功";
        }
        
        // 动态计算对话框尺寸
        int[] dimensions = calculateDialogDimensions(displayText);
        int prefWidth = dimensions[0];
        int prefHeight = dimensions[1];
        
        // 使用TextArea来显示内容，支持文本选择
        TextArea textArea = new TextArea(displayText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(3);
        textArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Microsoft YaHei', 'SimSun', sans-serif;");
        
        // 设置GridPane来容纳TextArea
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(textArea, 0, 0);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        
        alert.getDialogPane().setContent(gridPane);
        
        // 强制添加确定按钮
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);
        
        alert.initModality(Modality.APPLICATION_MODAL);
        
        // 设置对话框大小，根据内容动态调整，但支持拖拽调整大小
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(prefWidth, prefHeight);
        alert.getDialogPane().setMinSize(Math.max(400, prefWidth - 50), Math.max(150, prefHeight - 30));
        alert.getDialogPane().setMaxSize(Math.max(900, prefWidth + 100), Math.max(500, prefHeight + 100));
        
        // 查找主窗口Stage作为owner
        Window owner = null;
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                owner = window;
                break;
            }
        }
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置样式使文本可以被选中和复制
        alert.getDialogPane().getStylesheets().add(ViewUtils.class.getResource("/css/alert-selectable.css").toExternalForm());
        
        // 设置对话框样式，减小图标尺寸
        alert.getDialogPane().setStyle(
            "-fx-graphic-size: 18px;" +  // 减小图标尺寸
            "-fx-font-size: 12px;" +     // 减小字体
            "-fx-padding: 12px;"         // 减小内边距
        );
        
        // 设置按钮样式，缩小确定按钮
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
            "-fx-font-size: 11px;" +           // 减小按钮字体
            "-fx-padding: 6px 12px;" +         // 减小按钮内边距
            "-fx-min-width: 60px;" +           // 设置最小宽度
            "-fx-min-height: 24px;" +          // 设置最小高度
            "-fx-pref-width: 70px;" +          // 设置首选宽度
            "-fx-pref-height: 26px;"           // 设置首选高度
        );
        
        // 在显示对话框后设置置顶
        alert.setOnShown(event -> {
            try {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alertStage.setAlwaysOnTop(true);
                
                // 设置初始大小，但允许调整
                alertStage.setWidth(prefWidth);
                alertStage.setHeight(prefHeight);
                alertStage.setResizable(true);
                
                // 设置最小尺寸
                alertStage.setMinWidth(Math.max(400, prefWidth - 50));
                alertStage.setMinHeight(Math.max(150, prefHeight - 30));
            } catch (Exception e) {
                System.err.println("设置置顶失败: " + e.getMessage());
            }
        });
        alert.showAndWait();
    }

    public static void alertForFail(String contentText) {
        // 确保在JavaFX应用线程中执行
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(() -> alertForFail(contentText));
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("失败");
        alert.setHeaderText(null);
        
        // 处理空字符串或null的情况
        String displayText = contentText;
        if (contentText == null || contentText.trim().isEmpty()) {
            displayText = "操作失败，请检查错误日志";
        }
        
        // 动态计算对话框尺寸
        int[] dimensions = calculateDialogDimensions(displayText);
        int prefWidth = dimensions[0];
        int prefHeight = dimensions[1];
        
        // 使用TextArea来显示内容，支持文本选择
        TextArea textArea = new TextArea(displayText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(3);
        textArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Microsoft YaHei', 'SimSun', sans-serif;");
        
        // 设置GridPane来容纳TextArea
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(textArea, 0, 0);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        
        alert.getDialogPane().setContent(gridPane);
        
        // 强制添加确定按钮
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);
        
        alert.initModality(Modality.APPLICATION_MODAL);
        
        // 设置对话框大小，根据内容动态调整，但支持拖拽调整大小
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(prefWidth, prefHeight);
        alert.getDialogPane().setMinSize(Math.max(400, prefWidth - 50), Math.max(150, prefHeight - 30));
        alert.getDialogPane().setMaxSize(Math.max(900, prefWidth + 100), Math.max(500, prefHeight + 100));
        
        // 查找主窗口Stage作为owner
        Window owner = null;
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                owner = window;
                break;
            }
        }
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置样式使文本可以被选中和复制
        alert.getDialogPane().getStylesheets().add(ViewUtils.class.getResource("/css/alert-selectable.css").toExternalForm());
        
        // 设置对话框样式，减小图标尺寸
        alert.getDialogPane().setStyle(
            "-fx-graphic-size: 18px;" +  // 减小图标尺寸
            "-fx-font-size: 12px;" +     // 减小字体
            "-fx-padding: 12px;"         // 减小内边距
        );
        
        // 设置按钮样式，缩小确定按钮
        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
            "-fx-font-size: 11px;" +           // 减小按钮字体
            "-fx-padding: 6px 12px;" +         // 减小按钮内边距
            "-fx-min-width: 60px;" +           // 设置最小宽度
            "-fx-min-height: 24px;" +          // 设置最小高度
            "-fx-pref-width: 70px;" +          // 设置首选宽度
            "-fx-pref-height: 26px;"           // 设置首选高度
        );
        
        // 在显示对话框后设置置顶
        alert.setOnShown(event -> {
            try {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                alertStage.setAlwaysOnTop(true);
                
                // 设置初始大小，但允许调整
                alertStage.setWidth(prefWidth);
                alertStage.setHeight(prefHeight);
                alertStage.setResizable(true);
                
                // 设置最小尺寸
                alertStage.setMinWidth(Math.max(400, prefWidth - 50));
                alertStage.setMinHeight(Math.max(150, prefHeight - 30));
            } catch (Exception e) {
                System.err.println("设置置顶失败: " + e.getMessage());
            }
        });
        
        alert.showAndWait();
    }
    
    /**
     * 根据文本内容动态计算对话框尺寸
     * @param contentText 文本内容
     * @return int数组 [宽度, 高度]
     */
    private static int[] calculateDialogDimensions(String contentText) {
        if (contentText == null || contentText.trim().isEmpty()) {
            return new int[]{450, 180}; // 默认尺寸
        }
        
        // 分割文本为行
        String[] lines = contentText.split("\n");
        int maxLineLength = 0;
        
        // 计算最长行的字符数
        for (String line : lines) {
            // 计算中文字符和英文字符的混合长度
            int lineLength = calculateMixedStringLength(line);
            maxLineLength = Math.max(maxLineLength, lineLength);
        }
        
        // 基础尺寸
        int baseWidth = 450;
        int baseHeight = 180;
        
        // 根据最长行调整宽度
        int calculatedWidth = baseWidth;
        if (maxLineLength > 50) {
            // 每增加10个字符，宽度增加50px
            int extraWidth = ((maxLineLength - 50) / 10 + 1) * 50;
            calculatedWidth = Math.min(baseWidth + extraWidth, 800); // 最大宽度800px
        }
        
        // 根据行数调整高度
        int calculatedHeight = baseHeight;
        if (lines.length > 3) {
            // 每增加2行，高度增加30px
            int extraHeight = ((lines.length - 3) / 2 + 1) * 30;
            calculatedHeight = Math.min(baseHeight + extraHeight, 400); // 最大高度400px
        }
        
        return new int[]{calculatedWidth, calculatedHeight};
    }
    
    /**
     * 计算混合字符串的显示长度（中文字符按2个字符计算）
     * @param str 字符串
     * @return 显示长度
     */
    private static int calculateMixedStringLength(String str) {
        if (str == null) return 0;
        
        int length = 0;
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                length += 2; // 中文字符按2个字符计算
            } else {
                length += 1; // 英文字符按1个字符计算
            }
        }
        return length;
    }
    
    /**
     * 验证项目配置是否完整
     * @param globalProperties 全局配置对象
     * @return 如果配置完整返回true，否则返回false
     */
    public static boolean validateProjectConfiguration(com.murong.ecp.tools.fx.domain.entity.GlobalProperties globalProperties) {
        if (globalProperties == null) {
            alertForFail("项目配置未初始化，请先配置项目信息");
            return false;
        }
        
        String groupName = globalProperties.getGroupName();
        String projectName = globalProperties.getProjectName();
        
        if (groupName == null || groupName.trim().isEmpty()) {
            alertForFail("项目组名称为空，请先在项目配置中设置组名称");
            return false;
        }
        
        if (projectName == null || projectName.trim().isEmpty()) {
            alertForFail("项目名称为空，请先在项目配置中设置项目名称");
            return false;
        }
        
        return true;
    }
}
