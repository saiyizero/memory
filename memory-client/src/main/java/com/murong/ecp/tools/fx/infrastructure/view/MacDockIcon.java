package com.murong.ecp.tools.fx.infrastructure.view;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.InputStream;

/**
 * macOS Dock 图标：在 AWT/JavaFX 初始化后覆盖默认咖啡图标。
 */
public final class MacDockIcon {

    private static final String ICON_PATH = "/image/start-logo.png";

    private MacDockIcon() {
    }

    public static void apply() {
        if (!isMac()) {
            return;
        }
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            Taskbar taskbar = Taskbar.getTaskbar();
            if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                return;
            }
            Image image = loadImage();
            if (image != null) {
                taskbar.setIconImage(image);
            }
        } catch (Throwable e) {
            System.err.println("[Dock] 设置图标失败: " + e.getMessage());
        }
    }

    private static Image loadImage() {
        try (InputStream in = MacDockIcon.class.getResourceAsStream(ICON_PATH)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
