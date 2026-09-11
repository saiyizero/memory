package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 数据库配置对话框管理器
 */
@Component
public class DatabaseConfigDialog {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 显示数据库配置窗口并等待用户配置
     * @param ownerStage 父窗口
     * @return 配置是否成功
     */
    public boolean showDatabaseConfigDialog(Stage ownerStage) {
        try {
            // 加载FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/database_config_dialog.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            // 获取控制器
            com.murong.ecp.tools.fx.infrastructure.controller.DatabaseConfigDialogController controller = fxmlLoader.getController();

            // 创建配置窗口
            Stage configStage = new Stage();
            configStage.initOwner(ownerStage);
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

            // 返回配置结果
            return controller.isConfigSuccess();

        } catch (IOException e) {
            System.err.println("加载数据库配置窗口失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
