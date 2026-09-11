package com.murong.ecp.tools.fx.infrastructure.utils;

import lombok.SneakyThrows;

import java.awt.*;
import java.io.File;

public class RxFileUtils {

    @SneakyThrows
    public static boolean openFile(String filePath, String fileName) {
        String localPath=filePath + File.separator + fileName;
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(filePath));
            } catch (UnsupportedOperationException e) {
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", "-R", localPath});
                } else {
                    throw e;
                }
            }
            return true;
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            if (!new File(localPath).exists()) {
                return false;
            }
            Runtime.getRuntime().exec(new String[]{"open", "-R", localPath});
            return true;
        } else {
            return false;
        }
    }
}
