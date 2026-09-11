package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.controller.LoginDialogController;
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
 * 登录窗口管理器
 */
@Component
public class LoginDialog {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 显示登录窗口并等待用户登录
     * @param ownerStage 父窗口
     * @return 登录是否成功
     */
    public boolean showLoginDialog(Stage ownerStage) {
        try {
            // 加载FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/login_dialog.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent root = fxmlLoader.load();

            // 获取控制器
            LoginDialogController controller = fxmlLoader.getController();

            // 创建登录窗口
            Stage loginStage = new Stage();
            loginStage.initOwner(ownerStage);
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.initStyle(StageStyle.UTILITY);
            loginStage.setTitle("用户登录");
            loginStage.setResizable(false);

            // 设置场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/login-dialog.css").toExternalForm());
            loginStage.setScene(scene);

            // 设置控制器
            controller.setDialogStage(loginStage);

            // 显示窗口并等待关闭
            loginStage.showAndWait();

            // 返回登录结果
            return controller.isLoginSuccess();

        } catch (IOException e) {
            System.err.println("加载登录窗口失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
