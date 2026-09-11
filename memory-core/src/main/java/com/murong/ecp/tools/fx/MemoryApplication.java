package com.murong.ecp.tools.fx;

import com.murong.ecp.tools.fx.infrastructure.config.JavaFXRunner;
import com.murong.ecp.tools.fx.infrastructure.view.NativeSplashScreen;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MemoryApplication {
    public static void main(String[] args) {
        // 在JavaFX启动之前就显示原生启动画面
        System.out.println("[主程序] 开始启动Memory应用...");
        NativeSplashScreen.show();
        NativeSplashScreen.updateStatus("正在启动JavaFX...");
        
        // 直接启动JavaFX应用，不使用预加载器
        Application.launch(JavaFXRunner.class, args);
    }
} 