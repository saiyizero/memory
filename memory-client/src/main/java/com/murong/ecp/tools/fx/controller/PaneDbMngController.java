package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.DatasourceConfig;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.DbConnectionDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

@Component
public class PaneDbMngController {
    @FXML private TableView<DatasourceConfig> datasourceTable;
    @FXML private TableColumn<DatasourceConfig, String> driverCol;
    @FXML private TableColumn<DatasourceConfig, String> usernameCol;
    @FXML private TableColumn<DatasourceConfig, String> passwordCol;
    @FXML private TableColumn<DatasourceConfig, String> urlCol;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button importBtn;
    @FXML private TableColumn<DatasourceConfig, Boolean> selectCol;
    @FXML private Button toggleSelectBtn;
    @FXML private TableColumn<DatasourceConfig, String> appNameCol;
    @FXML private TableColumn<DatasourceConfig, String> schemaCol;
    @FXML private TableColumn<DatasourceConfig, String> envCol;
    @FXML private ComboBox<String> envCombo;
    @FXML private Button setEnvBtn;
    @FXML private TableColumn<DatasourceConfig, Boolean> mainFlagCol;

    private final ObservableList<DatasourceConfig> datasourceList = FXCollections.observableArrayList();

    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private DbConnectionDao dbConnectionDao;
    @Autowired
    private UserProjSettingDao UserProjSettingDao;

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalPropes)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        datasourceTable.setEditable(true);
        selectCol.setEditable(true);
        driverCol.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        setCopyableCellFactory(driverCol, DatasourceConfig::getDriverName);
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        setCopyableCellFactory(usernameCol, DatasourceConfig::getUsername);
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));
        setCopyableCellFactory(passwordCol, DatasourceConfig::getPassword);
        urlCol.setCellValueFactory(new PropertyValueFactory<>("url"));
        setCopyableCellFactory(urlCol, DatasourceConfig::getUrl);
        appNameCol.setCellValueFactory(new PropertyValueFactory<>("appName"));
        setCopyableCellFactory(appNameCol, DatasourceConfig::getAppName);
        schemaCol.setCellValueFactory(new PropertyValueFactory<>("schema"));
        setCopyableCellFactory(schemaCol, DatasourceConfig::getSchema);
        envCol.setCellValueFactory(new PropertyValueFactory<>("envName"));
        setCopyableCellFactory(envCol, DatasourceConfig::getEnvName);
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        appNameCol.setCellValueFactory(new PropertyValueFactory<>("appName"));
        schemaCol.setCellValueFactory(new PropertyValueFactory<>("schema"));
        envCol.setCellValueFactory(new PropertyValueFactory<>("envName"));
        mainFlagCol.setCellValueFactory(cellData -> cellData.getValue().mainFlagProperty());
        mainFlagCol.setCellFactory(col -> {
            CheckBoxTableCell<DatasourceConfig, Boolean> cell = new CheckBoxTableCell<>();
            cell.setSelectedStateCallback(index -> {
                DatasourceConfig cfg = datasourceTable.getItems().get(index);
                BooleanProperty prop = cfg.mainFlagProperty();
                
                // 检查是否已经添加过监听器，避免重复添加
                if (!cfg.isMainFlagListenerAdded()) {
                    prop.addListener((obs, oldVal, newVal) -> {
                        // 当前行数据库更新
                        DbConnectionPO updateDbPO = new DbConnectionPO();
                        updateDbPO.setGroupName(globalPropes.getGroupName());
                        updateDbPO.setProjectName(cfg.getAppName());
                        updateDbPO.setSchemaNm(cfg.getSchema());
                        updateDbPO.setEnvName(cfg.getEnvName());
                        updateDbPO.setMainFlg(newVal ? "1" : "0");
                        dbConnectionDao.updateMainFlg(updateDbPO);
                        
                        // 刷新查询页面，根据当前环境过滤
                        loadDbConnectionsByCurrentEnv();
                    });
                    cfg.setMainFlagListenerAdded(true);
                }
                return prop;
            });
            return cell;
        });
        mainFlagCol.setEditable(true);
        datasourceTable.setItems(datasourceList);
        loadDbConnections();
        addBtn.setOnAction(e -> showEditDialog(null));
        editBtn.setOnAction(e -> {
            // 过滤出所有被勾选的
            List<DatasourceConfig> selectedList = datasourceList.stream()
                    .filter(DatasourceConfig::isSelected)
                    .toList();
            if (selectedList.isEmpty()) {
                showAlert("请先勾选要编辑的数据源！");
            } else if (selectedList.size() > 1) {
                showAlert("只允许勾选一条进行编辑！");
            } else {
                showEditDialog(selectedList.get(0));
            }
        });
        deleteBtn.setOnAction(e -> {
            // 删除所有选中项
            java.util.List<DatasourceConfig> toRemove = new java.util.ArrayList<>();
            for (DatasourceConfig cfg : datasourceList) {
                if (cfg.isSelected()) {
                    toRemove.add(cfg);
                }
            }
            for (DatasourceConfig cfg : toRemove) {
                datasourceList.remove(cfg);
                // 同步删除数据库
                DbConnectionPO po = new DbConnectionPO();
                po.setGroupName(globalPropes.getGroupName());
                po.setDriver(cfg.getDriverName());
                po.setJdbcUrl(cfg.getUrl());
                po.setUsername(cfg.getUsername());
                po.setPassword(cfg.getPassword());
                dbConnectionDao.delete(po);
            }
        });
        importBtn.setOnAction(e -> handleImportConfig());
        toggleSelectBtn.setOnAction(e -> {
            boolean allSelected = datasourceList.stream().allMatch(DatasourceConfig::isSelected);
            if (allSelected) {
                datasourceList.forEach(cfg -> cfg.setSelected(false));
                toggleSelectBtn.setText("全选");
            } else {
                datasourceList.forEach(cfg -> cfg.setSelected(true));
                toggleSelectBtn.setText("全不选");
            }
        });
        setEnvBtn.setOnAction(e -> {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("dev", "sit", "uat", "poc");
            dialog.setTitle("设置环境");
            dialog.setHeaderText("请选择要设置的环境");
            dialog.setContentText("环境:");
            dialog.showAndWait().ifPresent(selectedEnv -> {
                for (DatasourceConfig cfg : datasourceList) {
                    if (cfg.isSelected()) {
                        // 更新内存
                        // cfg.setEnvName(selectedEnv); // 如果DatasourceConfig有envName字段可加
                        // 更新数据库
                        DbConnectionPO where = new DbConnectionPO();
                        where.setDriver(cfg.getDriverName());
                        where.setJdbcUrl(cfg.getUrl());
                        where.setUsername(cfg.getUsername());
                        where.setPassword(cfg.getPassword());
                        DbConnectionPO update = new DbConnectionPO();
                        update.setEnvName(selectedEnv);
                        dbConnectionDao.updateByOne(update, where);
                    }
                }
                // 可选：刷新表格
                loadDbConnectionsByCurrentEnv();
            });
        });
        if (envCombo != null) {
            envCombo.getItems().setAll("请选择环境", "dev", "sit", "uat", "poc","other");
            envCombo.getSelectionModel().selectFirst();
            envCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                // 根据选择的环境过滤加载数据
                loadDbConnectionsByCurrentEnv();
            });
        }
        // 支持双击复制
        enableCellDoubleClickCopy(driverCol);
        enableCellDoubleClickCopy(usernameCol);
        enableCellDoubleClickCopy(passwordCol);
        enableCellDoubleClickCopy(urlCol);
        enableCellDoubleClickCopy(appNameCol);
        enableCellDoubleClickCopy(schemaCol);
        enableCellDoubleClickCopy(envCol);
        // 设置表格行双击复制单元格内容
        datasourceTable.setRowFactory(tv -> {
            TableRow<DatasourceConfig> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    TableView.TableViewSelectionModel<DatasourceConfig> selectionModel = datasourceTable.getSelectionModel();
                    ObservableList<TablePosition> selectedCells = selectionModel.getSelectedCells();
                    if (!selectedCells.isEmpty()) {
                        TablePosition pos = selectedCells.get(0);
                        int colIndex = pos.getColumn();
                        TableColumn col = datasourceTable.getColumns().get(colIndex);
                        Object cellData = col.getCellData(row.getIndex());
                        if (cellData != null) {
                            final Clipboard clipboard = Clipboard.getSystemClipboard();
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(cellData.toString());
                            clipboard.setContent(content);
                            Tooltip tp = new Tooltip("已复制: " + cellData.toString());
                            Tooltip.install(row, tp);
                            tp.show(row, event.getScreenX(), event.getScreenY());
                            new Thread(() -> {
                                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                                Platform.runLater(tp::hide);
                            }).start();
                        }
                    }
                }
            });
            return row;
        });
    }

    private void loadDbConnections() {
        datasourceList.clear();
        DbConnectionPO dbConnectionPO = new DbConnectionPO();
        dbConnectionPO.setGroupName(globalPropes.getGroupName());
        java.util.List<DbConnectionPO> dbList = dbConnectionDao.queryForList(dbConnectionPO);
        for (DbConnectionPO po : dbList) {
            DatasourceConfig cfg = new DatasourceConfig();
            cfg.setDriverName(po.getDriver());
            cfg.setUsername(po.getUsername());
            cfg.setPassword(po.getPassword());
            cfg.setUrl(po.getJdbcUrl());
            cfg.setAppName(po.getProjectName());
            cfg.setSchema(po.getSchemaNm());
            cfg.setEnvName(po.getEnvName());
            cfg.setMainFlag("1".equals(po.getMainFlg()));
            datasourceList.add(cfg);
        }
    }
    
    /**
     * 根据当前环境过滤加载数据源连接
     */
    private void loadDbConnectionsByCurrentEnv() {
        datasourceList.clear();
        
        String selectedEnv = envCombo != null ? envCombo.getValue() : null;
        List<DbConnectionPO> dbList;
        
        if (selectedEnv == null || "请选择环境".equals(selectedEnv)) {
            // 加载所有数据
            DbConnectionPO dbConnectionPO = new DbConnectionPO();
            dbConnectionPO.setGroupName(globalPropes.getGroupName());
            dbList = dbConnectionDao.queryForList(dbConnectionPO);
        } else if ("other".equals(selectedEnv)) {
            // 查询envName为null或空的数据
            dbList = dbConnectionDao.queryForListWithNullEnv();
        } else {
            // 根据选择的环境过滤
            DbConnectionPO query = new DbConnectionPO();
            query.setGroupName(globalPropes.getGroupName());
            query.setEnvName(selectedEnv);
            dbList = dbConnectionDao.queryForList(query);
        }
        
        for (DbConnectionPO po : dbList) {
            DatasourceConfig cfg = new DatasourceConfig();
            cfg.setDriverName(po.getDriver());
            cfg.setUsername(po.getUsername());
            cfg.setPassword(po.getPassword());
            cfg.setUrl(po.getJdbcUrl());
            cfg.setAppName(po.getProjectName());
            cfg.setSchema(po.getSchemaNm());
            cfg.setEnvName(po.getEnvName());
            cfg.setMainFlag("1".equals(po.getMainFlg()));
            datasourceList.add(cfg);
        }
    }

    private void showEditDialog(DatasourceConfig config) {
        Dialog<DatasourceConfig> dialog = new Dialog<>();
        dialog.setTitle(config == null ? "新增数据源" : "编辑数据源");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // 可编辑字段
        TextField driverField = new TextField(config == null ? "" : config.getDriverName());
        TextField userField = new TextField(config == null ? "" : config.getUsername());
        TextField pwdField = new TextField(config == null ? "" : config.getPassword()); // 明文显示密码
        TextField urlField = new TextField(config == null ? "" : config.getUrl());
        TextField schemaField = new TextField(config == null ? "all" : config.getSchema());
        schemaField.setEditable(true); // schema字段可编辑

        // 下拉：微服务
        ComboBox<String> appNameCombo = new ComboBox<>();
        final List<UserProjSettingPO> projectParamList = new ArrayList<>();
        try {
            projectParamList.addAll(UserProjSettingDao.queryForList(new UserProjSettingPO()));
        } catch (Exception ex) {
            projectParamList.clear();
        }
        List<String> projectNames = new ArrayList<>();
        projectNames.add("all");
        for (UserProjSettingPO po : projectParamList) {
            if (po.getProjectName() != null && !po.getProjectName().trim().isEmpty()) {
                projectNames.add(po.getProjectName().trim());
            }
        }
        projectNames = projectNames.stream().distinct().sorted().toList();
        appNameCombo.getItems().addAll(projectNames);
        if (config != null && config.getAppName() != null) {
            appNameCombo.getSelectionModel().select(config.getAppName());
        } else {
            appNameCombo.getSelectionModel().select("all");
        }

        // 服务简称字段（只读）
        TextField appNameField = new TextField();
        appNameField.setEditable(false);
        if (config == null) {
            appNameField.setText("all");
        }

        // 微服务下拉监听，自动填充appName和schema
        appNameCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("all".equals(newVal)) {
                appNameField.setText("all");
                schemaField.setText("all");
            } else {
                UserProjSettingPO match = projectParamList.stream()
                    .filter(po -> newVal.equals(po.getProjectName()))
                    .findFirst()
                    .orElse(null);
                if (match != null) {
                    appNameField.setText(match.getAppName() != null ? match.getAppName() : "");
                    schemaField.setText(match.getSchemaNm() != null ? match.getSchemaNm() : "all");
                } else {
                    appNameField.setText("");
                    schemaField.setText("all");
                }
            }
        });
        // 初始化时也填充一次
        if (appNameCombo.getValue() != null) {
            appNameCombo.getSelectionModel().select(appNameCombo.getValue());
        } else if (!projectNames.isEmpty()) {
            appNameCombo.getSelectionModel().selectFirst();
        }

        // 下拉：主库标识
        ComboBox<String> mainFlagCombo = new ComboBox<>();
        mainFlagCombo.getItems().addAll("是", "否");
        if (config != null && config.isMainFlag()) {
            mainFlagCombo.getSelectionModel().select("是");
        } else {
            mainFlagCombo.getSelectionModel().select("否");
        }

        // 下拉：环境
        ComboBox<String> envCombo = new ComboBox<>();
        envCombo.getItems().addAll("dev", "sit", "uat", "poc", "other");
        if (config != null && config.getEnvName() != null) {
            envCombo.getSelectionModel().select(config.getEnvName());
        } else {
            envCombo.getSelectionModel().selectFirst();
        }

        // 只读字段（编辑时）
        Label appNameLabel = new Label(config == null ? "" : config.getAppName());
        Label envNameLabel = new Label(config == null ? "" : config.getEnvName());
        Label mainFlagLabel = new Label(config == null ? "" : (config.isMainFlag() ? "是" : "否"));

        int row = 0;
        grid.add(new Label("驱动名:"), 0, row); grid.add(driverField, 1, row++);
        grid.add(new Label("用户名:"), 0, row); grid.add(userField, 1, row++);
        grid.add(new Label("密码:"), 0, row); grid.add(pwdField, 1, row++);
        grid.add(new Label("数据库连接:"), 0, row); grid.add(urlField, 1, row++);
        if (config == null) {
            grid.add(new Label("微服务:"), 0, row); grid.add(appNameCombo, 1, row++);
            grid.add(new Label("服务简称:"), 0, row); grid.add(appNameField, 1, row++);
            grid.add(new Label("环境:"), 0, row); grid.add(envCombo, 1, row++);
            grid.add(new Label("主库:"), 0, row); grid.add(mainFlagCombo, 1, row++);
            grid.add(new Label("Schema:"), 0, row); grid.add(schemaField, 1, row++);
        } else {
            grid.add(new Label("微服务:"), 0, row); grid.add(appNameLabel, 1, row++);
            grid.add(new Label("环境:"), 0, row); grid.add(envNameLabel, 1, row++);
            grid.add(new Label("主库:"), 0, row); grid.add(mainFlagLabel, 1, row++);
            grid.add(new Label("Schema:"), 0, row); grid.add(schemaField, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                DatasourceConfig result = new DatasourceConfig(
                    driverField.getText(),
                    userField.getText(),
                    pwdField.getText(),
                    urlField.getText()
                );
                if (config == null) {
                    result.setAppName(appNameField.getText());
                    result.setEnvName(envCombo.getValue());
                    result.setMainFlag("是".equals(mainFlagCombo.getValue()));
                    result.setSchema(schemaField.getText());
                    result.setProjectName(appNameCombo.getValue()); // 新增：保存微服务下拉框的值
                } else {
                    result.setAppName(config.getAppName());
                    result.setEnvName(config.getEnvName());
                    result.setMainFlag(config.isMainFlag());
                    result.setSchema(schemaField.getText());
                    result.setProjectName(config.getProjectName()); // 编辑时也补全
                }
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (config == null) {
                datasourceList.add(result);
                // 新增时插入数据库
                DbConnectionPO po = new DbConnectionPO();
                po.setProjectName(result.getProjectName());
                po.setGroupName(globalPropes.getGroupName());
                po.setAppName(result.getAppName());
                po.setEnvName(result.getEnvName());
                po.setDriver(result.getDriverName());
                po.setUsername(result.getUsername());
                po.setPassword(result.getPassword());
                po.setJdbcUrl(result.getUrl());
                po.setSchemaNm(result.getSchema());
                po.setMainFlg(result.isMainFlag() ? "1" : "0");
                dbConnectionDao.save(po);
            } else {
                config.setDriverName(result.getDriverName());
                config.setUsername(result.getUsername());
                config.setPassword(result.getPassword());
                config.setUrl(result.getUrl());
                config.setSchema(result.getSchema());
                // 只读字段不变
                datasourceTable.refresh();

                // 数据库更新
                DbConnectionPO where = new DbConnectionPO();
                where.setGroupName(globalPropes.getGroupName());
                where.setProjectName(config.getAppName());
                where.setEnvName(config.getEnvName());

                DbConnectionPO update = new DbConnectionPO();
                update.setDriver(result.getDriverName());
                update.setUsername(result.getUsername());
                update.setPassword(result.getPassword());
                update.setJdbcUrl(result.getUrl());
                update.setSchemaNm(result.getSchema());

                dbConnectionDao.updateByOne(update, where);
            }
        });
    }

    private void handleImportConfig() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择配置文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties文件", "*.properties"));
        java.io.File file = fileChooser.showOpenDialog(null);
        if (file == null) return;
        if (!file.getName().endsWith(".properties")) {
            showAlert("请选择以.properties结尾的配置文件");
            return;
        }
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            // 1. 解析所有数据库连接URL（非#ECP，且只处理数据库相关key）
            Map<String, String> urlMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                boolean isDbUrl = 
                    key.startsWith("ecp.common.datasource.url") ||
                    (key.startsWith("ecp.common.db.") && key.contains(".url")) ||
                    (key.startsWith("ecp.datasource.") && key.contains(".url"));
                if (isDbUrl && !key.contains("#ECP")) {
                    urlMap.put(key, props.getProperty(key));
                }
            }
            // 2. 解析所有用户名、密码（非#ECP）
            Map<String, String> userMap = new java.util.HashMap<>();
            Map<String, String> pwdMap = new java.util.HashMap<>();
            for (String key : props.stringPropertyNames()) {
                System.out.println(key + ":" + props.getProperty(key));
                if (key.startsWith("ecp.common.db.") && key.contains(".username") && !key.contains("#ECP")) {
                    String micro = getMicroServiceName(key, ".username");
                    userMap.put(micro, props.getProperty(key));
                }
                if (key.startsWith("ecp.common.db.") && key.contains(".pwd") && !key.contains("#ECP")) {
                    String micro = getMicroServiceName(key, ".pwd");
                    pwdMap.put(micro, props.getProperty(key));
                }
            }
            // 3. 解析所有driver（非#ECP）
            Map<String, String> driverMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                if ((key.endsWith("driverClassName") || key.contains("driverClassName#")) && !key.contains("#ECP")) {
                    driverMap.put(key, props.getProperty(key));
                }
            }
            // 4. 组装DbConnectionPO并去重（每组用户名密码都生成一条数据源，url/driver共用）
            List<DbConnectionPO> dbConnectionLst = new ArrayList<>();
            for (Map.Entry<String, String> userEntry : userMap.entrySet()) {
                String micro = userEntry.getKey();
                String username = userEntry.getValue();
                String password = pwdMap.getOrDefault(micro, "");
                if (StringUtils.equals(micro,"urm")) {
                    System.out.println(username);
                    System.out.println(password);
                    System.out.println(micro);
                }

                // 跳过变量引用
                if ((username != null && username.trim().startsWith("${")) ||
                    (password != null && password.trim().startsWith("${"))) {
                    System.out.println("[导入跳过] micro=" + micro + ", username=" + username + ", password=" + password);
                    continue;
                }
                // url/driver优先用micro匹配，否则用default
                String url = null;
                String driver = null;
                // 优先用micro匹配url
                for (String urlKey : urlMap.keySet()) {
                    if (urlKey.contains(micro) && !urlMap.get(urlKey).trim().startsWith("${")) {
                        url = urlMap.get(urlKey);
                        break;
                    }
                }
                if (url == null) {
                    // fallback: 用default
                    for (String urlVal : urlMap.values()) {
                        if (!urlVal.trim().startsWith("${")) {
                            url = urlVal;
                            break;
                        }
                    }
                }
                driver = findDriverForMicro(driverMap, "", micro);
                if (url == null || driver == null || url.isEmpty() || driver.isEmpty()) {
                    System.out.println("[导入跳过] micro=" + micro + ", url/driver为空");
                    continue;
                }

                DbConnectionPO po = new DbConnectionPO();
                po.setGroupName(globalPropes.getGroupName());
                po.setAppName(micro);
                po.setJdbcUrl(url);
                po.setUsername(username);
                po.setPassword(password);
                po.setDriver(driver);
                po.setRemark("导入自配置文件");
                dbConnectionLst.add(po);
            }

            Set<String> seen = new HashSet<>();
            List<UserProjSettingPO> matchList = UserProjSettingDao.queryForList(new UserProjSettingPO());
            for (DbConnectionPO dbConnectionPO : dbConnectionLst) {
                for (UserProjSettingPO param : matchList) {
                    if (param.getAppName() != null && param.getAppName().equals(dbConnectionPO.getAppName())) {
                        dbConnectionPO.setProjectName(param.getProjectName());
                        dbConnectionPO.setSchemaNm(param.getSchemaNm());

                        String uniqueKey = dbConnectionPO.getGroupName() + "|" + dbConnectionPO.getProjectName()
                                + "|" + dbConnectionPO.getAppName() + "|" + dbConnectionPO.getSchemaNm();
                        if (seen.contains(uniqueKey)) continue;
                        seen.add(uniqueKey);

                        dbConnectionDao.save(dbConnectionPO);
                        break;
                    }
                }

            }


            showAlert("导入完成");
            loadDbConnectionsByCurrentEnv();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("导入失败: " + ex.getMessage());
        }
    }

    private String getMicroServiceName(String key, String suffix) {
        // ecp.common.db.eik.username#KEN => eik
        String s = key.substring("ecp.common.db.".length());
        int idx = s.indexOf(suffix);
        if (idx > 0) {
            return s.substring(0, idx);
        }
        return s;
    }

    private String extractMicroServiceNameFromUrlKey(String key) {
        // ecp.common.db.logadm.url
        if (key.startsWith("ecp.common.db.")) {
            String s = key.substring("ecp.common.db.".length());
            int idx = s.indexOf(".url");
            if (idx > 0) return s.substring(0, idx);
        }
        // ecp.common.datasource.url
        if (key.equals("ecp.common.datasource.url")) {
            return "default";
        }
        // ecp.datasource.ora1#KEN.url
        if (key.startsWith("ecp.datasource.")) {
            String s = key.substring("ecp.datasource.".length());
            int idx = s.indexOf("#");
            if (idx > 0) return s.substring(0, idx);
            idx = s.indexOf(".");
            if (idx > 0) return s.substring(0, idx);
        }
        // ecp.common.db.logadm.url#KEN
        if (key.startsWith("ecp.common.db.")) {
            String s = key.substring("ecp.common.db.".length());
            int idx = s.indexOf(".url#");
            if (idx > 0) return s.substring(0, idx);
        }
        return null;
    }

    private String findDriverForMicro(Map<String, String> driverMap, String urlKey, String micro) {
        // 优先匹配urlKey中的#KEN等后缀
        String env = null;
        int idx = urlKey.indexOf("#");
        if (idx > 0) {
            int end = urlKey.indexOf(".", idx);
            if (end > idx) {
                env = urlKey.substring(idx, end);
            } else {
                env = urlKey.substring(idx);
            }
        }
        if (env != null) {
            for (String key : driverMap.keySet()) {
                if (key.contains(env)) return driverMap.get(key);
            }
        }
        // 再用micro匹配
        for (String key : driverMap.keySet()) {
            if (key.contains(micro)) return driverMap.get(key);
        }
        // fallback
        return driverMap.values().stream().findFirst().orElse("");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private <S, T> void enableCellDoubleClickCopy(TableColumn<S, T> column) {
        column.setCellFactory(col -> {
            TableCell<S, T> cell = new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                }
            };
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getText());
                    clipboard.setContent(content);
                }
            });
            return cell;
        });
    }

    private <T> void setCopyableCellFactory(TableColumn<DatasourceConfig, T> column, java.util.function.Function<DatasourceConfig, String> getter) {
        column.setCellFactory(col -> new TableCell<DatasourceConfig, T>() {
            {
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(getText());
                        clipboard.setContent(content);
                        Tooltip tp = new Tooltip("已复制: " + getText());
                        Tooltip.install(this, tp);
                        tp.show(this, event.getScreenX(), event.getScreenY());
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(tp::hide);
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    DatasourceConfig row = getTableView().getItems().get(getIndex());
                    setText(getter.apply(row));
                    setGraphic(null);
                }
                setStyle("-fx-alignment: center-left;");
            }
        });
    }
} 