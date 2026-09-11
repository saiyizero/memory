package com.murong.ecp.tools.fx.infrastructure.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;

/**
 * 原生启动画面
 * 在JavaFX启动之前就显示，避免空白等待时间
 */
public class NativeSplashScreen {
    
    private static JWindow splashWindow;
    private static JProgressBar progressBar;
    private static JLabel statusLabel;
    private static JLabel progressLabel;
    private static Timer progressTimer;
    private static int currentProgress = 0;
    
    private static final int SPLASH_WIDTH = 500;
    private static final int SPLASH_HEIGHT = 300;
    private static final String APP_NAME = "Memory";
    private static final String VERSION = "v2.0.3";
    
    public static void show() {
        try {
            // 设置系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 忽略外观设置错误
        }
        
        // 创建无边框窗口
        splashWindow = new JWindow();
        splashWindow.setSize(SPLASH_WIDTH, SPLASH_HEIGHT);
        splashWindow.setLocationRelativeTo(null);
        
        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(44, 62, 80));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // 创建LOGO
        JLabel logoLabel = new JLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setPreferredSize(new Dimension(80, 80));
        logoLabel.setMaximumSize(new Dimension(80, 80));
        logoLabel.setMinimumSize(new Dimension(80, 80));
        
        try {
            // 尝试加载logo图片
            InputStream logoStream = NativeSplashScreen.class.getResourceAsStream("/image/login-logo.png");
            if (logoStream != null) {
                ImageIcon logoIcon = new ImageIcon(logoStream.readAllBytes());
                Image scaledImage = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                // 使用默认图标
                logoLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            }
        } catch (Exception e) {
            // 使用默认图标
            logoLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        }
        
        // 应用名称
        JLabel appNameLabel = new JLabel(APP_NAME);
        appNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        appNameLabel.setFont(new Font("System", Font.BOLD, 28));
        appNameLabel.setForeground(Color.WHITE);
        
        // 版本信息
        JLabel versionLabel = new JLabel(VERSION);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setFont(new Font("System", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(189, 195, 199));
        
        // 状态标签
        statusLabel = new JLabel("正在启动应用...");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("System", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        
        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setMinimumSize(new Dimension(400, 8));
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(52, 73, 94));
        progressBar.setForeground(new Color(52, 152, 219));
        
        // 进度百分比标签
        progressLabel = new JLabel("0%");
        progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressLabel.setFont(new Font("System", Font.BOLD, 12));
        progressLabel.setForeground(Color.WHITE);
        
        // 版权信息
        JLabel copyrightLabel = new JLabel("© 2025 Memory Team. All rights reserved.");
        copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        copyrightLabel.setFont(new Font("System", Font.PLAIN, 10));
        copyrightLabel.setForeground(new Color(149, 165, 166));
        
        // 组装界面
        mainPanel.add(logoLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(appNameLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(versionLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(progressLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(copyrightLabel);
        
        splashWindow.setContentPane(mainPanel);
        splashWindow.setVisible(true);
        
        // 确保窗口在最前面
        splashWindow.setAlwaysOnTop(true);
        
        // 启动进度条动画
        startProgressAnimation();
        
        System.out.println("[原生启动画面] 启动画面已显示");
    }
    
    /**
     * 启动进度条动画，每隔一秒增加30%
     */
    private static void startProgressAnimation() {
        currentProgress = 0;
        progressTimer = new Timer(1000, e -> {
            currentProgress += 30;
            if (currentProgress > 100) {
                currentProgress = 100;
                progressTimer.stop();
            }
            
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(currentProgress);
                progressLabel.setText(currentProgress + "%");
                
                // 根据进度更新状态信息
                updateStatusByProgress(currentProgress);
            });
        });
        progressTimer.start();
    }
    
    /**
     * 根据进度更新状态信息
     */
    private static void updateStatusByProgress(int progress) {
        String status;
        if (progress <= 30) {
            status = "正在初始化系统...";
        } else if (progress <= 60) {
            status = "正在加载组件...";
        } else if (progress <= 90) {
            status = "正在启动主界面...";
        } else {
            status = "启动完成";
        }
        updateStatus(status);
    }
    
    public static void updateProgress(int progress) {
        if (progressBar != null) {
            // 停止自动动画
            if (progressTimer != null && progressTimer.isRunning()) {
                progressTimer.stop();
            }
            
            currentProgress = progress;
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(progress);
                progressLabel.setText(progress + "%");
            });
        }
    }
    
    public static void updateStatus(String status) {
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(status);
                System.out.println("[原生启动画面] " + status);
            });
        }
    }
    
    public static void hide() {
        if (progressTimer != null && progressTimer.isRunning()) {
            progressTimer.stop();
        }
        
        if (splashWindow != null) {
            SwingUtilities.invokeLater(() -> {
                splashWindow.dispose();
                splashWindow = null;
            });
        }
    }
    
    public static void complete() {
        // 停止自动动画
        if (progressTimer != null && progressTimer.isRunning()) {
            progressTimer.stop();
        }
        
        updateProgress(100);
        updateStatus("启动完成");
        
        // 短暂延迟后隐藏启动画面，让过渡更平滑
        Timer timer = new Timer(200, e -> {
            hide();
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }
} 