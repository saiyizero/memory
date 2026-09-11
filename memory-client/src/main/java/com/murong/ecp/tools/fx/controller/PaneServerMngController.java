package com.murong.ecp.tools.fx.controller;


import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.ServiceInfoEntity;
import com.murong.ecp.tools.fx.domain.service.terminal.SShDomainService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServerInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ServerInfoPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.LogDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PaneServerMngController implements Initializable {
    @Autowired
    private GlobalProperties globalProps;
    @Autowired
    private ServerInfoRpcService serverInfoRpcService;
    @Autowired
    private UserProjSettingRpcService UserProjSettingRpcService;
    @Autowired
    private SShDomainService sshDomainService;
    @FXML
    private VBox serverConfigVBox;
    @FXML
    private ComboBox<String> environmentCombo;
    @FXML
    private Button addServerBtn;
    @FXML
    private Button addMicroServiceBtn;
    
    // 搜索表单字段
    @FXML
    private ComboBox<String> searchTypeCombo;
    @FXML
    private ComboBox<String> searchMicroServiceCombo;
    @FXML
    private TextField searchDayField;
    @FXML
    private TextField searchContentField;
    @FXML
    private Button searchBtn;
    @FXML
    private Button clearSearchBtn;
    
    // 搜索结果表格
    @FXML
    private TableView<Map<String, String>> searchResultTable;
    
    // 当前搜索的服务器列表
    private List<ServerInfoPO> currentSearchServerList = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }

        environmentCombo.setPrefWidth(100);
        environmentCombo.getItems().clear();
        environmentCombo.getItems().addAll("请选择","dev", "sit", "uat", "poc");
        environmentCombo.getSelectionModel().selectFirst();

        // 初始化搜索结果表格
        initializeSearchResultTable();
        
        // 初始化搜索表单字段
        initializeSearchFormFields();

        // 初始化添加微服务按钮事件
        addMicroServiceBtn.setOnAction(e -> {
            showAddMicroServiceDialog();
        });



        // 延迟加载服务器列表，避免在initialize阶段出现依赖注入问题
        Platform.runLater(() -> {
            try {
                // 调用sshDomainService获取所有服务器分组
                String groupName = globalProps.getGroupName();
                List<ServiceInfoEntity> serviceList = sshDomainService.getServiceList(groupName);
                // 初始化服务器列表
                refreshServerList(serviceList, globalProps.getGroupName());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("加载服务器列表失败: " + e.getMessage());
                // 显示错误信息给用户
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("加载失败");
                    alert.setHeaderText("服务器列表加载失败");
                    alert.setContentText("错误信息: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });



        addServerBtn.setOnAction(e -> {
            // 新增服务器弹窗
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("同步服务器");
            dialog.setHeaderText("请输入服务器信息");
            ButtonType okBtnType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okBtnType, ButtonType.CANCEL);
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            TextField ipField = new TextField();
            ipField.setPromptText("IP");
            TextField portField = new TextField("22");
            portField.setPromptText("端口");
            TextField userField = new TextField();
            userField.setPromptText("用户名");
            TextField pwdField = new TextField();
            pwdField.setPromptText("密码（明文）");
            TextField pathField = new TextField();
            pathField.setPromptText("扫描路径");
            grid.add(new javafx.scene.control.Label("IP:"), 0, 0);
            grid.add(ipField, 1, 0);
            grid.add(new javafx.scene.control.Label("端口:"), 0, 1);
            grid.add(portField, 1, 1);
            grid.add(new javafx.scene.control.Label("用户名:"), 0, 2);
            grid.add(userField, 1, 2);
            grid.add(new javafx.scene.control.Label("密码:"), 0, 3);
            grid.add(pwdField, 1, 3);
            grid.add(new javafx.scene.control.Label("扫描路径:"), 0, 4);
            grid.add(pathField, 1, 4);
            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okBtnType) {
                    Map<String, String> result = new HashMap<>();
                    result.put("ip", ipField.getText());
                    result.put("port", portField.getText());
                    result.put("user", userField.getText());
                    result.put("pwd", pwdField.getText());
                    result.put("path", pathField.getText());
                    return result;
                }
                return null;
            });
            dialog.showAndWait().ifPresent(input -> {
                String ip = input.get("ip");
                String port = input.get("port");
                String user = input.get("user");
                String pwd = input.get("pwd");
                String scanPath = input.get("path");

                Platform.runLater(() -> {
                    Dialog<Void> progressDialog = new Dialog<>();
                    progressDialog.setTitle("正在同步服务器...");
                    progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
                    ProgressBar progressBar = new ProgressBar(0);
                    progressBar.setPrefWidth(400);
                    Label progressLabel = new Label("正在同步服务器...");
                    VBox vbox = new VBox(16, progressLabel, progressBar);
                    vbox.setPrefWidth(420);
                    vbox.setAlignment(javafx.geometry.Pos.CENTER);
                    progressDialog.getDialogPane().setContent(vbox);
                    progressDialog.setResizable(false);
                    progressDialog.show();

                    new Thread(() -> {
                        try {
                            sshDomainService.syncServer(ip, port, user, pwd, scanPath, globalProps.getGroupName());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                progressDialog.close();
                                ViewUtils.alertForFail("SSH登录失败: " + ex.getMessage());
                            });
                            return;
                        }
                        // 完成后关闭进度条，弹窗提示，并刷新页面
                        Platform.runLater(() -> {
                            progressDialog.close();
                            ViewUtils.alertForSucess("同步完成");
                            List<ServiceInfoEntity> newServiceList = sshDomainService.getServiceList(globalProps.getGroupName());
                            refreshServerList(newServiceList, globalProps.getGroupName());
                        });
                    }).start();
                });
            });
        });

        // 环境下拉框联动刷新服务器列表
        environmentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            String env = environmentCombo.getValue();
            ServerInfoPO query = new ServerInfoPO();
            if (env != null && !"请选择".equals(env)) {
                query.setEnvName(env);
            }

            List<ServerInfoPO> serverList = serverInfoRpcService.queryForList(query);
            refreshServerList(convertServerInfoListToServiceList(serverList), globalProps.getGroupName());
        });
    }

    private void refreshServerList(List<ServiceInfoEntity> serviceList, String group) {
        serverConfigVBox.getChildren().clear();
        serviceList.stream()
                .filter(e -> e.getGroupName().equals(group))
                .forEach(entity -> {
                    List<ServiceInfoEntity.MicroServiceInfo> msList = entity.getMicroServiceList();
                    if (msList.isEmpty()) return;
                    Node titleBox = buildServerTitleFromEntity(entity);
                    javafx.scene.layout.GridPane microServiceGrid = new javafx.scene.layout.GridPane();
                    microServiceGrid.setHgap(10);
                    microServiceGrid.setVgap(6);
                    microServiceGrid.setStyle("-fx-padding: 8 0 0 32;");
                    microServiceGrid.add(new Label("微服务名称"), 0, 0);
                    microServiceGrid.add(new Label("服务简称"), 1, 0);
                    microServiceGrid.add(new Label("端口"), 2, 0);
                    microServiceGrid.add(new Label("路径"), 3, 0);
                    microServiceGrid.add(new Label("打开终端"), 4, 0);
                    microServiceGrid.add(new Label("配置文件"), 5, 0);
                    microServiceGrid.add(new Label("删除"), 6, 0);
                    int rowIdx = 1;
                    int msCount = msList.size();
                    int msIdx = 0;
                    for (ServiceInfoEntity.MicroServiceInfo ms : msList) {
                        msIdx++;
                        TextField msNameField = new TextField(ms.getProjectName());
                        TextField appNameField = new TextField(ms.getAppName());
                        appNameField.setPrefWidth(70);
                        appNameField.setMinWidth(70);
                        appNameField.setMaxWidth(70);
                        TextField portField = new TextField(ms.getAppPort());
                        portField.setPrefWidth(50);
                        portField.setMinWidth(50);
                        portField.setMaxWidth(50);
                        TextField pathField = new TextField(ms.getAppPath());
                        pathField.setMinWidth(150);
                        pathField.setPrefWidth(150);
                        pathField.setMaxWidth(600);
                        Button configBtn = new Button("配置");
                        configBtn.setOnAction(e -> {
                            // 远程读取配置文件内容
                            new Thread(() -> {
                                try {
                                    SSHClient ssh = new SSHClient();
                                    ssh.addHostKeyVerifier(new PromiscuousVerifier());
                                    ssh.connect(entity.getIp(), Integer.parseInt(entity.getPort()));
                                    ssh.authPassword(entity.getUsername(), entity.getPassword());
                                    SFTPClient sftp = ssh.newSFTPClient();
                                    // 下载远程配置文件到本地临时文件
                                    File tempFile = File.createTempFile("application", ".properties");
                                    String localTempPath = tempFile.getAbsolutePath();
                                    sftp.get(ms.getAppProp(), localTempPath);
                                    // 读取本地临时文件内容
                                    String content = Files.readString(java.nio.file.Paths.get(localTempPath));
                                    // 删除临时文件
                                    tempFile.delete();
                                    sftp.close();
                                    ssh.disconnect();
                                    Platform.runLater(() -> {
                                        // 使用LogDialogUtil显示配置文件内容
                                        LogDialogUtil.showLogDialog(content, "配置文件内容 - " + ms.getAppProp(), 900, 600);
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Platform.runLater(() -> {
                                        ViewUtils.alertForFail("读取配置文件失败: " + ex.getMessage());
                                    });
                                }
                            }).start();
                        });
                        // 新增：删除微服务按钮
                        Button delMsBtn = new Button("删除");
                        delMsBtn.setOnAction(e -> {
                            // 删除ServerInfoPO对应微服务记录
                            ServerInfoPO delPo = new ServerInfoPO();
                            delPo.setIp(entity.getIp());
                            delPo.setGroupName(entity.getGroupName());
                            delPo.setProjectName(ms.getProjectName());
                            delPo.setAppName(ms.getAppName());
                            serverInfoRpcService.delete(delPo);
                            // 刷新页面
                            List<ServiceInfoEntity> newServiceList = sshDomainService.getServiceList(globalProps.getGroupName());
                            refreshServerList(newServiceList, globalProps.getGroupName());
                        });
                        // 新增：终端图标按钮
                        Button terminalBtn = new Button();
                        ImageView terminalIcon = new ImageView(new Image(getClass().getResourceAsStream("/image/remixA/terminal-box-line.png")));
                        terminalIcon.setFitWidth(18);
                        terminalIcon.setFitHeight(18);
                        terminalBtn.setGraphic(terminalIcon);
                        terminalBtn.setStyle("-fx-background-color: transparent;");
                        terminalBtn.setTooltip(new Tooltip("打开终端"));
                        terminalBtn.setCursor(javafx.scene.Cursor.HAND);
                        terminalBtn.setOnMouseEntered(e -> terminalBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        terminalBtn.setOnMouseExited(e -> terminalBtn.setStyle("-fx-background-color: transparent;"));
                        terminalBtn.setOnMousePressed(e -> terminalBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                        terminalBtn.setOnMouseReleased(e -> terminalBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                        terminalBtn.setOnAction(ev -> {
                            System.out.println("terminalBtn clicked!");
                            String ip = entity.getIp();
                            String port = entity.getPort();
                            String username = entity.getUsername();
                            String password = entity.getPassword();
                            String appPath = ms.getAppPath()+ "/"+ MrDateUtils.getCurrentDay(); // 微服务目录

                            try {
                                // 始终在主TabPane中打开一个新的标签页（不复用已有“终端”页）
                                MainController mainController = MrSpringContextHolder.getBean(MainController.class);
                                String tabKey = "terminal-" + username + "@" + ip + ":" + ms.getAppName();
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pane_terminal.fxml"));
                                loader.setControllerFactory(clazz -> MrSpringContextHolder.getBean(clazz));
                                javafx.scene.Node root = loader.load();
                                Tab newTab = new Tab("终端 - " + ms.getAppName() + "@" + ip);
                                newTab.setId(tabKey);
                                newTab.setContent(root);
                                newTab.setClosable(true);
                                mainController.getPageContainer().getTabs().add(newTab);
                                mainController.getPageContainer().getSelectionModel().select(newTab);
                                PaneTerminalController controller = loader.getController();
                                Platform.runLater(() -> controller.connectAndCd(ip, port, username, password, appPath));
                                newTab.setOnClosed(e3 -> controller.cleanup());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                Platform.runLater(() -> ViewUtils.alertForFail("在标签页中打开终端失败: " + ex.getMessage()));
                            }
                        });
                        microServiceGrid.add(msNameField, 0, rowIdx);
                        microServiceGrid.add(appNameField, 1, rowIdx);
                        microServiceGrid.add(portField, 2, rowIdx);
                        microServiceGrid.add(pathField, 3, rowIdx);
                        microServiceGrid.add(terminalBtn, 4, rowIdx);
                        microServiceGrid.add(configBtn, 5, rowIdx);
                        microServiceGrid.add(delMsBtn, 6, rowIdx);
                        // 每行都加分割线，最后一行底部间距更大
                        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
                        separator.setPrefWidth(600);
                        if (msIdx == msCount) {
                            javafx.scene.layout.GridPane.setMargin(separator, new javafx.geometry.Insets(0, 0, 18, 0));
                        } else {
                            javafx.scene.layout.GridPane.setMargin(separator, new javafx.geometry.Insets(0, 0, 4, 0));
                        }
                        microServiceGrid.add(separator, 0, rowIdx + 1, 7, 1);
                        rowIdx += 2;
                    }
                    TitledPane pane = new TitledPane();
                    pane.setGraphic(titleBox);
                    pane.setContent(microServiceGrid);
                    pane.setExpanded(false);
                    serverConfigVBox.getChildren().add(pane);
                });
    }

    private Node buildServerTitleFromEntity(ServiceInfoEntity entity) {
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        TextField ipField = new TextField(entity.getIp());
        ipField.setPrefWidth(100);
        TextField userField = new TextField(entity.getUsername());
        userField.setPrefWidth(60);
        TextField pwdField = new TextField(entity.getPassword());
        pwdField.setPrefWidth(100);
        TextField portField = new TextField(entity.getPort());
        portField.setPrefWidth(40);
        // 新增环境下拉框
        ComboBox<String> envCombo = new ComboBox<>();
        envCombo.getItems().addAll("请选择","dev", "sit", "uat", "poc");
        envCombo.setPrefWidth(70);
        String envName = entity.getEnvName();
        if (envName != null && !envName.isEmpty()) {
            envCombo.getSelectionModel().select(envName);
        } else {
            envCombo.getSelectionModel().selectFirst();
        }
        // 监听环境下拉框变化，批量更新同IP服务器的envName
        envCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                ServerInfoPO query = new ServerInfoPO();
                ServerInfoPO update = new ServerInfoPO();
                update.setEnvName(newVal);
                query.setIp(ipField.getText());
                serverInfoRpcService.updateByOne(update,query);
            }
        });
        Label ipLabel = new Label("IP:");
        Label userLabel = new Label("用户名:");
        Label pwdLabel = new Label("密码:");
        Label portLabel = new Label("端口:");
        ipLabel.setAlignment(javafx.geometry.Pos.CENTER);
        userLabel.setAlignment(javafx.geometry.Pos.CENTER);
        pwdLabel.setAlignment(javafx.geometry.Pos.CENTER);
        portLabel.setAlignment(javafx.geometry.Pos.CENTER);
        ipLabel.setMinHeight(28);
        userLabel.setMinHeight(28);
        pwdLabel.setMinHeight(28);
        portLabel.setMinHeight(28);
        titleBox.getChildren().clear();
        titleBox.getChildren().addAll(ipLabel, ipField, userLabel, userField, pwdLabel, pwdField, portLabel, portField, envCombo);
        return titleBox;
    }

    // 新增：PO列表转Entity列表的基础转换方法
    private List<ServiceInfoEntity> convertServerInfoListToServiceList(List<ServerInfoPO> serverList) {
        Map<String, ServiceInfoEntity> entityMap = new LinkedHashMap<>();
        for (ServerInfoPO po : serverList) {
            String key = po.getIp() + ":" + po.getGroupName();
            ServiceInfoEntity entity = entityMap.get(key);
            if (entity == null) {
                entity = new ServiceInfoEntity();
                entity.setIp(po.getIp());
                entity.setPort(po.getPort() != null ? po.getPort().toString() : "22");
                entity.setUsername(po.getUsername());
                entity.setPassword(po.getPassword());
                entity.setGroupName(po.getGroupName());
                entity.setEnvName(po.getEnvName());
                entity.setMicroServiceList(new ArrayList<>());
                entityMap.put(key, entity);
            }
            // 构建微服务信息
            ServiceInfoEntity.MicroServiceInfo ms = new ServiceInfoEntity.MicroServiceInfo();
            ms.setProjectName(po.getProjectName());
            ms.setAppName(po.getAppName());
            ms.setAppPort(po.getAppPort());
            ms.setAppPath(po.getAppPath());
            ms.setAppProp(po.getAppProp());
            entity.getMicroServiceList().add(ms);
        }
        return new ArrayList<>(entityMap.values());
    }

    /**
     * 初始化搜索结果表格
     */
    private void initializeSearchResultTable() {
        // 设置表格列
        TableColumn<Map<String, String>, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("ip")));
        ipCol.setPrefWidth(80);

        TableColumn<Map<String, String>, String> appCol = new TableColumn<>("微服务");
        appCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("appName")));
        appCol.setPrefWidth(60);
        // 设置微服务列文字居中
        appCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        TableColumn<Map<String, String>, String> fileCol = new TableColumn<>("文件名");
        fileCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get("filename")));
        fileCol.setPrefWidth(200);
        // 为文件名列添加双击事件
        fileCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setOnMouseClicked(null);
                } else {
                    setText(item);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            // 双击事件 - 查看日志
                            Map<String, String> fileInfo = getTableView().getItems().get(getIndex());
                            String ip = fileInfo.get("ip");
                            String filePath = fileInfo.get("path");
                            new Thread(() -> {
                                try {
                                    ServerInfoPO server = currentSearchServerList.stream().filter(s -> s.getIp().equals(ip)).findFirst().orElse(null);
                                    if (server == null) return;
                                    String content = sshDomainService.readRemoteFile(ip, server.getPort(), server.getUsername(), server.getPassword(), filePath);
                                    Platform.runLater(() -> {
                                        LogDialogUtil.showLogDialog(content, "日志内容 - " + fileInfo.get("filename"), 900, 600);
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Platform.runLater(() -> {
                                        ViewUtils.alertForFail("读取日志失败: " + ex.getMessage());
                                    });
                                }
                            }).start();
                        }
                    });
                }
            }
        });

        TableColumn<Map<String, String>, String> timeCol = new TableColumn<>("登记时间");
        timeCol.setCellValueFactory(data -> {
            String mtime = data.getValue().get("mtime");
            if (mtime == null || mtime.isEmpty() || mtime.equals("0")) return new SimpleStringProperty("");
            long sec = Long.parseLong(mtime);
            LocalDateTime dt = LocalDateTime.ofEpochSecond(sec, 0, ZoneOffset.ofHours(8));
            String formatted = dt.format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"));
            return new SimpleStringProperty(formatted);
        });
        timeCol.setPrefWidth(100);



        searchResultTable.getColumns().addAll(appCol, fileCol, ipCol, timeCol);
        // 设置表格列调整策略，允许横向滚动
        searchResultTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // 确保表头字体颜色正确
        searchResultTable.setStyle("-fx-table-header-background-color: #f4f4f4; -fx-table-header-text-fill: black;");
    }
    
    /**
     * 初始化搜索表单字段
     */
    private void initializeSearchFormFields() {
        // 初始化搜索类型下拉框
        searchTypeCombo.getItems().clear();
        searchTypeCombo.getItems().addAll("按文件名搜索", "按文件内容搜索");
        searchTypeCombo.getSelectionModel().selectFirst();
        
        // 设置搜索内容输入框的最小宽度，确保横向滚动条能够正常工作
        searchContentField.setPrefWidth(150);
        
        // 初始化搜索天数默认值
        searchDayField.setText("1");
        
        // 延迟加载微服务名称下拉框
        searchMicroServiceCombo.getItems().add("请选择");
        searchMicroServiceCombo.getItems().add("加载中...");
        Platform.runLater(() -> {
            try {
                List<String> projectNames = new ArrayList<>();
                List<ServerInfoPO> allServers = serverInfoRpcService.queryForList(new ServerInfoPO());
                for (ServerInfoPO po : allServers) {
                    if (po.getProjectName() != null && !po.getProjectName().trim().isEmpty()) {
                        projectNames.add(po.getProjectName().trim());
                    }
                }
                final List<String> finalProjectNames = projectNames.stream().distinct().sorted().toList();
                
                Platform.runLater(() -> {
                    searchMicroServiceCombo.getItems().clear();
                    searchMicroServiceCombo.getItems().add("请选择");
                    searchMicroServiceCombo.getItems().addAll(finalProjectNames);
                    searchMicroServiceCombo.getSelectionModel().selectFirst();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    searchMicroServiceCombo.getItems().clear();
                    searchMicroServiceCombo.getItems().add("请选择");
                    searchMicroServiceCombo.getItems().add("加载失败");
                    searchMicroServiceCombo.getSelectionModel().selectFirst();
                });
            }
        });
        
        // 开始搜索按钮事件
        searchBtn.setOnAction(e -> {
            performSearchFromForm();
        });
        
        // 清空搜索按钮事件
        clearSearchBtn.setOnAction(e -> {
            clearSearchForm();
        });
    }
    
    /**
     * 从表单执行搜索
     */
    private void performSearchFromForm() {
        String searchType = searchTypeCombo.getValue();
        String selectedProject = searchMicroServiceCombo.getValue();
        String dayNum = searchDayField.getText();
        String keyword = searchContentField.getText();
        String env = environmentCombo.getValue();

        // 校验环境
        if ("请选择".equals(env)) {
            ViewUtils.alertForFail("请选择搜索环境");
            return;
        }
        // 校验天数
        if (dayNum == null || dayNum.trim().isEmpty()) {
            ViewUtils.alertForFail("请输入搜索天数");
            return;
        }
        // 校验搜索内容
        if (keyword == null || keyword.trim().isEmpty()) {
            ViewUtils.alertForFail("请输入搜索内容");
            return;
        }

        ServerInfoPO query = new ServerInfoPO();
        query.setEnvName(env);
        query.setScanFlg(1);

        if ("按文件名搜索".equals(searchType) && selectedProject != null && !"请选择".equals(selectedProject)) {
            query.setProjectName(selectedProject);
        }
        List<ServerInfoPO> serverList = serverInfoRpcService.queryForList(query);
        // 保存当前搜索的服务器列表
        currentSearchServerList = serverList;

        int dayCount = Integer.parseInt(dayNum);
        
        // 显示进度条并执行搜索
        Platform.runLater(() -> {
            Dialog<Void> progressDialog = new Dialog<>();
            progressDialog.setTitle("正在搜索...");
            progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);
            Label progressLabel = new Label("正在搜索服务器日志...");
            VBox vbox = new VBox(16, progressLabel, progressBar);
            vbox.setPrefWidth(420);
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            progressDialog.getDialogPane().setContent(vbox);
            progressDialog.setResizable(false);
            progressDialog.show();

            new Thread(() -> {
                try {
                    System.out.println("Starting search with " + serverList.size() + " servers");
                    List<Map<String, String>> fileLst = sshDomainService.searchLogFiles(
                        serverList, keyword, searchType, dayCount,
                        progress -> Platform.runLater(() -> progressBar.setProgress(progress))
                    );
                    System.out.println("Search completed, found " + (fileLst != null ? fileLst.size() : "null") + " files");
                    
                    Platform.runLater(() -> {
                        progressDialog.close();
                        if (fileLst == null || fileLst.isEmpty()) {
                            ViewUtils.alertForFail("未找到任何日志文件！");
                            return;
                        }
                        // 更新右侧搜索结果表格
                        System.out.println("Calling updateSearchResults with " + fileLst.size() + " files");
                        updateSearchResults(fileLst);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        progressDialog.close();
                        ViewUtils.alertForFail("搜索失败: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }
    
    /**
     * 清空搜索表单
     */
    private void clearSearchForm() {
        searchTypeCombo.getSelectionModel().selectFirst();
        searchMicroServiceCombo.getSelectionModel().selectFirst();
        searchDayField.setText("1");
        searchContentField.clear();
        // 同时清空搜索结果
        clearSearchResults();
    }
    
    /**
     * 更新搜索结果表格
     */
    public void updateSearchResults(List<Map<String, String>> fileLst) {
        System.out.println("updateSearchResults called with " + (fileLst != null ? fileLst.size() : "null") + " items");
        
        if (fileLst != null && !fileLst.isEmpty()) {
            // 排序：先按微服务名，再按最后修改时间降序
            fileLst.sort((a, b) -> {
                int cmp = a.get("appName").compareToIgnoreCase(b.get("appName"));
                if (cmp != 0) return cmp;
                long t1 = Long.parseLong(b.get("mtime"));
                long t2 = Long.parseLong(a.get("mtime"));
                return Long.compare(t1, t2);
            });
            
            Platform.runLater(() -> {
                System.out.println("Clearing and adding " + fileLst.size() + " items to table");
                searchResultTable.getItems().clear();
                searchResultTable.getItems().addAll(fileLst);
                System.out.println("Table now has " + searchResultTable.getItems().size() + " items");
                
                // 强制刷新表格
                searchResultTable.refresh();
            });
        } else {
            Platform.runLater(() -> {
                System.out.println("Clearing table - no results");
                searchResultTable.getItems().clear();
                searchResultTable.refresh();
            });
        }
    }
    
    /**
     * 清空搜索结果
     */
    public void clearSearchResults() {
        Platform.runLater(() -> {
            searchResultTable.getItems().clear();
        });
    }
    
    /**
     * 显示添加微服务对话框
     */
    private void showAddMicroServiceDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("添加微服务");
        dialog.setHeaderText("请输入微服务信息");
        ButtonType okBtnType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtnType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // 微服务名称下拉框
        ComboBox<String> projectNameCombo = new ComboBox<>();
        projectNameCombo.setPromptText("微服务名称");
        // 延迟查询所有project_param表的projectName
        projectNameCombo.getItems().add("加载中...");
        // 创建一个原子引用来存储projectParamList
        final AtomicReference<List<UserProjSettingPO>> projectParamListRef = new AtomicReference<>();
        
        Platform.runLater(() -> {
            try {
                List<UserProjSettingPO> tempList = UserProjSettingRpcService.queryForList(new UserProjSettingPO());
                projectParamListRef.set(tempList);
                List<String> projectNames = new ArrayList<>();
                for (UserProjSettingPO po : tempList) {
                    if (po.getProjectName() != null && !po.getProjectName().trim().isEmpty()) {
                        projectNames.add(po.getProjectName().trim());
                    }
                }
                final List<String> finalProjectNames = projectNames.stream().distinct().sorted().toList();
                
                Platform.runLater(() -> {
                    projectNameCombo.getItems().clear();
                    projectNameCombo.getItems().addAll(finalProjectNames);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    projectNameCombo.getItems().clear();
                    projectNameCombo.getItems().add("加载失败");
                });
            }
        });
        
        // 服务简称
        TextField appNameField = new TextField();
        appNameField.setPromptText("服务简称");
        // 监听微服务名称变化，自动填充服务简称
        projectNameCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                List<UserProjSettingPO> projectParamList = projectParamListRef.get();
                if (projectParamList != null) {
                    for (UserProjSettingPO po : projectParamList) {
                        if (newVal.equals(po.getProjectName())) {
                            appNameField.setText(po.getAppName() != null ? po.getAppName() : "");
                            break;
                        }
                    }
                }
            } else {
                appNameField.setText("");
            }
        });
        
        ComboBox<String> envNameCombo = new ComboBox<>();
        envNameCombo.getItems().addAll("请选择", "dev", "sit", "uat", "poc");
        envNameCombo.setPrefWidth(120);
        String currentEnv = environmentCombo.getValue();
        if (currentEnv != null && !currentEnv.isEmpty()) {
            envNameCombo.getSelectionModel().select(currentEnv);
        } else {
            envNameCombo.getSelectionModel().selectFirst();
        }
        
        TextField appPortField = new TextField();
        appPortField.setPromptText("端口");
        TextField ipField = new TextField();
        ipField.setPromptText("IP");
        TextField portField = new TextField("22");
        portField.setPromptText("SSH端口");
        TextField usernameField = new TextField();
        usernameField.setPromptText("用户名");
        TextField passwordField = new TextField();
        passwordField.setPromptText("密码");
        TextField appPathField = new TextField();
        appPathField.setPromptText("服务路径");
        TextField appPropField = new TextField();
        appPropField.setPromptText("配置文件路径");
        
        grid.add(new Label("微服务名称:"), 0, 0); grid.add(projectNameCombo, 1, 0);
        grid.add(new Label("服务简称:"), 0, 1); grid.add(appNameField, 1, 1);
        grid.add(new Label("环境:"), 0, 2); grid.add(envNameCombo, 1, 2);
        grid.add(new Label("端口:"), 0, 3); grid.add(appPortField, 1, 3);
        grid.add(new Label("IP:"), 0, 4); grid.add(ipField, 1, 4);
        grid.add(new Label("SSH端口:"), 0, 5); grid.add(portField, 1, 5);
        grid.add(new Label("用户名:"), 0, 6); grid.add(usernameField, 1, 6);
        grid.add(new Label("密码:"), 0, 7); grid.add(passwordField, 1, 7);
        grid.add(new Label("服务路径:"), 0, 8); grid.add(appPathField, 1, 8);
        grid.add(new Label("配置文件路径:"), 0, 9); grid.add(appPropField, 1, 9);
        
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okBtnType) {
                Map<String, String> result = new HashMap<>();
                result.put("projectName", projectNameCombo.getValue());
                result.put("appName", appNameField.getText());
                result.put("envName", envNameCombo.getValue());
                result.put("appPort", appPortField.getText());
                result.put("ip", ipField.getText());
                result.put("port", portField.getText());
                result.put("username", usernameField.getText());
                result.put("password", passwordField.getText());
                result.put("appPath", appPathField.getText());
                result.put("appProp", appPropField.getText());
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(input -> {
            String groupName = globalProps.getGroupName(); // 分组名自动获取
            String projectName = input.get("projectName");
            String appName = input.get("appName");
            String envName = input.get("envName");
            String appPort = input.get("appPort");
            String ip = input.get("ip");
            String port = input.get("port");
            String username = input.get("username");
            String password = input.get("password");
            String appPath = input.get("appPath");
            String appProp = input.get("appProp");
            
            ServerInfoPO po = new ServerInfoPO();
            po.setGroupName(groupName);
            po.setProjectName(projectName);
            po.setAppName(appName);
            po.setEnvName(envName);
            po.setAppPort(appPort);
            po.setIp(ip);
            po.setPort(port);
            po.setUsername(username);
            po.setPassword(password);
            po.setAppPath(appPath);
            po.setAppProp(appProp);

            
            try {
                if (!serverInfoRpcService.insertIfAbsent(po)) {
                    ViewUtils.alertForFail("微服务已存在");
                    return;
                }
                ViewUtils.alertForSucess("添加成功");
                // 刷新页面
                List<ServiceInfoEntity> newServiceList = sshDomainService.getServiceList(globalProps.getGroupName());
                refreshServerList(newServiceList, globalProps.getGroupName());
            } catch (Exception ex) {
                ex.printStackTrace();
                ViewUtils.alertForFail("添加失败: " + ex.getMessage());
            }
        });
    }
} 