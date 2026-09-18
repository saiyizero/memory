package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.domain.service.GenerateDocService;
import com.murong.ecp.tools.fx.domain.service.interfaces.EnumsCdAnalyService;
import com.murong.ecp.tools.fx.domain.service.interfaces.JavaCodeService;
import com.murong.ecp.tools.fx.domain.service.interfaces.InterFaceEntityService;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.InterfaceDataRpcService;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.SelectDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.ProgressDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;

@Component
public class PaneTransactionController implements Initializable {
    @Autowired
    GlobalProperties globalProps;
    @Autowired
    private InterfaceDataRpcService interfaceDataRpcService;
    @Autowired
    private GenerateDocService generateDocService;
    @Autowired
    private JavaCodeService javaCodeService;
    @Autowired
    private InterFaceEntityService interFaceEntityService;
    @Autowired
    private EnumsCdAnalyService enumsCdAnalyService;
    @Autowired
    private PanRestFulController restfulController;

    @FXML
    private TableView<InterfaceDataPO> transactionTableView;
    @FXML
    private TableColumn<InterfaceDataPO, Boolean> selectColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> transNameColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> interfaceNameColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> labenameColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> urlColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> statusColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> transCommentZhColumn;
    @FXML
    private TableColumn<InterfaceDataPO, String> transCommentEnColumn;
    @FXML
    private TableColumn<InterfaceDataPO, Void> actionColumn;
    @FXML
    private TableColumn<InterfaceDataPO, Void> testColumn;
    private ObservableList<InterfaceDataPO> transactionList = FXCollections.observableArrayList();
    // 用于存储每行的选中状态
    private ObservableList<SimpleBooleanProperty> selectedList = FXCollections.observableArrayList();
    @FXML
    private CheckBox selectAllCheckBox;
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<String> labenameComboBox;
    @FXML
    private Button syncApiButton;
    @FXML
    private Button importJarButton;    // 导入JAR包按钮
    @FXML
    private Button addApiButton;        // 新增接口按钮
    @FXML
    private TextField nameFilterField; // 名称输入框
    @FXML
    private Button queryButton;        // 查询按钮
    private final AtomicInteger loadSeq = new AtomicInteger();
    private boolean suppressLabelChange;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }

        try {
            setupTableUi();
            loadTableDataAsync(true);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("初始化交易接口页面失败: " + e.getMessage());
            ViewUtils.alertForFail("交易接口页面初始化失败: " + e.getMessage());
        }
    }

    private void setupTableUi() {
        transactionTableView.setItems(transactionList);
        transactionTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        transactionTableView.setPlaceholder(new Label("正在加载..."));
        if (nameFilterField != null) {
            nameFilterField.clear();
        }

        // 全选/全不选功能
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (SimpleBooleanProperty prop : selectedList) {
                prop.set(newVal);
            }
            transactionTableView.refresh();
        });

        // 复选框列绑定
        selectColumn.setCellValueFactory(param -> {
            int index = transactionList.indexOf(param.getValue());
            if (index >= 0 && index < selectedList.size()) {
                return selectedList.get(index);
            } else {
                return new SimpleBooleanProperty(false);
            }
        });
        selectColumn.setCellFactory(col -> new TableCell<InterfaceDataPO, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0 && index < selectedList.size()) {
                        selectedList.get(index).set(checkBox.isSelected());
                    }
                    // 更新全选框状态
                    updateSelectAllCheckBox();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= selectedList.size()) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(selectedList.get(getIndex()).get());
                    setGraphic(checkBox);
                }
            }
        });

        // 设置可复制的文本列
        setCopyableCellFactory(transNameColumn, InterfaceDataPO::getTransName);
        setCopyableCellFactoryCentered(labenameColumn, InterfaceDataPO::getLableName);  // 标签列居中
        setCopyableCellFactory(transCommentZhColumn, InterfaceDataPO::getTransCommentZh);
        setCopyableCellFactory(transCommentEnColumn, InterfaceDataPO::getTransCommentEn);
        setCopyableCellFactory(interfaceNameColumn, InterfaceDataPO::getInterfaceName);
        setCopyableCellFactoryCentered(statusColumn, po -> {
            if (po == null || po.getStatus() == null || po.getStatus().isBlank()) {
                return "";
            }
            DataStatusEnum statusEnum = DataStatusEnum.getByCode(po.getStatus());
            return statusEnum != null ? statusEnum.getDesc() : po.getStatus();
        });
        setCopyableCellFactory(urlColumn, po -> po.getMethodUrl());

        // 操作列：添加按钮
        actionColumn.setCellFactory(col -> new TableCell<InterfaceDataPO, Void>() {
            private final Button editBtn = new Button();
            private final Button delBtn = new Button();
            private final HBox hbox = new HBox(8, editBtn, delBtn);
            {
                // 设置编辑按钮样式 - 使用与删除按钮相同的SVG图标样式
                javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
                editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                editIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                editIcon.setScaleX(0.7);
                editIcon.setScaleY(0.7);
                editBtn.setGraphic(editIcon);
                editBtn.setTooltip(new Tooltip("编辑"));
                editBtn.setMinWidth(28);
                editBtn.setPrefWidth(28);
                editBtn.setMaxWidth(28);
                editBtn.setMinHeight(28);
                editBtn.setPrefHeight(28);
                editBtn.setMaxHeight(28);
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent;"));
                editBtn.setOnMousePressed(e -> editBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                editBtn.setOnMouseReleased(e -> editBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                editBtn.setOnAction(e -> {
                    InterfaceDataPO po = getTableView().getItems().get(getIndex());
                    openApiEdit(po.getTransName());
                });
                
                // 设置删除按钮样式 - 使用与PaneExeRecordController相同的SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                delBtn.setGraphic(trashIcon);
                delBtn.setTooltip(new Tooltip("删除"));
                delBtn.setMinWidth(28);
                delBtn.setPrefWidth(28);
                delBtn.setMaxWidth(28);
                delBtn.setMinHeight(28);
                delBtn.setPrefHeight(28);
                delBtn.setMaxHeight(28);
                delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                // 添加鼠标悬停效果
                delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent;"));
                delBtn.setOnMousePressed(e -> delBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                delBtn.setOnMouseReleased(e -> delBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                delBtn.setOnAction(e -> {/* 删除逻辑 */});
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
                setStyle("-fx-alignment: center;");
            }
        });

        // 新增测试列：只放测试按钮
        testColumn.setCellFactory(col -> new TableCell<InterfaceDataPO, Void>() {
            private final Button testBtn = new Button();
            {
                javafx.scene.shape.SVGPath testIcon = new javafx.scene.shape.SVGPath();
                testIcon.setContent("M8 5v14l11-7z");
                testIcon.setStyle("-fx-stroke: #27ae60; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                testIcon.setScaleX(0.85); testIcon.setScaleY(0.85);
                testBtn.setGraphic(testIcon);
                testBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                testBtn.setTooltip(new Tooltip("接口测试"));
                testBtn.setMinWidth(28);
                testBtn.setPrefWidth(28);
                testBtn.setMaxWidth(28);
                testBtn.setMinHeight(28);
                testBtn.setPrefHeight(28);
                testBtn.setMaxHeight(28);
                // 添加鼠标悬停效果 - 参考PaneExeRecordController的执行按钮样式
                testBtn.setOnMouseEntered(e -> testBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                testBtn.setOnMouseExited(e -> testBtn.setStyle("-fx-background-color: transparent;"));
                testBtn.setOnMousePressed(e -> testBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                testBtn.setOnMouseReleased(e -> testBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                
                testBtn.setOnAction(e -> {
                    InterfaceDataPO po = getTableView().getItems().get(getIndex());
                    openRestfulTest(po);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(testBtn);
                }
            }
        });

        suppressLabelChange = true;
        try {
            labenameComboBox.getItems().setAll("ALL");
            labenameComboBox.setValue("ALL");
        } finally {
            suppressLabelChange = false;
        }
        labenameComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressLabelChange) {
                return;
            }
            loadTableDataAsync(false);
        });

        syncApiButton.setOnAction(event -> syncApi());

        importJarButton.setOnAction(event -> importJarFile());

        addApiButton.setOnAction(event -> addNewApi());

        exportButton.setOnAction(event -> showExportDialog());

        if (queryButton != null) {
            queryButton.setOnAction(event -> doQuery());
        }
    }

    private void loadTableDataAsync(boolean refreshLabels) {
        InterfaceDataPO reqPO = new InterfaceDataPO();
        reqPO.setGroupName(globalProps.getGroupName());
        reqPO.setAppName(globalProps.getAppName());
        String selectedLabel = labenameComboBox == null ? null : labenameComboBox.getValue();
        if (selectedLabel != null && !selectedLabel.isEmpty() && !"ALL".equals(selectedLabel)) {
            reqPO.setLableName(selectedLabel);
        }
        queryAndApplyAsync(() -> interfaceDataRpcService.queryForListSummary(reqPO), refreshLabels);
    }

    private void queryAndApplyAsync(Supplier<List<InterfaceDataPO>> query, boolean refreshLabels) {
        int seq = loadSeq.incrementAndGet();
        transactionTableView.setPlaceholder(new Label("正在加载..."));
        Thread loader = new Thread(() -> {
            try {
                List<InterfaceDataPO> list = query.get();
                if (seq != loadSeq.get()) {
                    return;
                }
                List<InterfaceDataPO> result = list == null ? List.of() : list;
                Platform.runLater(() -> applyTableData(result, refreshLabels));
            } catch (Exception e) {
                e.printStackTrace();
                if (seq != loadSeq.get()) {
                    return;
                }
                Platform.runLater(() -> {
                    transactionTableView.setPlaceholder(new Label("加载失败"));
                    ViewUtils.alertForFail("加载交易接口数据失败: " + e.getMessage());
                });
            }
        }, "transaction-list-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyTableData(List<InterfaceDataPO> list, boolean refreshLabels) {
        transactionList.setAll(list);
        transactionTableView.setItems(transactionList);
        selectedList.clear();
        for (int i = 0; i < transactionList.size(); i++) {
            selectedList.add(new SimpleBooleanProperty(false));
        }
        if (selectAllCheckBox != null) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        }
        if (refreshLabels) {
            fillLabelCombo(list);
        }
        transactionTableView.setPlaceholder(new Label(list.isEmpty() ? "暂无数据" : ""));
        transactionTableView.refresh();
    }

    private void fillLabelCombo(List<InterfaceDataPO> list) {
        List<String> labelNames = list.stream()
                .map(InterfaceDataPO::getLableName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .toList();
        suppressLabelChange = true;
        try {
            List<String> items = new ArrayList<>();
            items.add("ALL");
            items.addAll(labelNames);
            labenameComboBox.getItems().setAll(items);
            labenameComboBox.setValue("ALL");
        } finally {
            suppressLabelChange = false;
        }
    }

    // 更新全选框状态
    private void updateSelectAllCheckBox() {
        long selectedCount = selectedList.stream().filter(SimpleBooleanProperty::get).count();
        if (selectedCount == selectedList.size() && selectedCount > 0) {
            selectAllCheckBox.setSelected(true);
            selectAllCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
        } else {
            selectAllCheckBox.setIndeterminate(true);
        }
    }

    @FXML
    private void exportTransactionDoc() {
        // 1. 获取选中交易名
        List<Map<String,String>> selectedTransNames = FXCollections.observableArrayList();
        for (int i = 0; i < transactionList.size(); i++) {
            if (selectedList.get(i).get()) {
                Map<String, String> map = new HashMap<>();
                map.put("transName", transactionList.get(i).getTransName());
                map.put("interfaceName", transactionList.get(i).getInterfaceName());
                selectedTransNames.add(map);
            }
        }
        if (selectedTransNames.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "请至少选择一个交易！", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // 2. 调用服务生成markdown
        String markdown = generateDocService.generateTransactionDoc(selectedTransNames);
        if (markdown == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "未找到选中的交易数据！", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // 3. 保存文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存Markdown文档");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown文件", "*.md"));
        String defaultFileName = (globalProps.getProjectName() != null ? globalProps.getProjectName() : "project") + "_API_" + MrDateUtils.getCurrentDate() + ".md";
        fileChooser.setInitialFileName(defaultFileName);
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(markdown);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    // 跳转到API编辑界面并传递交易名
    private void openApiEdit(String transName) {
        MainController mainController = MrSpringContextHolder.getBean(MainController.class);
        Tab transactionTab = mainController.getTabByKey("transactionApi");
        if (transactionTab != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pane_api.fxml"));
                loader.setControllerFactory(clazz -> MrSpringContextHolder.getBean(clazz));
                Node apiPage = loader.load();
                transactionTab.setContent(apiPage);
                mainController.getPageContainer().getSelectionModel().select(transactionTab);
                // 设置交易名
                PaneApiController apiController = loader.getController();
                apiController.setSelectedApi(transName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void syncApi() {
        try {
            // 1. 获取所有需要同步的模块路径
            String baseUrl = globalProps.getBasePath();
            List<String> scanPaths = new ArrayList<>();
            
            for (GlobalProperties.ModuleApi moduleApi : globalProps.getModuleApis()) {
                if (moduleApi.getBasePath() != null && !moduleApi.getBasePath().trim().isEmpty()) {
                    String scanPath = baseUrl + "/" + moduleApi.getBasePath();
                    scanPaths.add(scanPath);
                }
            }

            if (scanPaths.isEmpty()) {
                throw new RuntimeException("项目module-api相关配置有误，未找到有效的API模块路径，请检查");
            }

            // 计算总步骤数
            int totalSteps = 0;
            if (!globalProps.getEnums().isEmpty()) totalSteps += globalProps.getEnums().size();
            if (!globalProps.getMsgCodes().isEmpty()) totalSteps += globalProps.getMsgCodes().size();
            totalSteps += 1; // 接口同步步骤
            
            final int finalTotalSteps = totalSteps;
            final int[] currentStep = {0};

            // 使用进度对话框执行批量同步
            ProgressDialogUtil.executeWithProgress(
                "同步接口",
                "正在同步接口...",
                "准备开始同步，共 " + finalTotalSteps + " 个步骤",
                () -> {
                    CrResult result = new CrResult(SuccessFailureEnum.SUCCESS);
                    StringBuilder resultMsg = new StringBuilder();
                    
                    try {

                        // 预备：备份删除枚举
                        interFaceEntityService.deleteByEnums(globalProps.getAppName());

                        // 第一步：解析枚举
                        if (!globalProps.getEnums().isEmpty()) {
                            resultMsg.append("枚举解析完成。");
                            for (GlobalProperties.CrEnum enums : globalProps.getEnums()) {
                                currentStep[0]++;
                                String progressMsg = String.format("正在解析枚举 (%d/%d): %s", 
                                    currentStep[0], finalTotalSteps, enums.getBasePath());
                                
                                // 输出进度信息到控制台
                                System.out.println(progressMsg);
                                
                                try {
                                    enumsCdAnalyService.analyzeDict(baseUrl, enums);
                                } catch (Exception e) {
                                    System.err.println("解析枚举失败: " + enums.getBasePath() + ", 错误: " + e.getMessage());
                                    resultMsg.append("枚举解析失败: ").append(e.getMessage()).append("; ");
                                }
                            }
                        }
                        
                        // 第二步：解析消息代码
                        if (!globalProps.getMsgCodes().isEmpty()) {
                            resultMsg.append("消息代码解析完成。");
                            for (GlobalProperties.CrMsgCode crMsgCode : globalProps.getMsgCodes()) {
                                currentStep[0]++;
                                String progressMsg = String.format("正在解析消息代码 (%d/%d): %s", 
                                    currentStep[0], finalTotalSteps, crMsgCode.getBasePath());
                                
                                // 输出进度信息到控制台
                                System.out.println(progressMsg);
                                
                                try {
                                    enumsCdAnalyService.analyzeMsgCode(baseUrl, crMsgCode);
                                } catch (Exception e) {
                                    System.err.println("解析消息代码失败: " + crMsgCode.getBasePath() + ", 错误: " + e.getMessage());
                                    resultMsg.append("消息代码解析失败: ").append(e.getMessage()).append("; ");
                                }
                            }
                        }
                        
                        // 第三步：同步接口
                        currentStep[0]++;
                        String progressMsg = String.format("正在同步接口 (%d/%d): 扫描 %d 个模块", 
                            currentStep[0], finalTotalSteps, scanPaths.size());
                        System.out.println(progressMsg);
                        
                        CrResult syncResult = syncAllModules(scanPaths);
                        if (syncResult != null && syncResult.isSucess()) {
                            resultMsg.append("接口同步完成。");
                        } else {
                            String errorMessage = syncResult != null ? syncResult.getMsgInf() : "未知错误";
                            resultMsg.append("接口同步失败: ").append(errorMessage).append("; ");
                        }
                        
                        result.setMsgInf(resultMsg.toString());
                        
                    } catch (Exception e) {
                        result = new CrResult(SuccessFailureEnum.FAILURE);
                        result.setMsgInf("同步过程中发生错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    return result;
                },
                result -> {
                    if (result != null && result.isSucess()) {
                        // 使用Platform.runLater确保在JavaFX线程中执行
                        javafx.application.Platform.runLater(() -> {
                            try {
                                ViewUtils.alertForSucess(result.getMsgInf());
                                // 刷新表格数据
                                refreshTable();
                            } catch (Exception e) {
                                e.printStackTrace();
                                ViewUtils.alertForFail(e.getMessage());
                                refreshTable();
                            }
                        });
                    } else {
                        String errorMessage = result != null ? result.getMsgInf() : "未知错误";
                        // 使用Platform.runLater确保在JavaFX线程中执行
                        javafx.application.Platform.runLater(() -> {
                            try {
                                ViewUtils.alertForFail(errorMessage);
                            } catch (Exception e) {
                                e.printStackTrace();
                                ViewUtils.alertForFail(errorMessage);
                            }
                        });
                    }
                },
                errorMsg -> {
                    // 使用Platform.runLater确保在JavaFX线程中执行
                    javafx.application.Platform.runLater(() -> {
                        try {
                            ViewUtils.alertForFail("同步接口失败: " + errorMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ViewUtils.alertForFail(errorMsg);
                        }
                    });
                }
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                try {
                    ViewUtils.alertForFail("同步接口失败: " + e.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ViewUtils.alertForFail(e.getMessage());
                }
            });
        }
    }

    /**
     * 批量同步所有模块的接口
     * @param scanPaths 所有需要扫描的路径列表
     * @return 同步结果
     */
    private CrResult syncAllModules(List<String> scanPaths) {
        try {
            int totalSuccessCount = 0;
            StringBuilder resultMessage = new StringBuilder();
            
            // 1. 先备份和清理现有数据
            interFaceEntityService.deleteByAppName(globalProps.getAppName());
            
            // 2. 逐个处理每个模块
            for (int i = 0; i < scanPaths.size(); i++) {
                String scanPath = scanPaths.get(i);
                try {
                    // 调用 JavaCodeService 解析接口
                    List<InterFaceEntity> entityList = javaCodeService.javaEntityConvInterFace(scanPath);
                    
                    // 如果返回null或空列表，说明编译失败，立即停止
                    if (entityList == null) {
                        String moduleName = scanPath.substring(scanPath.lastIndexOf("/") + 1);
                        String errorMsg = "模块 " + moduleName + " 编译失败，同步已停止。请检查Maven依赖配置后重试。";
                        CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                        result.setMsgInf(errorMsg);
                        return result;
                    }
                    
                    // 登记接口对应父类
                    CrResult registerResult = javaCodeService.registerParentClass(entityList, scanPath);
                    if (!registerResult.isSucess()) {
                        String moduleName = scanPath.substring(scanPath.lastIndexOf("/") + 1);
                        CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                        result.setMsgInf("模块 " + moduleName + " 父类登记失败: " + registerResult.getMsgInf() + "\n同步已停止。");
                        return result;
                    }
                    
                    // 存储到数据库
                    int moduleSuccessCount = 0;
                    if (entityList != null && !entityList.isEmpty()) {
                        for (InterFaceEntity entity : entityList) {
                            interFaceEntityService.saveInterFaceEntity(entity);
                            moduleSuccessCount++;
                        }
                    }
                    
                    totalSuccessCount += moduleSuccessCount;
                    
                    // 构建结果消息
                    String moduleName = scanPath.substring(scanPath.lastIndexOf("/") + 1);
                    if (moduleSuccessCount > 0) {
                        resultMessage.append("模块 ").append(moduleName).append(": ").append(moduleSuccessCount).append(" 个接口\n");
                    }
                    
                } catch (Exception e) {
                    String moduleName = scanPath.substring(scanPath.lastIndexOf("/") + 1);
                    String errorMsg = "模块 " + moduleName + " 处理失败: " + e.getMessage() + "\n同步已停止。";
                    CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                    result.setMsgInf(errorMsg);
                    return result;
                }
            }
            
            CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
            result.setMsgInf("同步完成，共处理 " + scanPaths.size() + " 个模块，总计 " + totalSuccessCount + " 个接口\n\n" + resultMessage.toString());
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("批量同步接口失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 设置TableColumn为可选中和右键复制的Label
     */
    private void setCopyableCellFactory(TableColumn<InterfaceDataPO, String> column, Function<InterfaceDataPO, String> getter) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<InterfaceDataPO, String>() {
            private final Label label = new Label();
            {
                label.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(label.getText());
                        clipboard.setContent(content);
                        // 可选：弹出提示
                        Tooltip tp = new Tooltip("已复制: " + label.getText());
                        Tooltip.install(label, tp);
                        tp.show(label, event.getScreenX(), event.getScreenY());
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(tp::hide);
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
                setStyle("-fx-alignment: center-left;");
            }
        });
    }

    /**
     * 设置TableColumn为可选中和右键复制的Label，文字居中显示
     */
    private void setCopyableCellFactoryCentered(TableColumn<InterfaceDataPO, String> column, Function<InterfaceDataPO, String> getter) {
        column.setCellValueFactory(cellData -> new SimpleStringProperty(getter.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<InterfaceDataPO, String>() {
            private final Label label = new Label();
            {
                label.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(label.getText());
                        clipboard.setContent(content);
                        // 可选：弹出提示
                        Tooltip tp = new Tooltip("已复制: " + label.getText());
                        Tooltip.install(label, tp);
                        tp.show(label, event.getScreenX(), event.getScreenY());
                        new Thread(() -> {
                            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                            Platform.runLater(tp::hide);
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
                setStyle("-fx-alignment: center;");  // 文字居中显示
            }
        });
    }

    @FXML
    private void refreshTable() {
        if (nameFilterField != null) {
            nameFilterField.clear();
        }
        suppressLabelChange = true;
        try {
            if (labenameComboBox != null) {
                labenameComboBox.setValue("ALL");
            }
        } finally {
            suppressLabelChange = false;
        }
        loadTableDataAsync(true);
    }

    /**
     * 弹出导出类型选择对话框
     */
    private void showExportDialog() {
        List<String> options = List.of("CI-导出CoreApis文档", "MD-导出接口MD文档", "ESQL-导出相关枚举SQL");
        
        SelectDialogUtil.showSelectDialog(
            "选择导出类型", 
            options, 
            "CI-导出CoreApis文档",
            selected -> {
                switch (selected) {
                    case "MD-导出接口MD文档":
                        exportTransactionDoc();
                        break;
                    case "ESQL-导出相关枚举SQL":
                        exportAssociatEnumSql();
                        break;
                    case "CI-导出CoreApis文档":
                        exportCoreApiMarkDown();
                        break;
                }
            }
        );
    }

    /**
     * 导出Core-Api所需MD文档
     */
    private void exportCoreApiMarkDown() {
        // 1. 获取选中交易名和interfaceName
        List<Map<String, String>> selectedTrans = FXCollections.observableArrayList();
        for (int i = 0; i < transactionList.size(); i++) {
            if (selectedList.get(i).get()) {
                Map<String, String> map = new HashMap<>();
                map.put("transName", transactionList.get(i).getTransName());
                map.put("interfaceName", transactionList.get(i).getInterfaceName());
                selectedTrans.add(map);
            }
        }
        if (selectedTrans.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "请至少选择一个交易！", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // 2. 选择导出目录
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择导出目录中的任意文件夹");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown文件", "*.md"));
        fileChooser.setInitialFileName("md-demo.md");
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) return;
        File dir = file.getParentFile();
        try {
            // 3. 逐个生成md（每个交易只生成一个文件，文件名和API Name都用transName）
            for (Map<String, String> item : selectedTrans) {
                String transName = item.get("transName");
                String interfaceName = item.get("interfaceName");
                InterFaceEntity entity = interFaceEntityService.getByTransName(transName, interfaceName);
                if (entity == null) continue;
                java.util.Map<String, Object> dataModel = new java.util.HashMap<>();
                dataModel.put("transName", entity.getTransName());
                dataModel.put("properties", entity.getProperties());
                dataModel.put("transCommentEn", entity.getTransCommentEn());
                dataModel.put("request", entity.getRequest());
                dataModel.put("response", entity.getResponse());
                dataModel.put("requestExample", "{}"); // 可根据需要生成示例
                dataModel.put("responseExample", "{}"); // 可根据需要生成示例
                freemarker.template.Configuration cfg = MrSpringContextHolder.getBean(freemarker.template.Configuration.class);
                freemarker.template.Template template = cfg.getTemplate("markdown/core-api-markdown.ftl");
                java.io.StringWriter out = new java.io.StringWriter();
                template.process(dataModel, out);
                String fileName = "md-" + entity.getTransName() + ".md";
                File outFile = new File(dir, fileName);
                try (java.io.FileWriter writer = new java.io.FileWriter(outFile, false)) {
                    writer.write(out.toString());
                }
            }
            ViewUtils.alertForSucess("导出完成！");
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出相关枚举SQL为txt文件
     */
    private void exportAssociatEnumSql() {
        // 1. 获取选中交易
        StringBuilder sqlBuilder = new StringBuilder();
        boolean hasEnum = false;
        for (int i = 0; i < transactionList.size(); i++) {
            if (selectedList.get(i).get()) {
                InterfaceDataPO po = transactionList.get(i);
                String associatEnum = null;
                try {
                    associatEnum = po.getAssociatEnum();
                } catch (Exception ignore) {}
                if (associatEnum != null && !associatEnum.trim().isEmpty()) {
                    hasEnum = true;
                    // 这里假设associatEnum为逗号分隔的枚举名
                    String[] enums = associatEnum.split(",");
                    for (String enumName : enums) {
                        enumName = enumName.trim();
                        if (!enumName.isEmpty()) {
                            sqlBuilder.append("-- 枚举: ").append(enumName).append("\n");
                            sqlBuilder.append("SELECT * FROM enum_table WHERE enum_name = '")
                                    .append(enumName).append("';\n\n");
                        }
                    }
                }
            }
        }
        if (!hasEnum) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "选中的交易没有关联枚举！", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        // 2. 保存txt文件
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存枚举SQL");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
        String defaultFileName = (globalProps.getProjectName() != null ? globalProps.getProjectName() : "project") + "_ENUM_SQL_" + MrDateUtils.getCurrentDate() + ".txt";
        fileChooser.setInitialFileName(defaultFileName);
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(sqlBuilder.toString());
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    // 跳转到Restful测试界面并传递url和交易信息
    private void openRestfulTest(InterfaceDataPO po) {
        if (po == null) return;
        String url = po.getInterfaceUrl() + po.getMethodUrl();
        MainController mainController = MrSpringContextHolder.getBean(MainController.class);
        mainController.showPage("测试-"+po.getTransName(), "/fxml/pane_restful.xml");
        restfulController.setUrl(url);
        restfulController.setTransInfo(po.getTransName(), po.getTransCommentZh());
        restfulController.onQueryDebugLog();
    }

    /**
     * 导入JAR包文件
     */
    private void importJarFile() {
        try {
            // 1. 选择JAR文件
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择JAR包文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JAR文件", "*.jar")
            );
            
            File selectedFile = fileChooser.showOpenDialog(importJarButton.getScene().getWindow());
            if (selectedFile == null) {
                return; // 用户取消选择
            }
            
            // 2. 验证文件
            if (!selectedFile.exists()) {
                ViewUtils.alertForFail("选择的文件不存在！");
                return;
            }
            
            if (!selectedFile.getName().toLowerCase().endsWith(".jar")) {
                ViewUtils.alertForFail("请选择有效的JAR文件！");
                return;
            }
            
            // 3. 使用进度对话框执行导入
            ProgressDialogUtil.executeWithProgress(
                "导入JAR包",
                "正在解析JAR包...",
                "正在导入: " + selectedFile.getName(),
                () -> interFaceEntityService.importJarInterfaces(selectedFile.getAbsolutePath()),
                result -> {
                    if (result.isSucess()) {
                        // ViewUtils.alertForSucess("成功导入JAR包: " + selectedFile.getName() +" "+result.getMsgInf());
                        // 显示成功对话框
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("导入成功");
                        successAlert.setHeaderText("JAR包导入完成");
                        successAlert.setContentText("成功导入JAR包: " + selectedFile.getName() +
                                                  "\n\n" + result.getMsgInf());
                        
                        // 强制添加确定按钮
                        successAlert.getButtonTypes().clear();
                        successAlert.getButtonTypes().add(ButtonType.OK);
                        
                        // 设置模态和置顶
                        successAlert.initModality(Modality.APPLICATION_MODAL);
                        if (importJarButton.getScene() != null && importJarButton.getScene().getWindow() != null) {
                            successAlert.initOwner(importJarButton.getScene().getWindow());
                        }
                        
                        successAlert.showAndWait();
                        
                        // 刷新表格数据
                        refreshTable();
                    } else {
                        ViewUtils.alertForFail("导入JAR包失败: " + result.getMsgInf());
                    }
                },
                errorMsg -> ViewUtils.alertForFail("导入JAR包失败: " + errorMsg)
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("导入JAR包时发生错误: " + e.getMessage());
        }
    }

    /**
     * 新增接口按钮事件
     */
    private void addNewApi() {
        MainController mainController = MrSpringContextHolder.getBean(MainController.class);
        Tab transactionTab = mainController.getTabByKey("transactionApi");
        if (transactionTab != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pane_api.fxml"));
                loader.setControllerFactory(clazz -> MrSpringContextHolder.getBean(clazz));
                Node apiPage = loader.load();
                transactionTab.setContent(apiPage);
                mainController.getPageContainer().getSelectionModel().select(transactionTab);
                // 设置交易名
                PaneApiController apiController = loader.getController();
                apiController.setSelectedApi(null); // 新增接口时，不传递交易名
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 按输入框内容进行查询
     */
    private void doQuery() {
        String name = nameFilterField != null ? nameFilterField.getText() : "";
        InterfaceDataPO reqPO = new InterfaceDataPO();
        reqPO.setAppName(globalProps.getAppName());
        reqPO.setGroupName(globalProps.getGroupName());
        if (name != null && !name.isEmpty()) {
            queryAndApplyAsync(() -> interfaceDataRpcService.queryForSearch(reqPO.getAppName(), name), false);
        } else {
            queryAndApplyAsync(() -> interfaceDataRpcService.queryForListSummary(reqPO), false);
        }
    }
}
