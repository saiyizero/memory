package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.LogDialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.event.ActionEvent;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

// SSH相关导入
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Component
public class PaneTerminalController {
    
    @FXML private javafx.scene.layout.VBox root;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;
    @FXML private TextArea terminalOutput;
    @FXML private TextField commandInput;
    @FXML private Label statusLabel;
    @FXML private Label currentPathLabel;
    @FXML private Label promptLabel;
    
    private SSHClient sshClient;
    private ExecutorService executorService;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private String currentPath = "~";
    private final StringBuilder commandHistory = new StringBuilder();
    private int historyIndex = 0;
    // 外部自动连接后需要切换的目标目录
    private volatile String autoConnectTargetPath = null;
    // 是否按下了快捷键修饰键（Command/Ctrl）或组合键进行中
    private volatile boolean shortcutModifierPressed = false;
    // Tab 补全已移除
    
    @FXML
    public void initialize() {
        executorService = Executors.newCachedThreadPool();
        
        // 设置默认连接信息
        ipField.setText("182.188.5.91");
        portField.setText("22");
        usernameField.setText("logadm");
        passwordField.setText("logadm@123");
        
        // 设置终端输出的样式
        terminalOutput.setStyle(
            "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-background-color: #1e1e1e;" +
            "-fx-text-fill: #d4d4d4;" +
            "-fx-control-inner-background: #1e1e1e;" +
            "-fx-highlight-fill: #264f78;" +
            "-fx-highlight-text-fill: #ffffff;" +
            "-fx-caret-color: #ffffff;"
        );
        
        // 设置命令输入的样式
        commandInput.setStyle(
            "-fx-font-family: 'Monaco', 'Consolas', 'Courier New', monospace;" +
            "-fx-font-size: 13px;" +
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #d4d4d4;" +
            "-fx-prompt-text-fill: #808080;"
        );
        
        // 初始化提示符
        promptLabel.setText("未连接 > ");
        
        // 初始化状态
        updateConnectionStatus(false);

        // 只要终端页面在当前界面中，任意按键都让命令输入框获得焦点（但放行复制等快捷键）
        if (root != null) {
            // 记录是否处于快捷键组合状态，并在非快捷键时引导焦点到输入框
            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (commandInput == null) return;
                KeyCode code = event.getCode();
                // 更新快捷键修饰键状态
                if (event.isShortcutDown() || event.isMetaDown() || event.isControlDown()
                        || code == KeyCode.META || code == KeyCode.CONTROL) {
                    shortcutModifierPressed = true;
                }
                // 忽略纯修饰键与Tab/Windows键
                if (code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT
                        || code == KeyCode.META || code == KeyCode.WINDOWS || code == KeyCode.TAB) {
                    return;
                }
                // 放行快捷键（如 Command/Ctrl + C/A/V/X/...）
                if (shortcutModifierPressed) {
                    return;
                }
                if (!commandInput.isFocused()) {
                    commandInput.requestFocus();
                }
            });
            // 释放修饰键时复位状态
            root.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.META || code == KeyCode.CONTROL) {
                    shortcutModifierPressed = false;
                }
                // 兜底：若未按下任何快捷键修饰键也复位
                if (!event.isMetaDown() && !event.isControlDown()) {
                    shortcutModifierPressed = false;
                }
            });
            // 捕获 KEY_TYPED：仅在非快捷键状态下引导焦点
            root.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                if (commandInput == null) return;
                if (shortcutModifierPressed) {
                    return;
                }
                // 过滤不可见字符（如回车、制表等）
                String ch = event.getCharacter();
                if (ch == null || ch.isEmpty() || "\r".equals(ch) || "\n".equals(ch) || "\t".equals(ch)) {
                    return;
                }
                if (!commandInput.isFocused()) {
                    commandInput.requestFocus();
                }
            });
        }
    }
    
    @FXML
    private void handleConnect() {
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (ip.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty()) {
            ViewUtils.alertForFail("请填写完整的连接信息");
            return;
        }
        
        connectBtn.setDisable(true);
        statusLabel.setText("正在连接...");
        statusLabel.setStyle("-fx-text-fill: #ffa500;");
        
        CompletableFuture.runAsync(() -> {
            try {
                sshClient = new SSHClient();
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
                sshClient.connect(ip, Integer.parseInt(port));
                sshClient.authPassword(username, password);
                
                // 获取初始路径
                updateCurrentPath();

                // 如果指定了自动切换目录，尝试切换
                String targetPath = this.autoConnectTargetPath;
                if (targetPath != null && !targetPath.isEmpty()) {
                    try {
                        handleChangeDirectory(targetPath);
                    } catch (Exception ignore) {
                        // 忽略切换目录异常，保持连接状态
                    } finally {
                        this.autoConnectTargetPath = null;
                    }
                }
                
                Platform.runLater(() -> {
                    updateConnectionStatus(true);
                    appendToTerminal("连接成功！\n");
                    appendToTerminal("欢迎使用远程终端\n");
                    appendToTerminal("当前路径: " + currentPath + "\n");
                    appendToTerminal("输入 'help' 查看可用命令\n\n");
                    scrollToBottom();
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateConnectionStatus(false);
                    ViewUtils.alertForFail("连接失败: " + e.getMessage());
                    appendToTerminal("连接失败: " + e.getMessage() + "\n");
                    scrollToBottom();
                });
            } finally {
                Platform.runLater(() -> connectBtn.setDisable(false));
            }
        }, executorService);
    }
    
    @FXML
    private void handleDisconnect() {
        if (isConnected.get()) {
            try {
                if (sshClient != null) {
                    sshClient.disconnect();
                    sshClient = null;
                }
                updateConnectionStatus(false);
                appendToTerminal("已断开连接\n");
                scrollToBottom();
            } catch (Exception e) {
                ViewUtils.alertForFail("断开连接失败: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleCommandEnter(ActionEvent event) {
        handleSendCommand();
    }
    
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleSendCommand();
        } else if (event.getCode() == KeyCode.TAB) {
            // 阻止默认Tab切换焦点行为，但不做任何补全
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            // 历史命令向上
            navigateHistory(-1);
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            // 历史命令向下
            navigateHistory(1);
            event.consume();
        }
    }
    
    @FXML
    private void handleSendCommand() {
        if (!isConnected.get()) {
            ViewUtils.alertForFail("请先连接到服务器");
            return;
        }
        
        String command = commandInput.getText().trim();
        if (command.isEmpty()) {
            return;
        }
        
        // 添加到历史记录
        commandHistory.append(command).append("\n");
        historyIndex = 0;
        
        // 显示命令（在终端输出中显示完整的命令）
        appendToTerminal(command + "\n");
        
        // 清空输入框
        commandInput.clear();
        
        // 执行命令
        executeCommand(command);
    }
    
    private void executeCommand(String command) {
        CompletableFuture.runAsync(() -> {
            try {
                // 特殊命令处理
                if (command.equals("help")) {
                    showHelp();
                    return;
                } else if (command.equals("clear")) {
                    Platform.runLater(() -> terminalOutput.clear());
                    return;
                } else if (command.startsWith("cd ")) {
                    handleChangeDirectory(command.substring(3).trim());
                    return;
                } else if (command.equals("pwd")) {
                    // 执行pwd命令并显示结果
                    Session session = null;
                    try {
                        session = sshClient.startSession();
                        // 在当前目录下执行pwd
                        String fullCommand = "cd " + currentPath + " && pwd";
                        System.out.println("执行pwd命令: " + fullCommand); // 调试信息
                        Session.Command cmd = session.exec(fullCommand);
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                final String outputLine = line;
                                Platform.runLater(() -> appendToTerminal(outputLine + "\n"));
                            }
                        }
                        cmd.join();
                        Platform.runLater(this::scrollToBottom);
                    } finally {
                        if (session != null) {
                            session.close();
                        }
                    }
                    return;
                } else if (isEditorViewCommand(command)) {
                    // vi/vim/view命令改为在查看器中打开文件
                    handleEditorViewCommand(command);
                    return;
                }
                
                // 为每个命令创建新的会话，并在当前目录下执行
                Session session = null;
                try {
                    session = sshClient.startSession();
                    // 在当前目录下执行命令
                    String fullCommand = "cd " + currentPath + " && " + command;
                    System.out.println("执行命令: " + fullCommand); // 调试信息
                    Session.Command cmd = session.exec(fullCommand);
                    
                    // 读取输出
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line;
                            Platform.runLater(() -> appendToTerminal(outputLine + "\n"));
                        }
                    }
                    
                    // 读取错误输出
                    try (BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(cmd.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            final String errorLine = line;
                            Platform.runLater(() -> appendToTerminal(errorLine + "\n"));
                        }
                    }
                    
                    // 等待命令完成
                    cmd.join();
                    Platform.runLater(this::scrollToBottom);
                    
                } finally {
                    if (session != null) {
                        session.close();
                    }
                }
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendToTerminal("命令执行失败: " + e.getMessage() + "\n");
                    scrollToBottom();
                });
            }
        }, executorService);
    }

    private boolean isEditorViewCommand(String command) {
        String c = command.trim();
        return c.startsWith("vi ") || c.startsWith("vim ") || c.startsWith("view ");
    }

    private String quoteForShell(String path) {
        if (path == null) return "";
        // 简单单引号转义: ' -> '\''
        return "'" + path.replace("'", "'\\''") + "'";
    }


    private void handleEditorViewCommand(String command) {
        // 解析目标文件路径（取最后一个非选项参数）
        String[] parts = command.trim().split("\\s+");
        String target = null;
        for (int i = parts.length - 1; i >= 1; i--) {
            String p = parts[i];
            if (!p.startsWith("-")) { // 忽略选项
                target = p;
                break;
            }
        }
        if (target == null || target.isEmpty()) {
            Platform.runLater(() -> ViewUtils.alertForFail("请指定要查看的文件路径，例如: vi filename"));
            return;
        }

        final String displayName = target;
        // 组合成绝对执行命令（在当前目录下cat该文件）
        String fullCmd;
        if (target.startsWith("/")) {
            fullCmd = "cat " + quoteForShell(target);
        } else {
            fullCmd = "cd " + currentPath + " && cat " + quoteForShell(target);
        }

        CompletableFuture.runAsync(() -> {
            Session session = null;
            try {
                session = sshClient.startSession();
                Session.Command cmd = session.exec(fullCmd);
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append('\n');
                    }
                }
                StringBuilder err = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(cmd.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        err.append(line).append('\n');
                    }
                }
                cmd.join();

                if (err.length() > 0 && content.length() == 0) {
                    Platform.runLater(() -> ViewUtils.alertForFail("读取文件失败: " + err.toString().trim()));
                } else {
                    // 打开日志查看器显示文件内容
                    LogDialogUtil.showLogDialog(content.toString(), "文件内容 - " + displayName, 1000, 700);
                    Platform.runLater(() -> {
                        appendToTerminal("已在查看器中打开文件: " + displayName + "\n");
                        scrollToBottom();
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> ViewUtils.alertForFail("打开文件失败: " + ex.getMessage()));
            } finally {
                if (session != null) {
                    try { session.close(); } catch (IOException ignored) {}
                }
            }
        }, executorService);
    }

    /**
     * 对外提供：设置连接参数并自动连接与切换目录
     */
    public void connectAndCd(String ip, String port, String username, String password, String targetPath) {
        // 记录目标目录，连接成功后进行切换
        this.autoConnectTargetPath = targetPath;
        // 在FX线程设置UI字段并触发连接
        Platform.runLater(() -> {
            if (ipField != null) ipField.setText(ip != null ? ip : "");
            if (portField != null) portField.setText(port != null ? port : "22");
            if (usernameField != null) usernameField.setText(username != null ? username : "");
            if (passwordField != null) passwordField.setText(password != null ? password : "");
            handleConnect();
        });
    }
    
    private void handleChangeDirectory(String path) {
        try {
            // 为cd命令创建新的会话
            Session session = null;
            try {
                session = sshClient.startSession();
                // 处理相对路径和绝对路径
                String targetPath;
                if (path.isEmpty() || path.equals("~")) {
                    // cd 或 cd ~ 回到用户主目录
                    targetPath = "~";
                } else if (path.equals("-")) {
                    // cd - 回到上一个目录（这里简化处理，暂时不支持）
                    Platform.runLater(() -> {
                        appendToTerminal("cd - 功能暂不支持\n");
                        scrollToBottom();
                    });
                    return;
                } else if (path.startsWith("/")) {
                    // 绝对路径
                    targetPath = path;
                } else {
                    // 相对路径，需要从当前路径开始
                    targetPath = currentPath + "/" + path;
                }
                
                String cdCommand = "cd " + targetPath + " && pwd";
                System.out.println("切换目录命令: " + cdCommand); // 调试信息
                Session.Command cmd = session.exec(cdCommand);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                    String newPath = reader.readLine();
                    if (newPath != null) {
                        currentPath = newPath;
                        System.out.println("当前路径更新为: " + currentPath); // 调试信息
                        Platform.runLater(() -> {
                            currentPathLabel.setText(currentPath);
                            promptLabel.setText(currentPath + " $ ");
                            appendToTerminal("当前路径: " + currentPath + "\n");
                            scrollToBottom();
                        });
                    } else {
                        // 如果pwd没有输出，可能是路径不存在
                        Platform.runLater(() -> {
                            appendToTerminal("切换目录失败: 路径不存在或无法访问\n");
                            scrollToBottom();
                        });
                    }
                }
                cmd.join();
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                appendToTerminal("切换目录失败: " + e.getMessage() + "\n");
                scrollToBottom();
            });
        }
    }
    
    private void updateCurrentPath() {
        try {
            // 为pwd命令创建新的会话
            Session session = null;
            try {
                session = sshClient.startSession();
                Session.Command cmd = session.exec("pwd");
                System.out.println("初始化路径命令: pwd"); // 调试信息
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
                    String path = reader.readLine();
                    if (path != null) {
                        currentPath = path;
                        System.out.println("初始化路径为: " + currentPath); // 调试信息
                        Platform.runLater(() -> {
                            currentPathLabel.setText(currentPath);
                            promptLabel.setText(currentPath + " $ ");
                            scrollToBottom();
                        });
                    }
                }
                cmd.join();
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        } catch (Exception e) {
            // 忽略错误，保持当前路径
            System.err.println("获取当前路径失败: " + e.getMessage());
        }
    }
    
    private void showHelp() {
        String helpText = """
            可用命令:
            - help: 显示此帮助信息
            - clear: 清屏
            - cd <path>: 切换目录
            - pwd: 显示当前路径
            - ls: 列出文件
            - cat <file>: 查看文件内容
            - ps: 查看进程
            - top: 系统监控
            - df: 磁盘使用情况
            - free: 内存使用情况
            - netstat: 网络连接状态
            - 其他Linux命令...
            """;
        Platform.runLater(() -> {
            appendToTerminal(helpText);
            scrollToBottom();
        });
    }
    
    private void navigateHistory(int direction) {
        String[] history = commandHistory.toString().split("\n");
        if (history.length > 1) {
            if (direction < 0 && historyIndex < history.length - 1) {
                historyIndex++;
            } else if (direction > 0 && historyIndex > 0) {
                historyIndex--;
            }
            
            if (historyIndex >= 0 && historyIndex < history.length) {
                commandInput.setText(history[history.length - 1 - historyIndex]);
                commandInput.positionCaret(commandInput.getText().length());
            }
        }
    }
    
    private void appendToTerminal(String text) {
        if (terminalOutput != null) {
            terminalOutput.appendText(text);
            // 将光标定位到文本末尾以确保滚动条跟随
            terminalOutput.positionCaret(terminalOutput.getText().length());
            // 基础滚动保证
            terminalOutput.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    private void scrollToBottom() {
        if (terminalOutput != null) {
            terminalOutput.positionCaret(terminalOutput.getText().length());
            terminalOutput.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    private void updateConnectionStatus(boolean connected) {
        isConnected.set(connected);
        Platform.runLater(() -> {
            if (connected) {
                statusLabel.setText("已连接");
                statusLabel.setStyle("-fx-text-fill: #52c41a;");
                connectBtn.setDisable(true);
                disconnectBtn.setDisable(false);
                commandInput.setDisable(false);
                promptLabel.setText(currentPath + " $ ");
            } else {
                statusLabel.setText("未连接");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
                commandInput.setDisable(true);
                promptLabel.setText("未连接 > ");
                currentPathLabel.setText("~");
            }
        });
    }
    
    public void cleanup() {
        handleDisconnect();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 